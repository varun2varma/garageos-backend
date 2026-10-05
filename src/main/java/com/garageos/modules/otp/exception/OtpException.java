package com.garageos.modules.otp.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Every OTP failure the API reports. {@code code} is the stable
 * machine-readable value clients branch on; {@code message} is safe to show.
 * Never carries the OTP value or any provider detail.
 */
@Getter
public class OtpException extends RuntimeException {

    public enum Code {
        OTP_REQUIRED(HttpStatus.BAD_REQUEST),
        OTP_INVALID(HttpStatus.BAD_REQUEST),
        OTP_EXPIRED(HttpStatus.BAD_REQUEST),
        OTP_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS),
        OTP_ALREADY_VERIFIED(HttpStatus.CONFLICT),
        OTP_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS),
        OTP_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
        OTP_INVALID_DESTINATION(HttpStatus.BAD_REQUEST),
        OTP_PROVIDER_FAILURE(HttpStatus.BAD_GATEWAY),
        OTP_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE),
        OTP_DISABLED(HttpStatus.SERVICE_UNAVAILABLE);

        private final HttpStatus status;

        Code(HttpStatus status) {
            this.status = status;
        }

        public HttpStatus status() {
            return status;
        }
    }

    private final Code code;

    public OtpException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public OtpException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
