package com.garageos.core.exception;

import com.garageos.core.api.error.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Backend hardening: IllegalStateException (thrown by
 * JobCardStatusValidator and other lifecycle guards for an invalid state
 * transition) previously fell through to the generic
 * @ExceptionHandler(Exception.class) catch-all and returned HTTP 500,
 * even though it is a business/lifecycle validation failure, not a
 * server failure. Now maps to the same 400 shape as BusinessException.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalStateException_mapsTo400_notServerError() {

        ResponseEntity<ApiError> response =
                handler.handleIllegalStateException(
                        new IllegalStateException("Invalid Job Card status transition: CLOSED -> CLOSED"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Invalid Job Card status transition: CLOSED -> CLOSED");
        assertThat(response.getBody().isSuccess()).isFalse();
    }
}
