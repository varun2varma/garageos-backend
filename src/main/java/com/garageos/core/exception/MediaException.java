package com.garageos.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * A media failure the client can be told something useful about.
 *
 * Media upload previously surfaced two unhelpful things: a bare 500
 * "Something went wrong." whenever an unmapped exception escaped, and a
 * 400 "Failed to upload media to Google Drive." that flattened every
 * Google failure - expired token, revoked grant, missing folder,
 * insufficient scope, network error - into one message with the wrong
 * HTTP status. Neither told the client what had actually gone wrong, and
 * the real Google error was visible only in the logs.
 *
 * This carries a stable {@link MediaErrorCode} plus the HTTP status that
 * genuinely fits, so a client can distinguish "you need to reauthorize
 * Google Drive" from "the file you sent is invalid" from "Drive is
 * failing right now".
 *
 * The cause is always attached for the logs and is never sent to the
 * client: Google's exceptions can contain request URLs and token
 * metadata, which must not leave the server.
 */
@Getter
public class MediaException extends RuntimeException {

    public enum MediaErrorCode {

        /** Google Drive has never been authorized, or the stored grant is gone. */
        MEDIA_DRIVE_AUTH_REQUIRED(HttpStatus.SERVICE_UNAVAILABLE),

        /** A credential exists but Google rejected it (expired, revoked, wrong scope). */
        MEDIA_DRIVE_AUTH_FAILED(HttpStatus.SERVICE_UNAVAILABLE),

        /** Drive accepted the credential but the operation itself failed. */
        MEDIA_DRIVE_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY),

        /** The file reached Drive but its metadata could not be persisted. */
        MEDIA_METADATA_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR),

        /** The request itself is wrong - missing stage, empty file, bad content type. */
        MEDIA_INVALID_REQUEST(HttpStatus.BAD_REQUEST);

        private final HttpStatus status;

        MediaErrorCode(HttpStatus status) {
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }

    private final MediaErrorCode errorCode;

    public MediaException(MediaErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MediaException(MediaErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }
}
