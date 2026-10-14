package com.peditto.common.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Refuses to run the OTP worker when either user-selected channel is only partially configured. */
@Component
public class OtpDeliveryConfigurationValidator implements ApplicationRunner {
    private final boolean enabled;
    private final String smtpHost;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String mailFrom;
    private final String accountSid;
    private final String apiKeySid;
    private final String apiKeySecret;
    private final String messagingServiceSid;

    public OtpDeliveryConfigurationValidator(
            @Value("${peditto.identity.delivery-enabled:false}") boolean enabled,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${spring.mail.username:}") String smtpUsername,
            @Value("${spring.mail.password:}") String smtpPassword,
            @Value("${peditto.identity.mail-from:}") String mailFrom,
            @Value("${peditto.identity.twilio-account-sid:}") String accountSid,
            @Value("${peditto.identity.twilio-api-key-sid:}") String apiKeySid,
            @Value("${peditto.identity.twilio-api-key-secret:}") String apiKeySecret,
            @Value("${peditto.identity.twilio-messaging-service-sid:}") String messagingServiceSid) {
        this.enabled = enabled;
        this.smtpHost = smtpHost;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.mailFrom = mailFrom;
        this.accountSid = accountSid;
        this.apiKeySid = apiKeySid;
        this.apiKeySecret = apiKeySecret;
        this.messagingServiceSid = messagingServiceSid;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (blank(smtpHost) || blank(smtpUsername) || blank(smtpPassword) || blank(mailFrom)
                || !accountSid.matches("AC[0-9a-fA-F]{32}")
                || !apiKeySid.matches("SK[0-9a-fA-F]{32}") || blank(apiKeySecret)
                || !messagingServiceSid.matches("MG[0-9a-fA-F]{32}")) {
            throw new IllegalStateException("OTP delivery is enabled but required SMTP/Twilio settings are incomplete.");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
