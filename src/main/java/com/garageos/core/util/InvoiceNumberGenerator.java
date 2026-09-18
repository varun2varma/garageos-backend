package com.garageos.core.util;

import java.time.Year;

public final class InvoiceNumberGenerator {

    private InvoiceNumberGenerator() {
    }

    public static String generate(
            String garageCode,
            String lastInvoiceNumber) {

        if (garageCode == null || garageCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Garage code is required to generate invoice number.");
        }

        int currentYear = Year.now().getValue();

        if (lastInvoiceNumber == null
                || lastInvoiceNumber.isBlank()) {

            return String.format(
                    "%s-INV-%d-%06d",
                    garageCode,
                    currentYear,
                    1);
        }

        String[] parts = lastInvoiceNumber.split("-");

        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "Invalid invoice number format: "
                            + lastInvoiceNumber);
        }

        int year = Integer.parseInt(parts[2]);
        int sequence = Integer.parseInt(parts[3]);

        if (year != currentYear) {
            return String.format(
                    "%s-INV-%d-%06d",
                    garageCode,
                    currentYear,
                    1);
        }

        sequence++;

        return String.format(
                "%s-INV-%d-%06d",
                garageCode,
                currentYear,
                sequence);
    }
}