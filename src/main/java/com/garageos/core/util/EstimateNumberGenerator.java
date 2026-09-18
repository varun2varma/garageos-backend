package com.garageos.core.util;

import java.time.Year;

public final class EstimateNumberGenerator {

    private EstimateNumberGenerator() {
    }

    public static String generate(
            String garageCode,
            String lastEstimateNumber) {

        if (garageCode == null || garageCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Garage code is required to generate estimate number.");
        }

        int currentYear = Year.now().getValue();

        if (lastEstimateNumber == null
                || lastEstimateNumber.isBlank()) {

            return String.format(
                    "%s-EST-%d-%06d",
                    garageCode,
                    currentYear,
                    1);
        }

        String[] parts = lastEstimateNumber.split("-");

        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "Invalid estimate number format: "
                            + lastEstimateNumber);
        }

        int year = Integer.parseInt(parts[2]);
        int sequence = Integer.parseInt(parts[3]);

        if (year != currentYear) {
            return String.format(
                    "%s-EST-%d-%06d",
                    garageCode,
                    currentYear,
                    1);
        }

        sequence++;

        return String.format(
                "%s-EST-%d-%06d",
                garageCode,
                currentYear,
                sequence);
    }
}