package com.garageos.modules.garage.util;

import com.garageos.core.exception.BusinessException;

import java.util.regex.Pattern;

/** Validation and normalisation of garage brand colours. */
public final class BrandColors {

    private static final Pattern HEX6 = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private BrandColors() {
    }

    /**
     * Upper-case {@code #RRGGBB} for a valid colour; null for null/blank (meaning "use the default theme").
     *
     * @throws BusinessException for anything else, including 3-digit shorthand, names, alpha channels and CSS
     */
    public static String normalize(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();

        if (!HEX6.matcher(value).matches()) {
            throw new BusinessException("Colours must be written as #RRGGBB, for example #1D4ED8.");
        }

        return value.toUpperCase();
    }
}
