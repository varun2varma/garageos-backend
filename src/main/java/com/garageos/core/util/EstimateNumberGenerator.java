package com.garageos.core.util;

import java.time.Year;

public final class EstimateNumberGenerator {

    private EstimateNumberGenerator() {
    }

    public static String generate(String garageCode, String lastEstimateNumber) {
        if (garageCode == null || garageCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Garage code is required to generate estimate number."
            );
        }

        int currentYear = Year.now().getValue();

        /*
         * No previous estimate for this garage.
         */
        if (lastEstimateNumber == null || lastEstimateNumber.isBlank()) {
            return format(garageCode, currentYear, 1);
        }

        String[] parts = lastEstimateNumber.split("-");

        /*
         * Legacy format:
         * EST-2026-000007
         *
         * Do not try to increment this number.
         * It belongs to the old global numbering scheme.
         *
         * Start the garage-specific sequence independently.
         */
        if (parts.length != 4) {
            return format(garageCode, currentYear, 1);
        }

        try {
            int year = Integer.parseInt(parts[2]);
            int sequence = Integer.parseInt(parts[3]);

            if (year != currentYear) {
                return format(garageCode, currentYear, 1);
            }

            return format(garageCode, currentYear, sequence + 1);

        } catch (NumberFormatException ex) {
            /*
             * Any malformed/legacy value should not break
             * the garage-specific numbering flow.
             */
            return format(garageCode, currentYear, 1);
        }
    }

    private static String format(
            String garageCode,
            int year,
            int sequence
    ) {
        return String.format(
                "%s-EST-%d-%06d",
                garageCode,
                year,
                sequence
        );
    }
}