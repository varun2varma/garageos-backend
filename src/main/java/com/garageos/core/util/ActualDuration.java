package com.garageos.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Actual work duration of a repair task, derived only from the two timestamps the
 * server records itself (task start and task completion). It never reads
 * technician-entered hours or the estimate, and it never invents a value: a task
 * that was not started, is not completed, or has inconsistent timestamps has no
 * actual duration (null).
 *
 * This is elapsed wall-clock time between start and completion. The domain has no
 * pause/resume, so there is no separate "active time".
 */
public final class ActualDuration {

    private ActualDuration() {
    }

    /** Elapsed time between start and completion, or null when it cannot be established. */
    public static Duration between(LocalDateTime startedAt, LocalDateTime completedAt) {

        if (startedAt == null || completedAt == null || completedAt.isBefore(startedAt)) {
            return null;
        }

        return Duration.between(startedAt, completedAt);
    }

    /**
     * Human text for an {@link #hours} value: {@code 35 min}, {@code 1 hr 5 min}, {@code 3 hr}; {@code -} when there
     * is no duration. Mirrors the app's DurationFormatter so every surface (app, PDF) reads the same.
     */
    public static String format(Double hours) {

        if (hours == null || hours.isNaN() || hours.isInfinite() || hours < 0) {
            return "-";
        }

        long minutes = Math.round(hours * 60);

        if (minutes == 0) {
            return "< 1 min";
        }

        long h = minutes / 60;
        long m = minutes % 60;

        if (h == 0) {
            return m + " min";
        }

        return m == 0 ? h + " hr" : h + " hr " + m + " min";
    }

    /** Elapsed hours rounded to 2 decimals (HALF_UP), or null when it cannot be established. */
    public static Double hours(LocalDateTime startedAt, LocalDateTime completedAt) {

        Duration d = between(startedAt, completedAt);

        if (d == null) {
            return null;
        }

        return BigDecimal.valueOf(d.toMillis())
                .divide(BigDecimal.valueOf(3_600_000L), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
