package com.peditto.identity.application;

import com.peditto.identity.domain.AppUser;
import com.peditto.common.persistence.AuditWriter;
import com.peditto.identity.domain.AuthChallengePurpose;
import com.peditto.identity.domain.OtpChannel;
import com.peditto.identity.domain.UserDocument;
import com.peditto.identity.infrastructure.AppUserRepository;
import com.peditto.identity.infrastructure.UserDocumentRepository;
import com.peditto.organizations.domain.Membership;
import com.peditto.organizations.domain.MembershipRole;
import com.peditto.organizations.domain.Organization;
import com.peditto.organizations.infrastructure.MembershipRepository;
import com.peditto.organizations.infrastructure.OrganizationRepository;
import com.peditto.stores.domain.Store;
import com.peditto.stores.domain.StoreSettings;
import com.peditto.stores.infrastructure.StoreRepository;
import com.peditto.stores.infrastructure.StoreSettingsRepository;
import java.time.ZoneId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final OrganizationRepository organizations;
    private final AppUserRepository users;
    private final UserDocumentRepository documents;
    private final MembershipRepository memberships;
    private final StoreRepository stores;
    private final StoreSettingsRepository settings;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService otpDelivery;
    private final StoreSlugFactory slugFactory;
    private final AuditWriter audit;

    public RegistrationService(OrganizationRepository organizations, AppUserRepository users,
            UserDocumentRepository documents, MembershipRepository memberships, StoreRepository stores,
            StoreSettingsRepository settings, PasswordEncoder passwordEncoder,
            OtpDeliveryService otpDelivery, StoreSlugFactory slugFactory, AuditWriter audit) {
        this.organizations = organizations; this.users = users; this.documents = documents;
        this.memberships = memberships; this.stores = stores; this.settings = settings;
        this.passwordEncoder = passwordEncoder; this.otpDelivery = otpDelivery; this.slugFactory = slugFactory;
        this.audit = audit;
    }

    @Transactional
    public void register(RegistrationInput input) {
        Organization organization = organizations.saveAndFlush(new Organization(input.documentType(), input.document(),
                input.legalName(), input.storeName()));
        AppUser user = users.saveAndFlush(new AppUser(input.email(), input.phoneE164(), passwordEncoder.encode(input.password())));
        documents.save(new UserDocument(user, input.documentType(), input.document()));
        memberships.save(new Membership(organization, user, MembershipRole.OWNER));
        String slug = slugFactory.uniqueSlug(input.storeName(), stores::existsBySlug);
        Store store = stores.saveAndFlush(new Store(organization, slug, input.storeName(), ZoneId.of("America/Sao_Paulo")));
        settings.save(new StoreSettings(organization, store));

        // Trial is created only after both contact channels are verified.
        otpDelivery.issue(user, AuthChallengePurpose.VERIFY_EMAIL, OtpChannel.EMAIL, user.getEmailNormalized());
        otpDelivery.issue(user, AuthChallengePurpose.VERIFY_PHONE, OtpChannel.SMS, user.getPhoneE164());
        audit.record("IDENTITY_REGISTRATION_STARTED", "organization", organization.getId(), user.getId(), organization.getId());
    }
}
