package com.garageos.core.api.error;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiError {

    boolean success;

    String message;

    /**
     * Stable machine-readable identifier for a specific, known business
     * error (e.g. "NAVIGATION_ALREADY_ASSIGNED", "QUALITY_CHECK_NOT_AVAILABLE")
     * so a client can react to *which* error this is without parsing the
     * human-readable message. Null for errors with no such stable
     * identity (validation errors, generic exceptions) — the message
     * remains the only signal for those, exactly as before this field was
     * added.
     */
    String code;

    List<FieldError> errors;

    LocalDateTime timestamp;

    String requestId;

}