package com.garageos.modules.media.util;

import com.garageos.modules.media.entity.JobCardMedia;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Single source of truth for the text that is burned into evidence media.
 * Used by the server-side image renderer and returned to the client in the
 * upload intent so the on-device video burn-in shows identical wording.
 * Built only from the metadata persisted at capture time; a field that was
 * not captured (e.g. GPS unavailable) is omitted, never fabricated.
 */
public final class MediaEvidenceText {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private MediaEvidenceText() {
    }

    public static List<String> lines(JobCardMedia media) {

        List<String> lines = new ArrayList<>();

        if (media.getMediaStage() != null) {
            lines.add(stageLabel(media.getMediaStage()));
        }

        if (media.getCapturedAt() != null) {
            lines.add("Captured: " + TIMESTAMP_FORMAT.format(media.getCapturedAt()));
        }

        if (media.getCapturedByNameSnapshot() != null) {
            lines.add("By: " + media.getCapturedByNameSnapshot());
        }

        if (media.getLocationName() != null && !media.getLocationName().isBlank()) {
            lines.add("Location: " + media.getLocationName());
        }

        if (media.getLatitude() != null && media.getLongitude() != null) {
            lines.add(String.format(Locale.ENGLISH, "Lat/Lng: %.6f, %.6f",
                    media.getLatitude(), media.getLongitude()));
        }

        if (media.getLocationAccuracyMeters() != null) {
            lines.add(String.format(Locale.ENGLISH, "Accuracy: +/- %.0f m",
                    media.getLocationAccuracyMeters()));
        }

        return lines;
    }

    public static String stageLabel(String stage) {
        return switch (stage) {
            case "BEFORE_SERVICE" -> "Before Service";
            case "DURING_REPAIR" -> "During Repair";
            case "AFTER_REPAIR" -> "After Repair";
            default -> stage;
        };
    }
}
