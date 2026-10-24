package com.garageos.core.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Rate-limit thresholds. Every policy has a documented production default here and can be overridden per
 * deployment, for example {@code garagest.rate-limit.policies.login-ip.limit=40}
 * in application properties or as a system property.
 *
 * <p>Defaults are sized for legitimate use first (a garage's staff share one NAT address, so the per-IP limits are
 * generous); the per-account/identifier limits are the tight ones.
 */
@org.springframework.stereotype.Component
@ConfigurationProperties(prefix = "garagest.rate-limit")
public class RateLimitProperties {

    /** Master switch. Leave on in production; off only for local debugging. */
    private boolean enabled = true;

    private final Map<String, Policy> policies = new HashMap<>();

    public RateLimitProperties() {
        // limit hits per windowSeconds
        policies.put("login-ip", new Policy(30, 300));
        policies.put("login-identifier", new Policy(10, 900));
        policies.put("otp-send-ip", new Policy(20, 3600));
        policies.put("otp-verify-ip", new Policy(60, 3600));
        policies.put("register-ip", new Policy(10, 3600));
        policies.put("places-user", new Policy(120, 60));
        policies.put("nearby-user", new Policy(120, 60));
        policies.put("booking-create-user", new Policy(30, 3600));
        policies.put("media-upload-user", new Policy(600, 3600));
        policies.put("admin-stepup-user", new Policy(5, 900));
        policies.put("bank-stepup-user", new Policy(5, 900));
        policies.put("emergency-ip", new Policy(10, 3600));
        policies.put("emergency-phone", new Policy(5, 3600));
        // Separate from the verification-code allowance: resending a code must never use up the right to ask for help.
        policies.put("emergency-create-phone", new Policy(5, 3600));
        policies.put("emergency-status-ip", new Policy(60, 600));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Policy> getPolicies() {
        return policies;
    }

    public static class Policy {

        private int limit;
        private int windowSeconds;

        public Policy() {
        }

        public Policy(int limit, int windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
