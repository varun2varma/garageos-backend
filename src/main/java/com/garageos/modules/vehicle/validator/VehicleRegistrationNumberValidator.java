package com.garageos.modules.vehicle.validator;

import com.garageos.core.exception.BusinessException;

import java.util.regex.Pattern;

/**
 * Mission backlog #11 - Indian vehicle registration number format
 * validation, ported from the legacy static JS frontend
 * (employee/workflow/vehicleStep.js and workflow/vehicleStep.js).
 *
 * IMPORTANT PROVENANCE NOTE: in the actual legacy source, this exact
 * pattern set (state/Bharat-series/diplomatic/consular/temporary/military)
 * and its {@code statePlateRegex.test(regNo)} call are entirely commented
 * out - the live legacy behavior today is "non-empty only," no format
 * check. This is flagged explicitly per this repo's own source-of-truth
 * rules (CLAUDE.md 12/21): the patterns themselves are well-formed, real
 * Indian RC number formats, and the mission's own instruction was to
 * restore this validation, so it is ported and activated here rather than
 * left disabled - but this is a deliberate judgment call reviving
 * intentionally-disabled legacy logic, not a port of currently-active
 * behavior, and is called out as such rather than silently presented as
 * "the legacy behavior."
 */
public final class VehicleRegistrationNumberValidator {

    private VehicleRegistrationNumberValidator() {
    }

    private static final Pattern PATTERN = Pattern.compile(
            "^(?:"
                    // State-issued: e.g. AP01AB1234
                    + "[A-Z]{2}(?:0[1-9]|[1-9][0-9])[A-Z]{1,3}(?:000[1-9]|00[1-9]\\d|0[1-9]\\d{2}|[1-9]\\d{3})"
                    // Bharat Series: e.g. 21BH1234AB
                    + "|\\d{2}BH\\d{4}[A-Z]{2}"
                    // Diplomatic: e.g. CD12A1234
                    + "|CD\\d{1,3}[A-Z]\\d{1,4}"
                    // Consular: e.g. CC12345
                    + "|CC\\d{1,3}\\d{1,4}"
                    // Temporary
                    + "|TR\\d{1,6}"
                    + "|TEMP\\d{1,6}"
                    // Military: e.g. AB123456C
                    + "|[A-Z]{2}\\d{6}[A-Z]?"
                    + ")$"
    );

    /**
     * Normalizes (trim, strip spaces/hyphens, uppercase - same
     * normalization the legacy JS applied before testing) and validates.
     * Returns the normalized value; throws BusinessException if it
     * doesn't match any recognized format.
     */
    public static String normalizeAndValidate(String rawRegistrationNumber) {

        if (rawRegistrationNumber == null || rawRegistrationNumber.isBlank()) {
            throw new BusinessException("Registration number is required.");
        }

        String normalized = rawRegistrationNumber
                .trim()
                .replaceAll("[-\\s]", "")
                .toUpperCase();

        if (!PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(
                    "Please enter a valid Indian vehicle registration number.");
        }

        return normalized;
    }
}
