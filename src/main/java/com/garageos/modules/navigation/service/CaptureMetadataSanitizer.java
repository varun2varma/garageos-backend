package com.garageos.modules.navigation.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Validates and normalises the capture metadata a driver's device sends with a pickup/delivery photo or video.
 *
 * Rules (nothing is ever invented):
 * - capture time: a valid device timestamp is kept (an offline photo keeps its real capture time); a missing,
 *   unparsable, future (beyond clock-skew tolerance) or implausibly old timestamp falls back to server receipt
 *   time and is marked SERVER so the audit trail says which it is. A timestamp with no offset is read as UTC.
 * - coordinates: kept only as a valid pair; otherwise both are dropped. Accuracy needs valid coordinates.
 * - address: trimmed free text, control characters removed, bounded length; blank means unavailable.
 */
public final class CaptureMetadataSanitizer {

    public static final String SOURCE_DEVICE = "DEVICE";
    public static final String SOURCE_SERVER = "SERVER";

    static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);
    static final Duration MAX_AGE = Duration.ofDays(30);
    static final double MAX_ACCURACY_METRES = 100_000d;
    static final int MAX_ADDRESS = 500;

    /** Raw values as received from the client; all optional. */
    public record Raw(String capturedAt, Double latitude, Double longitude, Double accuracyMeters, String locationAddress) {
        public static Raw none() {
            return new Raw(null, null, null, null, null);
        }
    }

    public record Result(
            LocalDateTime capturedAt,
            String capturedAtSource,
            Double latitude,
            Double longitude,
            Double accuracyMeters,
            String locationAddress) {
    }

    private CaptureMetadataSanitizer() {
    }

    public static Result sanitize(Raw raw, Instant now, ZoneId storageZone) {

        Raw in = raw == null ? Raw.none() : raw;

        Instant parsed = parseInstant(in.capturedAt());
        boolean usable = parsed != null
                && !parsed.isAfter(now.plus(FUTURE_TOLERANCE))
                && !parsed.isBefore(now.minus(MAX_AGE));

        Instant effective = usable ? parsed : now;

        boolean validCoordinates = in.latitude() != null && in.longitude() != null
                && Double.isFinite(in.latitude()) && Double.isFinite(in.longitude())
                && in.latitude() >= -90 && in.latitude() <= 90
                && in.longitude() >= -180 && in.longitude() <= 180;

        Double accuracy = validCoordinates
                && in.accuracyMeters() != null
                && Double.isFinite(in.accuracyMeters())
                && in.accuracyMeters() >= 0
                && in.accuracyMeters() <= MAX_ACCURACY_METRES
                ? in.accuracyMeters() : null;

        return new Result(
                LocalDateTime.ofInstant(effective, storageZone),
                usable ? SOURCE_DEVICE : SOURCE_SERVER,
                validCoordinates ? in.latitude() : null,
                validCoordinates ? in.longitude() : null,
                accuracy,
                cleanAddress(in.locationAddress()));
    }

    static Instant parseInstant(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String v = value.trim();

        try {
            return OffsetDateTime.parse(v).toInstant();
        } catch (DateTimeParseException ignored) {
            // fall through: no offset means the canonical UTC instant
        }

        try {
            return LocalDateTime.parse(v).atZone(ZoneId.of("UTC")).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    static String cleanAddress(String address) {

        if (address == null) {
            return null;
        }

        String cleaned = address.replaceAll("\\p{Cntrl}", " ").trim();

        if (cleaned.isEmpty()) {
            return null;
        }

        return cleaned.length() > MAX_ADDRESS ? cleaned.substring(0, MAX_ADDRESS) : cleaned;
    }
}
