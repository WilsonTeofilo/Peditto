package com.peditto.common.outbox;

import com.peditto.identity.domain.OtpChannel;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends OTP through SMTP or the fixed Twilio API host. Provider secrets come from the runtime environment. */
@Component
public class ConfiguredOtpMessageGateway implements OtpMessageGateway {
    private static final String TWILIO_MESSAGES = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final String mailFrom;
    private final String twilioAccountSid;
    private final String twilioApiKeySid;
    private final String twilioApiKeySecret;
    private final String twilioMessagingServiceSid;

    public ConfiguredOtpMessageGateway(ObjectProvider<JavaMailSender> mailSenders,
            @Value("${peditto.identity.mail-from:}") String mailFrom,
            @Value("${peditto.identity.twilio-account-sid:}") String twilioAccountSid,
            @Value("${peditto.identity.twilio-api-key-sid:}") String twilioApiKeySid,
            @Value("${peditto.identity.twilio-api-key-secret:}") String twilioApiKeySecret,
            @Value("${peditto.identity.twilio-messaging-service-sid:}") String twilioMessagingServiceSid) {
        this.mailSenders = mailSenders;
        this.mailFrom = mailFrom;
        this.twilioAccountSid = twilioAccountSid;
        this.twilioApiKeySid = twilioApiKeySid;
        this.twilioApiKeySecret = twilioApiKeySecret;
        this.twilioMessagingServiceSid = twilioMessagingServiceSid;
    }

    @Override
    public void send(OtpChannel channel, String destination, String code) {
        String body = "Seu código Peditto é " + code + ". Ele expira em 10 minutos. Não compartilhe.";
        if (channel == OtpChannel.EMAIL) sendEmail(destination, body);
        else sendSms(destination, body);
    }

    private void sendEmail(String destination, String body) {
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null || mailFrom.isBlank()) throw new DeliveryException("email_not_configured");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(destination);
        message.setSubject("Código de segurança Peditto");
        message.setText(body);
        sender.send(message);
    }

    private void sendSms(String destination, String body) {
        if (!twilioAccountSid.matches("AC[0-9a-fA-F]{32}")
                || !twilioApiKeySid.matches("SK[0-9a-fA-F]{32}")
                || twilioApiKeySecret.isBlank()
                || !twilioMessagingServiceSid.matches("MG[0-9a-fA-F]{32}")) {
            throw new DeliveryException("sms_not_configured");
        }
        String form = "To=" + encode(destination) + "&MessagingServiceSid=" + encode(twilioMessagingServiceSid)
                + "&Body=" + encode(body);
        String credentials = Base64.getEncoder().encodeToString(
                (twilioApiKeySid + ":" + twilioApiKeySecret).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(TWILIO_MESSAGES.formatted(twilioAccountSid)))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try {
            int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status < 200 || status >= 300) throw new DeliveryException("sms_provider_http_" + status);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new DeliveryException("sms_provider_interrupted");
        } catch (java.io.IOException failure) {
            throw new DeliveryException("sms_provider_unavailable");
        }
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    public static class DeliveryException extends RuntimeException {
        public DeliveryException(String safeCode) { super(safeCode); }
    }
}
