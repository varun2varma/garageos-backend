package com.garageos.core.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    /**
     * Stable machine-readable code for this specific error, or null when
     * there isn't one worth naming — see ApiError.code's doc comment.
     */
    private final String code;

    public BusinessException(String message) {
        super(message);
        this.code = null;
    }

    public BusinessException(String message, String code) {
        super(message);
        this.code = code;
    }

}