package com.garageos.core.util;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The single place where stored instants become user-facing text.
 *
 * Policy: timestamps are persisted/exchanged as UTC instants and only formatted for display, in the timezone of
 * the place the event happened. Resolution order for a display zone: (1) a verified event-location timezone,
 * (2) the garage's configured IANA timezone, (3) {@link #DEFAULT_ZONE}. The device timezone is never used to guess
 * a location. Legacy columns are java.time.LocalDateTime values written with the JVM's default zone; they are
 * converted from that zone, so an instant is never shifted by formatting.
 */
public final class DisplayTime {

    /** Documented fallback when no event/garage timezone is known: GarageST workshops operate in India. */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm z", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME_SHORT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.ENGLISH);

    private DisplayTime() {
    }

    /** First resolvable IANA id wins; blank or unknown ids are skipped; falls back to {@link #DEFAULT_ZONE}. */
    public static ZoneId resolveZone(String... candidateIanaIds) {

        if (candidateIanaIds != null) {
            for (String id : candidateIanaIds) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                try {
                    return ZoneId.of(id.trim());
                } catch (DateTimeException ignored) {
                    // unknown zone id: try the next candidate
                }
            }
        }
        return DEFAULT_ZONE;
    }

    /** Converts a stored (JVM-default-zone) local timestamp to the display zone. */
    public static ZonedDateTime toZone(LocalDateTime stored, ZoneId storedZone, ZoneId display) {
        return stored.atZone(storedZone).withZoneSameInstant(display);
    }

    /** "13-10-2026 15:30 IST" - with the zone abbreviation, so audit times are unambiguous. */
    public static String format(LocalDateTime stored, ZoneId storedZone, ZoneId display) {
        return stored == null ? "-" : DATE_TIME.format(toZone(stored, storedZone, display));
    }

    /** Same without the zone suffix, for dense tables where the zone is stated once in the header. */
    public static String formatShort(LocalDateTime stored, ZoneId storedZone, ZoneId display) {
        return stored == null ? "-" : DATE_TIME_SHORT.format(toZone(stored, storedZone, display));
    }
}
