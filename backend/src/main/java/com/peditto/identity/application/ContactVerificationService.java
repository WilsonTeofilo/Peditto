package com.peditto.identity.application;

import com.peditto.identity.domain.AppUser;
import com.peditto.common.persistence.AuditWriter;
import com.peditto.common.tenant.TenantTransactionContext;
import com.peditto.identity.domain.AuthChallenge;
import com.peditto.identity.domain.AuthChallengePurpose;
import com.peditto.identity.domain.IdentityInputNormalizer;
import com.peditto.identity.domain.OtpChannel;
import com.peditto.identity.infrastructure.AppUserRepository;
import com.peditto.identity.infrastructure.AuthChallengeRepository;
import com.peditto.organizations.domain.Membership;
import com.peditto.organizations.domain.MembershipRole;
import com.peditto.organizations.domain.MembershipStatus;
import com.peditto.organizations.domain.Organization;
import com.peditto.organizations.infrastructure.MembershipRepository;
import com.peditto.subscriptions.domain.Trial;
import com.peditto.subscriptions.infrastructure.TrialRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactVerificationService {
    private final AppUserRepository users;
    private final AuthChallengeRepository challenges;
    private final MembershipRepository memberships;
    private final TrialRepository trials;
    private final OtpDeliveryService otpDelivery;
    private final IdentityAbuseLimiter limiter;
    private final AuditWriter audit;
    private final TenantTransactionContext tenantContext;

    public ContactVerificationService(AppUserRepository users, AuthChallengeRepository challenges,
            MembershipRepository memberships, TrialRepository trials, OtpDeliveryService otpDelivery,
            IdentityAbuseLimiter limiter, AuditWriter audit, TenantTransactionContext tenantContext) {
        this.users = users; this.challenges = challenges; this.memberships = memberships;
        this.trials = trials; this.otpDelivery = otpDelivery; this.limiter = limiter; this.audit = audit;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public boolean verify(String email, OtpChannel channel, String code) {
        final String normalized;
        try { normalized = IdentityInputNormalizer.email(email); }
        catch (IllegalArgumentException invalid) { return false; }
        if (channel == null || !limiter.allowOtp(normalized, channel.name())) return false;
        AppUser user = users.lockByEmailNormalized(normalized).orElse(null);
        if (user == null) return false;
        AuthChallengePurpose purpose = channel == OtpChannel.EMAIL
                ? AuthChallengePurpose.VERIFY_EMAIL : AuthChallengePurpose.VERIFY_PHONE;
        List<AuthChallenge> candidates = challenges.lockLiveCandidates(normalized, purpose, channel);
        Instant now = Instant.now();
        AuthChallenge challenge = candidates.stream().findFirst().orElse(null);
        if (challenge == null || !challenge.isUsableAt(now) || !otpDelivery.matches(challenge, code)) {
            if (challenge != null && challenge.getAttempts() < 10) challenge.recordFailedAttempt();
            return false;
        }
        challenge.consume(now);
        if (channel == OtpChannel.EMAIL) user.verifyEmail(now); else user.verifyPhone(now);
        audit.record("IDENTITY_CONTACT_VERIFIED", "auth_challenge", challenge.getId(), user.getId(), null);
        if (user.isEnabled()) activateOrganizationAndStartTrial(user, now);
        return true;
    }

    private void activateOrganizationAndStartTrial(AppUser user, Instant now) {
        Membership owner = memberships.findByUser_IdAndRoleAndStatus(user.getId(), MembershipRole.OWNER, MembershipStatus.INVITED)
                .orElse(null);
        if (owner == null) return;
        Organization organization = owner.getOrganization();
        tenantContext.activate(organization.getId());
        if (organization.getStatus() == com.peditto.organizations.domain.OrganizationStatus.PENDING_VERIFICATION) {
            organization.activate();
            owner.activate();
            if (!trials.existsByOrganization_Id(organization.getId())) {
                Trial trial = new Trial(organization, now, now.plus(Duration.ofDays(30)), "mvp-trial-v1");
                trial.activate();
                trials.save(trial);
                audit.record("IDENTITY_ONBOARDING_COMPLETED", "organization", organization.getId(), user.getId(), organization.getId());
            }
        }
    }
}
