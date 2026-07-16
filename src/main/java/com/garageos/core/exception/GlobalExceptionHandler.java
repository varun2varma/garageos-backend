package com.garageos.core.exception;

import com.garageos.core.api.error.ApiError;
import com.garageos.core.util.RequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.garageos.core.api.error.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
                .body(buildError(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex) {

        return ResponseEntity.badRequest()
                .body(buildError(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError("Something went wrong."));
    }

    private ApiError buildError(String message) {

        return ApiError.builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getRequestId())
                .build();
    }
}