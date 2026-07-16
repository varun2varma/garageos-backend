package com.garageos.core.util;

import java.time.Year;

public final class EstimateNumberGenerator {

    private EstimateNumberGenerator() {
    }

    public static String generate(String lastEstimateNumber) {

        int currentYear = Year.now().getValue();

        if (lastEstimateNumber == null || lastEstimateNumber.isBlank()) {
            return "EST-" + currentYear + "-000001";
        }

        String[] parts = lastEstimateNumber.split("-");

        int year = Integer.parseInt(parts[1]);

        int sequence = Integer.parseInt(parts[2]);

        if (year != currentYear) {
            return "EST-" + currentYear + "-000001";
        }

        sequence++;

        return String.format(
                "EST-%d-%06d",
                currentYear,
                sequence
        );
    }
}