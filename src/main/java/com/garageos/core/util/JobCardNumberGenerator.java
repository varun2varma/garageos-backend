package com.garageos.core.util;

import java.time.Year;

public final class JobCardNumberGenerator {

    private JobCardNumberGenerator() {
    }

    public static String generate(String lastJobCardNumber) {

        int currentYear = Year.now().getValue();

        if (lastJobCardNumber == null || lastJobCardNumber.isBlank()) {
            return "JC-" + currentYear + "-000001";
        }

        String[] parts = lastJobCardNumber.split("-");

        int year = Integer.parseInt(parts[1]);

        int sequence = Integer.parseInt(parts[2]);

        if (year != currentYear) {
            return "JC-" + currentYear + "-000001";
        }

        sequence++;

        return String.format(
                "JC-%d-%06d",
                currentYear,
                sequence
        );
    }
}