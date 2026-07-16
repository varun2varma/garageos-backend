package com.garageos.core.util;

import java.time.Year;

public final class InvoiceNumberGenerator {

    private InvoiceNumberGenerator() {
    }

    public static String generate(String lastInvoiceNumber) {

        int currentYear = Year.now().getValue();

        if (lastInvoiceNumber == null || lastInvoiceNumber.isBlank()) {
            return "INV-" + currentYear + "-000001";
        }

        String[] parts = lastInvoiceNumber.split("-");

        int year = Integer.parseInt(parts[1]);
        int sequence = Integer.parseInt(parts[2]);

        if (year != currentYear) {
            return "INV-" + currentYear + "-000001";
        }

        sequence++;

        return String.format(
                "INV-%d-%06d",
                currentYear,
                sequence
        );
    }
}