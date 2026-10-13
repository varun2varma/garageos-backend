package com.garageos.modules.identity.security;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Layered throttling for the public password-recovery endpoints, on top of (never instead of) the per-number OTP
 * cooldown, hourly cap and per-challenge attempt limit in {@code OtpServiceImpl}.
 *
 * Layers: per client address, per identifier, and "many different identifiers from one address" (account sweeping).
 * All checks are identifier-independent in what they reveal: a blocked send is answered exactly like an accepted one.
 *
 * State is in memory (one application instance, like the account-deletion limiter): it resets on restart and is not
 * shared between instances. If the service is scaled out, move these counters to the database or a shared cache.
 */
@Component
public class PasswordRecoveryThrottle {

    static final long WINDOW_MS = 15 * 60 * 1000L;
    static final long HOUR_MS = 60 * 60 * 1000L;

    static final int SENDS_PER_IP = 10;
    static final int SENDS_PER_IDENTIFIER_PER_HOUR = 5;
    static final int DISTINCT_IDENTIFIERS_PER_IP = 5;
    static final int VERIFIES_PER_IP = 20;
    static final int VERIFIES_PER_IDENTIFIER = 10;
    static final int RESETS_PER_IP = 10;

    private static final int MAX_KEYS = 50_000;

    private final LongSupplier clock;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Long>> identifiersByIp = new ConcurrentHashMap<>();

    public PasswordRecoveryThrottle() {
        this(System::currentTimeMillis);
    }

    PasswordRecoveryThrottle(LongSupplier clock) {
        this.clock = clock;
    }

    /** @return false when this OTP request must be silently dropped (the caller still answers 204). */
    public boolean allowSend(String ip, String identifier) {

        String id = normalise(identifier);

        // Evaluate every layer first, record afterwards: a request refused by one layer must not consume the others.
        boolean ok = under("send-ip:" + ip, SENDS_PER_IP, WINDOW_MS)
                && under("send-id:" + id, SENDS_PER_IDENTIFIER_PER_HOUR, HOUR_MS)
                && distinctIdentifiersOk(ip, id);

        if (ok) {
            record("send-ip:" + ip);
            record("send-id:" + id);
            noteIdentifier(ip, id);
        }
        return ok;
    }

    public boolean allowVerify(String ip, String identifier) {

        String id = normalise(identifier);

        boolean ok = under("verify-ip:" + ip, VERIFIES_PER_IP, WINDOW_MS)
                && under("verify-id:" + id, VERIFIES_PER_IDENTIFIER, WINDOW_MS);

        if (ok) {
            record("verify-ip:" + ip);
            record("verify-id:" + id);
        }
        return ok;
    }

    public boolean allowReset(String ip) {

        boolean ok = under("reset-ip:" + ip, RESETS_PER_IP, WINDOW_MS);

        if (ok) {
            record("reset-ip:" + ip);
        }
        return ok;
    }

    // ------------------------------------------------------------------ internals

    private static String normalise(String identifier) {
        String s = identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
        return s.length() > 150 ? s.substring(0, 150) : s;
    }

    private boolean under(String key, int limit, long windowMs) {
        Deque<Long> d = hits.get(key);
        if (d == null) {
            return true;
        }
        synchronized (d) {
            prune(d, windowMs);
            return d.size() < limit;
        }
    }

    private void record(String key) {
        if (hits.size() > MAX_KEYS) {
            sweep();
        }
        Deque<Long> d = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (d) {
            d.addLast(clock.getAsLong());
        }
    }

    private boolean distinctIdentifiersOk(String ip, String id) {
        Map<String, Long> seen = identifiersByIp.get(ip);
        if (seen == null) {
            return true;
        }
        synchronized (seen) {
            long now = clock.getAsLong();
            seen.values().removeIf(t -> now - t > WINDOW_MS);
            return seen.containsKey(id) || seen.size() < DISTINCT_IDENTIFIERS_PER_IP;
        }
    }

    private void noteIdentifier(String ip, String id) {
        if (identifiersByIp.size() > MAX_KEYS) {
            sweep();
        }
        Map<String, Long> seen = identifiersByIp.computeIfAbsent(ip, k -> new java.util.HashMap<>());
        synchronized (seen) {
            seen.put(id, clock.getAsLong());
        }
    }

    private void prune(Deque<Long> d, long windowMs) {
        long cutoff = clock.getAsLong() - windowMs;
        while (!d.isEmpty() && d.peekFirst() < cutoff) {
            d.pollFirst();
        }
    }

    /** Bounded memory: drop everything that has aged out of the longest window. */
    private void sweep() {
        long cutoff = clock.getAsLong() - HOUR_MS;
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || e.getValue().peekLast() < cutoff;
            }
        });
        identifiersByIp.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Set<Long> times = new HashSet<>(e.getValue().values());
                return times.stream().allMatch(t -> t < cutoff);
            }
        });
    }
}
