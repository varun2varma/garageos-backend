package com.garageos.core.exception;

import lombok.Getter;

/**
 * The request is well formed but conflicts with the current state of the resource (HTTP 409): for example
 * correcting the classification of an estimate that already has an invoice. Carries a stable machine-readable code.
 */
@Getter
public class ConflictException extends RuntimeException {

    private final String code;

    public ConflictException(String message, String code) {
        super(message);
        this.code = code;
    }
}
