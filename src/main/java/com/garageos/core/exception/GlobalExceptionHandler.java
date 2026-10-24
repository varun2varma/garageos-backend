package com.garageos.core.exception;

import com.garageos.modules.otp.exception.OtpException;
import com.garageos.core.api.error.ApiError;
import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.core.util.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.garageos.core.api.error.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex) {

        List<FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
                .toList();

        ApiError error = ApiError.builder()
                .success(false)
                .message("Validation Failed")
                .errors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getRequestId())
                .build();

        return ResponseEntity.badRequest().body(error);
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {

        // Explicit JSON content type: the error must stay readable even on a route whose success
        // response is not JSON (e.g. a PDF download) and for a client whose Accept header excludes JSON.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildError(ex.getMessage(), ex.getCode()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflictException(ConflictException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError(ex.getMessage(), ex.getCode()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex) {

        // Explicit JSON content type: see handleResourceNotFound.
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildError(ex.getMessage(), ex.getCode()));
    }

    /**
     * Backend hardening: JobCardStatusValidator (and other lifecycle
     * guards) throw IllegalStateException for an invalid state
     * transition - e.g. closing an already-CLOSED JobCard, or closing
     * from an earlier state. That is a business/lifecycle validation
     * failure, not a server failure, so it belongs on the same 400
     * response shape as BusinessException, not the generic 500
     * catch-all below.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalStateException(IllegalStateException ex) {

        return ResponseEntity.badRequest()
                .body(buildError(ex.getMessage()));
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiError> handleException(Exception ex) {
//
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(buildError("Something went wrong."));
//    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceAlreadyExistsException(
            ResourceAlreadyExistsException ex) {

        return ApiResponseUtil.badRequest(ex.getMessage());
    }

//    @ExceptionHandler(ResourceNotFoundException.class)
//    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
//            ResourceNotFoundException ex) {
//
//        return ApiResponseUtil.notFound(ex.getMessage());
//    }

    /**
     * Media failures carry a stable code and the status that genuinely
     * fits, instead of every Google Drive problem collapsing into one
     * message. The cause is logged in full and never returned: Google's
     * exceptions can carry request URLs and token metadata.
     */
    @ExceptionHandler(MediaException.class)
    public ResponseEntity<ApiError> handleMediaException(MediaException ex) {

        log.error(
                "[MEDIA] {} - {}",
                ex.getErrorCode(),
                ex.getMessage(),
                ex
        );

        ApiError error = ApiError.builder()
                .success(false)
                .message(ex.getErrorCode().name() + ": " + ex.getMessage())
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getRequestId())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    /**
     * Corrective fix: AccessDeniedException (both from @PreAuthorize
     * checks and from manual ownership checks such as
     * VehicleServiceImpl.updateVehicle) had no handler at all, so every
     * authorization failure fell through to the generic catch-all below
     * and reached the client as a bare 500 "Something went wrong.",
     * indistinguishable from a real server error.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(
            AccessDeniedException ex) {

        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildError(ex.getMessage()));
    }

    /**
     * Corrective fix: IllegalArgumentException had no handler at all, so
     * every "bad request" the service raised - a missing media stage, an
     * empty file, an unsupported content type - fell through to the
     * catch-all below and reached the client as a bare 500 "Something
     * went wrong.", with no indication of what the caller had got wrong.
     *
     * These are genuinely client errors and its message is written for
     * the caller, so it is returned as a 400 with that message.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.warn("Rejected request: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(buildError(ex.getMessage()));
    }

    /**
     * Corrective fix: a request body exceeding Spring's configured
     * multipart limits (application.properties: spring.servlet.multipart.
     * max-file-size/max-request-size) had no handler, so it fell through
     * to the generic catch-all below and reached the client as a bare 500
     * "Something went wrong." - indistinguishable from a real server
     * failure and easy to misdiagnose as a video-upload bug rather than a
     * client payload that was simply too large for this endpoint.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {

        log.warn("Rejected request: upload exceeds the configured size limit");

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(buildError("File exceeds the maximum upload size for this endpoint."));
    }

    /**
     * OTP failures carry a stable code (OTP_INVALID, OTP_EXPIRED, ...) and the
     * status that fits (400 / 409 / 429 / 502 / 503). The message never
     * contains the OTP or provider detail.
     */
    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ApiError> handleOtpException(OtpException ex) {

        log.warn("[OTP] {}", ex.getCode());

        return ResponseEntity.status(ex.getCode().status())
                .body(buildError(ex.getMessage(), ex.getCode().name()));
    }

    /**
     * Last resort. The client still gets a deliberately generic message -
     * an unexpected failure must never leak internals - but the cause is
     * now logged with its stack trace. Previously it was swallowed
     * entirely, which is why "Something went wrong." was untraceable.
     */
    /**
     * Wrong password, unknown/ambiguous identifier, disabled and locked accounts all end here with ONE message, so
     * the response never reveals whether an account exists or why it was refused. Previously these fell through to
     * the catch-all below and surfaced as a 500 "Something went wrong."
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimit(RateLimitExceededException ex) {

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildError(ex.getMessage(), "RATE_LIMITED"));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            org.springframework.security.core.AuthenticationException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildError("Invalid username or password."));
    }

    /**
     * A request the client built wrongly (missing or mistyped parameter/header, unreadable body) is a 400, not a
     * server failure: before this it fell into the catch-all below, was logged as an ERROR with a stack trace and was
     * reported to the caller as "Something went wrong." with a 500. The message never echoes the offending value.
     */
    @ExceptionHandler({
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(Exception ex) {

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildError("The request is incomplete or malformed."));
    }

    /**
     * A URL that matches no controller and no packaged static file is a plain 404. Before this it fell into the
     * catch-all below: a 500 "Something went wrong." plus an ERROR stack trace for every probe of a removed page.
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildError("Not found."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {

        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        return ApiResponseUtil.internalServerError(
                "Something went wrong."
        );
    }

    private ApiError buildError(String message) {
        return buildError(message, null);
    }

    private ApiError buildError(String message, String code) {

        return ApiError.builder()
                .success(false)
                .message(message)
                .code(code)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getRequestId())
                .build();
    }
}