package com.garageos.core.ratelimit;

import com.garageos.core.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Multi-instance rate limiting backed by PostgreSQL (the project has no Redis; every instance already shares the
 * database). A fixed window per policy and subject is advanced by ONE atomic upsert, so parallel requests across
 * instances cannot under-count.
 *
 * <ul>
 *   <li>The counter is committed in its own transaction, so a failed login that rolls back its caller's transaction
 *       still counts.</li>
 *   <li>Subjects are stored only as SHA-256 digests; nothing personal or secret is persisted or logged.</li>
 *   <li>If the counter store is unreachable the request is allowed and the failure is logged: a database blip must
 *       not lock every user out (the OTP service keeps its own database-checked caps).</li>
 *   <li>Expired rows are pruned on a schedule, so the table cannot grow without bound.</li>
 * </ul>
 *
 * Operational trade-off: this adds one small write per limited request. If traffic outgrows that, swap this class
 * for a Redis implementation behind the same {@link #require} contract.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private static final String UPSERT = """
            INSERT INTO rate_limit_bucket (bucket_key, window_start, hit_count, expires_at)
            VALUES (?, LOCALTIMESTAMP, 1, LOCALTIMESTAMP + (? * INTERVAL '1 second'))
            ON CONFLICT (bucket_key) DO UPDATE SET
                hit_count = CASE WHEN rate_limit_bucket.expires_at <= LOCALTIMESTAMP
                                 THEN 1 ELSE rate_limit_bucket.hit_count + 1 END,
                window_start = CASE WHEN rate_limit_bucket.expires_at <= LOCALTIMESTAMP
                                    THEN LOCALTIMESTAMP ELSE rate_limit_bucket.window_start END,
                expires_at = CASE WHEN rate_limit_bucket.expires_at <= LOCALTIMESTAMP
                                  THEN LOCALTIMESTAMP + (? * INTERVAL '1 second')
                                  ELSE rate_limit_bucket.expires_at END
            RETURNING hit_count, GREATEST(1, CEIL(EXTRACT(EPOCH FROM (expires_at - LOCALTIMESTAMP))))::int
            """;

    private final JdbcTemplate jdbc;
    private final RateLimitProperties properties;
    private final TransactionTemplate requiresNew;

    public RateLimitService(JdbcTemplate jdbc, RateLimitProperties properties, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.requiresNew = new TransactionTemplate(txManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Outcome of one counted hit. */
    public record Decision(boolean allowed, int count, int limit, int retryAfterSeconds) {
    }

    /** Counts one hit and throws {@link RateLimitExceededException} (HTTP 429) when the policy's limit is exceeded. */
    public void require(String policy, String subject) {

        Decision d = hit(policy, subject);

        if (!d.allowed()) {
            throw new RateLimitExceededException(policy, d.retryAfterSeconds());
        }
    }

    /** Counts one hit without throwing. An unknown policy, a disabled limiter or a store failure allows the request. */
    public Decision hit(String policy, String subject) {

        RateLimitProperties.Policy p = properties.getPolicies().get(policy);

        if (!properties.isEnabled() || p == null || p.getLimit() <= 0 || subject == null || subject.isBlank()) {
            return new Decision(true, 0, p == null ? 0 : p.getLimit(), 0);
        }

        String key = policy + ":" + digest(subject);

        try {
            int[] row = requiresNew.execute(status -> jdbc.queryForObject(
                    UPSERT,
                    (rs, i) -> new int[] {rs.getInt(1), rs.getInt(2)},
                    key, p.getWindowSeconds(), p.getWindowSeconds()));

            int count = row[0];
            boolean allowed = count <= p.getLimit();

            if (!allowed) {
                // Policy and counts only - never the subject.
                log.warn("[RATE-LIMIT] blocked policy={} count={} limit={} retryAfterSeconds={}",
                        policy, count, p.getLimit(), row[1]);
            }

            return new Decision(allowed, count, p.getLimit(), allowed ? 0 : row[1]);

        } catch (RuntimeException e) {
            log.error("[RATE-LIMIT] counter store unavailable; allowing request. policy={} reason={}",
                    policy, e.getClass().getSimpleName());
            return new Decision(true, 0, p.getLimit(), 0);
        }
    }

    /** Bounded table size: drop windows that ended more than an hour ago. */
    @Scheduled(fixedDelayString = "${garagest.rate-limit.prune-delay-ms:3600000}",
            initialDelayString = "${garagest.rate-limit.prune-initial-delay-ms:600000}")
    public void prune() {
        try {
            int removed = jdbc.update(
                    "DELETE FROM rate_limit_bucket WHERE expires_at < LOCALTIMESTAMP - INTERVAL '1 hour'");
            if (removed > 0) {
                log.info("[RATE-LIMIT] pruned expiredBuckets={}", removed);
            }
        } catch (RuntimeException e) {
            log.warn("[RATE-LIMIT] prune failed. reason={}", e.getClass().getSimpleName());
        }
    }

    /** Test helper: forget one subject. */
    public void reset(String policy, String subject) {
        jdbc.update("DELETE FROM rate_limit_bucket WHERE bucket_key = ?", policy + ":" + digest(subject));
    }

    static String digest(String subject) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(subject.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h, 0, 20); // 160 bits: collision-safe for this purpose, keeps keys short
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Lower-cases and trims an identifier; a phone-shaped one is reduced to its last ten digits so formatting cannot dodge a limit. */
    public static String normalizeIdentifier(String raw) {

        if (raw == null) {
            return "";
        }

        String t = raw.trim().toLowerCase();
        String digits = t.replaceAll("[\\s\\-().+]", "");

        if (digits.matches("\\d{10,15}")) {
            return digits.substring(digits.length() - 10);
        }

        return t;
    }
}
