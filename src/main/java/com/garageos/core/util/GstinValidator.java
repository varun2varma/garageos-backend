package com.garageos.core.util;

import java.util.regex.Pattern;

/**
 * Structural validation of an Indian GSTIN. This proves a number is WELL FORMED; it does NOT prove the GSTIN is
 * registered, active or belongs to this business. That needs the GST portal or a licensed verification provider, and
 * until one is integrated a GSTIN must be presented as "format valid", never as "verified".
 *
 * <p>Layout (15 characters): 2-digit state code, 10-character PAN, 1 entity number (1-9 or A-Z), the letter Z, and a
 * check character computed with the published modulus-36 algorithm.
 */
public final class GstinValidator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Pattern SHAPE = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

    private GstinValidator() {
    }

    /** Trimmed, upper-cased, internal spaces and dashes removed; null for null/blank. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.replaceAll("[\\s-]", "").toUpperCase();
        return v.isEmpty() ? null : v;
    }

    /** True when the (already normalised) value has a valid shape, state code and check character. */
    public static boolean isStructurallyValid(String gstin) {

        if (gstin == null || !SHAPE.matcher(gstin).matches()) {
            return false;
        }

        int state = Integer.parseInt(gstin.substring(0, 2));

        // 01-38 are states/UTs; 97 other territory, 99 centre jurisdiction.
        if (!((state >= 1 && state <= 38) || state == 97 || state == 99)) {
            return false;
        }

        return checkCharacter(gstin.substring(0, 14)) == gstin.charAt(14);
    }

    /** The PAN embedded in a GSTIN (characters 3 to 12), or null when the value is too short. */
    public static String embeddedPan(String gstin) {
        return gstin == null || gstin.length() < 12 ? null : gstin.substring(2, 12);
    }

    /** The check character for the first 14 characters (modulus-36 weighted sum). */
    static char checkCharacter(String first14) {

        int sum = 0;

        for (int i = 0; i < first14.length(); i++) {
            int value = ALPHABET.indexOf(first14.charAt(i));
            int product = value * ((i % 2 == 0) ? 1 : 2);
            sum += product / 36 + product % 36;
        }

        return ALPHABET.charAt((36 - (sum % 36)) % 36);
    }

    /** Last four characters only, for display where the full number is not needed (for example an admin list). */
    public static String mask(String gstin) {
        return gstin == null || gstin.length() < 5 ? null : "XXXXXXXXXXX" + gstin.substring(gstin.length() - 4);
    }
}
