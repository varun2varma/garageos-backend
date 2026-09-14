package com.garageos.core.util;

import java.time.Year;

public final class JobCardNumberGenerator {

    private JobCardNumberGenerator() {
    }

    public static String generate(
            String garageCode,
            String lastJobCardNumber) {

        if (garageCode == null || garageCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Garage code is required to generate job card number."
            );
        }

        int currentYear = Year.now().getValue();

        if (lastJobCardNumber == null || lastJobCardNumber.isBlank()) {
            return String.format(
                    "%s-JC-%d-%06d",
                    garageCode,
                    currentYear,
                    1
            );
        }

        String[] parts = lastJobCardNumber.split("-");

        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "Invalid job card number format: "
                            + lastJobCardNumber
            );
        }

        int year = Integer.parseInt(parts[2]);
        int sequence = Integer.parseInt(parts[3]);

        if (year != currentYear) {
            return String.format(
                    "%s-JC-%d-%06d",
                    garageCode,
                    currentYear,
                    1
            );
        }

        sequence++;

        return String.format(
                "%s-JC-%d-%06d",
                garageCode,
                currentYear,
                sequence
        );
    }
}