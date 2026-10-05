package com.garageos.core.integration.email;

import java.util.List;

/**
 * Outbound e-mail. The one e-mail seam in the backend: callers build an
 * {@link EmailMessage} and never touch a mail library, so the transport can
 * change without touching any flow.
 */
public interface EmailService {

    /** Whether e-mail delivery is switched on and configured (EMAIL_ENABLED + an SMTP host). */
    boolean isEnabled();

    /**
     * Sends one message.
     *
     * @throws EmailDeliveryException when delivery is not possible or fails (never includes credentials)
     */
    void send(EmailMessage message);

    record EmailMessage(String to, String subject, String textBody, List<EmailAttachment> attachments) {
    }

    record EmailAttachment(String fileName, String contentType, byte[] content) {
    }
}
