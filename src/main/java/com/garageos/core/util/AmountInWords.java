package com.garageos.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Indian-system (lakh/crore) amount in words for invoices, e.g. "Rupees Two Thousand Only". */
public final class AmountInWords {

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private AmountInWords() {
    }

    public static String rupees(BigDecimal amount) {

        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP).abs();

        long rupees = value.longValue();
        int paise = value.remainder(BigDecimal.ONE).movePointRight(2).intValue();

        StringBuilder out = new StringBuilder("Rupees ");

        out.append(rupees == 0 ? "Zero" : words(rupees));

        if (paise > 0) {
            out.append(" and ").append(words(paise)).append(" Paise");
        }

        return out.append(" Only").toString();
    }

    private static String words(long n) {

        StringBuilder sb = new StringBuilder();

        long crore = n / 10_000_000L;
        n %= 10_000_000L;
        long lakh = n / 100_000L;
        n %= 100_000L;
        long thousand = n / 1_000L;
        n %= 1_000L;
        long hundred = n / 100L;
        long rest = n % 100L;

        append(sb, crore, "Crore");
        append(sb, lakh, "Lakh");
        append(sb, thousand, "Thousand");
        append(sb, hundred, "Hundred");

        if (rest > 0) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(twoDigits((int) rest));
        }

        return sb.toString();
    }

    private static void append(StringBuilder sb, long count, String unit) {

        if (count == 0) {
            return;
        }

        if (sb.length() > 0) {
            sb.append(' ');
        }

        sb.append(count >= 100 ? words(count) : twoDigits((int) count)).append(' ').append(unit);
    }

    private static String twoDigits(int n) {

        if (n < 20) {
            return ONES[n];
        }

        return TENS[n / 10] + (n % 10 == 0 ? "" : " " + ONES[n % 10]);
    }
}
