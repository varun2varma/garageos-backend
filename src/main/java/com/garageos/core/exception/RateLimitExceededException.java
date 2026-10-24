package com.garageos.core.exception;

/** A rate-limit policy was exceeded; mapped to HTTP 429 with a Retry-After header and a generic message. */
public class RateLimitExceededException extends RuntimeException {

    private final String policy;
    private final int retryAfterSeconds;

    public RateLimitExceededException(String policy, int retryAfterSeconds) {
        super("Too many requests. Please try again later.");
        this.policy = policy;
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public String getPolicy() {
        return policy;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
