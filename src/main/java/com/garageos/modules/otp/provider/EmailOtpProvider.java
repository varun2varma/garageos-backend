package com.garageos.modules.otp.provider;

import com.garageos.core.enums.otp.OtpChannel;
import com.garageos.core.enums.otp.OtpPurpose;
import com.garageos.core.integration.email.EmailDeliveryException;
import com.garageos.core.integration.email.EmailService;
import com.garageos.modules.otp.config.OtpProperties;
import com.garageos.modules.otp.exception.OtpException;
import com.garageos.modules.otp.exception.OtpException.Code;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Delivers an OTP by e-mail through the one e-mail seam ({@link EmailService}). It is a pure transport, exactly like
 * the SMS provider: the code is generated, hashed, expired and attempt-limited by {@code OtpServiceImpl}, so e-mail
 * inherits every existing OTP rule rather than re-implementing any.
 *
 * <p>It is "configured" only when e-mail delivery itself is switched on with an SMTP host; with no credentials it
 * reports unconfigured and e-mail OTP is refused rather than silently skipped. The message carries the code and the
 * expiry only, and the code is never logged.
 */
@Component
public class EmailOtpProvider implements OtpProvider {

    private final EmailService emailService;
    private final OtpProperties props;

    public EmailOtpProvider(EmailService emailService, OtpProperties props) {
        this.emailService = emailService;
        this.props = props;
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.EMAIL;
    }

    @Override
    public String name() {
        return "SMTP_EMAIL";
    }

    @Override
    public boolean isConfigured() {
        return emailService.isEnabled();
    }

    @Override
    public String send(String destination, String otp, OtpPurpose purpose) {

        String minutes = String.valueOf(Math.max(1, props.getExpiry().toMinutes()));

        String body = "Your GarageST verification code is " + otp + ".\n\n"
                + "It expires in " + minutes + " minutes. If you did not ask for it, you can ignore this message; "
                + "nobody can use it without access to your account.\n\n"
                + "GarageST never asks you to share this code with anyone.";

        try {
            emailService.send(new EmailService.EmailMessage(
                    destination, "Your GarageST verification code", body, List.of()));
        } catch (EmailDeliveryException e) {
            // A delivery failure is the provider's failure: the challenge is rolled back and no attempt is counted.
            throw new OtpException(Code.OTP_PROVIDER_FAILURE,
                    "We could not send the e-mail right now. Please try again shortly.");
        }

        return null;
    }
}
