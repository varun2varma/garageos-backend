package com.garageos.modules.navigation.storage;

import com.garageos.core.enums.navigation.TripMediaStage;
import com.garageos.modules.media.util.MediaKeys;

import java.util.regex.Pattern;

/**
 * Object-key layout for pickup and delivery evidence in the garage / job-card hierarchy:
 * <pre>
 * GarageST/&lt;garageCode&gt;/&lt;jobCardNumber&gt;/DURING_PICKUP/&lt;file&gt;        (job card exists)
 * GarageST/&lt;garageCode&gt;/&lt;jobCardNumber&gt;/DURING_DELIVERY/&lt;file&gt;
 * GarageST/&lt;garageCode&gt;/TRIP-&lt;tripId&gt;/DURING_PICKUP/&lt;file&gt;          (provisional: no job card yet)
 * </pre>
 * The persisted/API stage values stay {@code BEFORE_PICKUP} and {@code DELIVERY}; they map to the physical folder
 * names only here. The garage code and job-card number must come from server-side records; values that are not plain
 * identifiers are rejected (empty result) so a caller can never inject path segments.
 */
public final class TripEvidenceKeys {

    public static final String ROOT = "GarageST/";

    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private TripEvidenceKeys() {
    }

    /** Physical folder name for a stage, e.g. BEFORE_PICKUP -> DURING_PICKUP. */
    public static String physicalStage(TripMediaStage stage) {
        return switch (stage) {
            case BEFORE_PICKUP -> "DURING_PICKUP";
            case DELIVERY -> "DURING_DELIVERY";
        };
    }

    /** Folder owner segment used before the job card exists. */
    public static String provisionalOwner(Long tripId) {
        return "TRIP-" + tripId;
    }

    /**
     * Folder (no trailing slash) for evidence of {@code stage} owned by {@code owner} (a job-card number or a
     * provisional owner), or null when an input is not a safe identifier.
     */
    public static String folder(String garageCode, String owner, TripMediaStage stage) {
        if (!isSafe(garageCode) || !isSafe(owner) || stage == null) {
            return null;
        }
        return ROOT + garageCode + "/" + owner + "/" + physicalStage(stage);
    }

    /**
     * The canonical evidence key, built with the existing job-card conventions:
     * {@code GarageST/<garageCode>/<jobCardNumber>/<STAGE>/<jobCardNumber>_<STAGE>_<seq 3 digits>_evidence.jpg}
     * ({@code MediaKeys.derive} adds the role suffix). Null for an unsafe segment or a sequence below 1.
     */
    public static String canonicalEvidenceKey(String garageCode, String jobCardNumber, TripMediaStage stage, int sequence) {
        String folder = folder(garageCode, jobCardNumber, stage);
        if (folder == null || sequence < 1) {
            return null;
        }
        String base = String.format("%s_%s_%03d.jpg", jobCardNumber, physicalStage(stage), sequence);
        return MediaKeys.derive(folder + "/" + base, MediaKeys.EVIDENCE, "jpg");
    }

    /** The thumbnail key that goes with an evidence key (same derivation as job-card media). */
    public static String thumbnailKeyOf(String evidenceKey) {
        return MediaKeys.derive(evidenceKey, MediaKeys.THUMBNAIL, "jpg");
    }

    /** True for a key that follows the canonical evidence naming ({@code ..._evidence.jpg}). */
    public static boolean isCanonicalEvidenceKey(String key) {
        return key != null && key.startsWith(ROOT) && key.endsWith("_" + MediaKeys.EVIDENCE + ".jpg");
    }

    public static boolean isInFolder(String key, String folder) {
        return key != null && folder != null && key.startsWith(folder + "/");
    }

    /** True for a key under the provisional {@code TRIP-<id>} owner of this garage. */
    public static boolean isProvisional(String key, String garageCode) {
        return key != null && isSafe(garageCode) && key.startsWith(ROOT + garageCode + "/TRIP-");
    }

    public static String fileName(String key) {
        int slash = key.lastIndexOf('/');
        return slash < 0 ? key : key.substring(slash + 1);
    }

    private static boolean isSafe(String segment) {
        return segment != null && SAFE_SEGMENT.matcher(segment).matches() && !segment.equals(".") && !segment.equals("..");
    }
}
