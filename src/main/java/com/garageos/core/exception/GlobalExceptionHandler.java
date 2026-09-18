package com.garageos.core.exception;

import com.garageos.core.api.error.ApiError;
import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.core.util.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.garageos.core.api.error.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError(ex.getMessage(), ex.getCode()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex) {

        return ResponseEntity.badRequest()
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
     * Last resort. The client still gets a deliberately generic message -
     * an unexpected failure must never leak internals - but the cause is
     * now logged with its stack trace. Previously it was swallowed
     * entirely, which is why "Something went wrong." was untraceable.
     */
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