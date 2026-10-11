package com.garageos.modules.catalog.service;

import com.garageos.core.exception.BusinessException;

import java.util.Locale;
import java.util.Set;

/** The units of measure a catalog item may default to. Garages still choose their own units on stock. */
public final class CatalogUnits {

    public static final Set<String> ALLOWED = Set.of(
            "NOS", "SET", "PAIR", "LTR", "ML", "KG", "GM", "MTR", "KIT", "BOX", "PKT", "ROLL", "JOB", "HR");

    private CatalogUnits() {
    }

    /** Upper-cases and validates; throws a user-presentable error for anything else. */
    public static String normalize(String raw) {

        String unit = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED.contains(unit)) {
            throw new BusinessException("Unit must be one of: " + String.join(", ", new java.util.TreeSet<>(ALLOWED)) + ".");
        }

        return unit;
    }
}
