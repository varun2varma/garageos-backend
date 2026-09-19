package com.garageos.core.util;

import java.time.Year;

public final class InvoiceNumberGenerator {

    private InvoiceNumberGenerator() {
    }

    public static String generate(
            String garageCode,
            String lastInvoiceNumber
    ) {
        if (garageCode == null || garageCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Garage code is required to generate invoice number."
            );
        }

        int currentYear = Year.now().getValue();

        if (lastInvoiceNumber == null || lastInvoiceNumber.isBlank()) {
            return format(garageCode, currentYear, 1);
        }

        String[] parts = lastInvoiceNumber.split("-");

        /*
         * Legacy format:
         * INV-2026-000007
         *
         * Ignore it for the new garage-specific sequence.
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

            return format(
                    garageCode,
                    currentYear,
                    sequence + 1
            );

        } catch (NumberFormatException ex) {
            return format(garageCode, currentYear, 1);
        }
    }

    private static String format(
            String garageCode,
            int year,
            int sequence
    ) {
        return String.format(
                "%s-INV-%d-%06d",
                garageCode,
                year,
                sequence
        );
    }
}