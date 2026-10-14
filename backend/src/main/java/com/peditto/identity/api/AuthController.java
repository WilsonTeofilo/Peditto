package com.peditto.identity.api;

import com.peditto.identity.application.ContactVerificationService;
import com.peditto.identity.application.LoginService;
import com.peditto.identity.application.PasswordRecoveryService;
import com.peditto.identity.application.RegistrationFacade;
import com.peditto.identity.application.RegistrationInput;
import com.peditto.identity.application.SessionCookieFactory;
import com.peditto.identity.application.SessionManagementService;
import com.peditto.identity.domain.AppUser;
import com.peditto.identity.domain.OtpChannel;
import com.peditto.identity.infrastructure.AppUserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** DTO-only authentication API; the API is absent until both OTP channels are configured. */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "peditto.identity.delivery-enabled", havingValue = "true")
public class AuthController {
    private final RegistrationFacade registrations;
    private final ContactVerificationService contactVerification;
    private final LoginService login;
    private final SessionManagementService sessions;
    private final SessionCookieFactory cookies;
    private final AppUserRepository users;
    private final com.peditto.identity.application.PasswordRecoveryService recovery;

    public AuthController(RegistrationFacade registrations, ContactVerificationService contactVerification,
            LoginService login, SessionManagementService sessions, SessionCookieFactory cookies,
            AppUserRepository users, PasswordRecoveryService recovery) {
        this.registrations = registrations;
        this.contactVerification = contactVerification;
        this.login = login;
        this.sessions = sessions;
        this.cookies = cookies;
        this.users = users;
        this.recovery = recovery;
    }

    @PostMapping("/registrations")
    public ResponseEntity<Accepted> register(@Valid @RequestBody RegistrationRequest request) {
        RegistrationInput input = RegistrationInput.from(request);
        registrations.register(input);
        return ResponseEntity.accepted().body(new Accepted("verification_required"));
    }

    @PostMapping("/registrations/verify")
    public ResponseEntity<Accepted> verifyContact(@Valid @RequestBody ContactVerificationRequest request) {
        contactVerification.verify(request.email(), request.channel(), request.code());
        return ResponseEntity.accepted().body(new Accepted("verification_required"));
    }

    @PostMapping("/login")
    public LoginStartResponse beginLogin(@Valid @RequestBody LoginRequest request) {
        LoginService.LoginStart result = login.begin(request.identifier(), request.password());
        return new LoginStartResponse(result.preAuthenticationToken(), result.availableChannels(), result.status());
    }

    @PostMapping("/login/codes")
    public ResponseEntity<Accepted> requestLoginCode(@Valid @RequestBody LoginCodeRequest request) {
        login.requestCode(request.preAuthenticationToken(), request.channel());
        return ResponseEntity.accepted().body(new Accepted("verification_required"));
    }

    @PostMapping("/login/verify")
    public ResponseEntity<Accepted> verifyLogin(@Valid @RequestBody LoginVerificationRequest request,
            HttpServletResponse response) {
        return login.verifyAndCreateSession(request.preAuthenticationToken(), request.channel(), request.code())
                .map(token -> {
                    response.addHeader("Set-Cookie", cookies.issue(token).toString());
                    return ResponseEntity.ok(new Accepted("authenticated"));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new Accepted("verification_required")));
    }

    @PostMapping("/password-recovery")
    public ResponseEntity<Accepted> requestPasswordRecovery(@Valid @RequestBody RecoveryRequest request) {
        recovery.request(request.email());
        return ResponseEntity.accepted().body(new Accepted("verification_required"));
    }

    @PostMapping("/password-recovery/verify")
    public ResponseEntity<?> confirmPasswordRecovery(@Valid @RequestBody RecoveryConfirmationRequest request) {
        boolean changed = recovery.reset(request.email(), request.emailCode(), request.smsCode(), request.newPassword());
        if (!changed) return ResponseEntity.badRequest().body(new Accepted("verification_required"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserSummary currentUser(Authentication authentication) {
        UUID userId = requireUserId(authentication);
        AppUser user = users.findById(userId).orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));
        return new UserSummary(user.getEmailNormalized(), maskPhone(user.getPhoneE164()),
                user.getEmailVerifiedAt() != null, user.getPhoneVerifiedAt() != null);
    }

    @PostMapping("/sessions/current/rotate")
    public ResponseEntity<Accepted> rotate(HttpServletRequest request, HttpServletResponse response) {
        String token = sessionCookie(request);
        return sessions.rotate(token)
                .map(replacement -> {
                    response.addHeader("Set-Cookie", cookies.issue(replacement).toString());
                    return ResponseEntity.ok(new Accepted("rotated"));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new Accepted("authentication_required")));
    }

    @DeleteMapping("/sessions/current")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        sessions.revokeCurrent(sessionCookie(request));
        response.addHeader("Set-Cookie", cookies.clear().toString());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<Void> logoutEverywhere(Authentication authentication, HttpServletResponse response) {
        sessions.revokeAll(requireUserId(authentication));
        response.addHeader("Set-Cookie", cookies.clear().toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    public List<SessionManagementService.SessionSummary> activeSessions(Authentication authentication,
            HttpServletRequest request) {
        return sessions.listActive(requireUserId(authentication), sessionCookie(request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID sessionId, Authentication authentication,
            HttpServletRequest request, HttpServletResponse response) {
        SessionManagementService.SessionRevocation result = sessions.revoke(
                requireUserId(authentication), sessionId, sessionCookie(request));
        if (result.current()) response.addHeader("Set-Cookie", cookies.clear().toString());
        return ResponseEntity.noContent().build();
    }

    private UUID requireUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) return userId;
        throw new org.springframework.security.access.AccessDeniedException("Authentication is required.");
    }

    private String sessionCookie(HttpServletRequest request) {
        Cookie[] requestCookies = request.getCookies();
        if (requestCookies == null) return null;
        for (Cookie cookie : requestCookies) {
            if (com.peditto.common.security.SessionAuthenticationFilter.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }

    public record Accepted(String status) {}
    public record LoginStartResponse(String preAuthenticationToken, List<OtpChannel> availableChannels, String status) {}
    public record UserSummary(String email, String maskedPhone, boolean emailVerified, boolean phoneVerified) {}
    public record RecoveryRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record RecoveryConfirmationRequest(@NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$") String emailCode,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$") String smsCode,
            @NotBlank @Size(min = 12, max = 128) String newPassword) {
        @Override public String toString() { return "RecoveryConfirmationRequest[email=[redacted], emailCode=[redacted], smsCode=[redacted], newPassword=[redacted]]"; }
    }

    public record ContactVerificationRequest(@NotBlank @Email @Size(max = 254) String email,
            @NotNull OtpChannel channel, @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code) {}
    public record LoginRequest(@NotBlank @Size(max = 254) String identifier,
            @NotBlank @Size(min = 12, max = 128) String password) {
        @Override public String toString() { return "LoginRequest[identifier=[redacted], password=[redacted]]"; }
    }
    public record LoginCodeRequest(@NotBlank @Size(max = 100) String preAuthenticationToken,
            @NotNull OtpChannel channel) {
        @Override public String toString() { return "LoginCodeRequest[preAuthenticationToken=[redacted], channel=" + channel + "]"; }
    }
    public record LoginVerificationRequest(@NotBlank @Size(max = 100) String preAuthenticationToken,
            @NotNull OtpChannel channel, @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code) {
        @Override public String toString() { return "LoginVerificationRequest[preAuthenticationToken=[redacted], channel=" + channel + ", code=[redacted]]"; }
    }
}
