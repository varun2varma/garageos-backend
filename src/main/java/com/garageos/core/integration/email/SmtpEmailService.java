package com.garageos.core.integration.email;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * SMTP implementation. Credentials and host come only from Spring's standard
 * {@code spring.mail.*} settings (env: SPRING_MAIL_HOST, SPRING_MAIL_PORT,
 * SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD, ...); the switch is
 * {@code communication.email.enabled} (EMAIL_ENABLED, default false) and the
 * sender address {@code communication.email.from} (EMAIL_FROM).
 *
 * Without {@code spring.mail.host} Spring creates no {@link JavaMailSender},
 * the app still starts, and {@link #isEnabled()} is false.
 */
@Slf4j
@Service
public class SmtpEmailService implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;

    public SmtpEmailService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${communication.email.enabled:false}") boolean enabled,
            @Value("${communication.email.from:}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    @Override
    public boolean isEnabled() {
        return enabled && mailSender.getIfAvailable() != null && from != null && !from.isBlank();
    }

    @Override
    public void send(EmailMessage message) {

        if (!isEnabled()) {
            throw new EmailDeliveryException("E-mail delivery is not enabled or not configured.");
        }

        try {
            JavaMailSender sender = mailSender.getObject();
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());

            helper.setFrom(from);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.textBody(), false);

            if (message.attachments() != null) {
                for (EmailAttachment a : message.attachments()) {
                    helper.addAttachment(a.fileName(), new ByteArrayResource(a.content()), a.contentType());
                }
            }

            sender.send(mime);

        } catch (Exception e) {
            // Class name only: mail exceptions can echo server responses.
            log.error("[EMAIL] Send failed: {}", e.getClass().getSimpleName());
            throw new EmailDeliveryException("E-mail could not be sent: " + e.getClass().getSimpleName(), e);
        }
    }
}
