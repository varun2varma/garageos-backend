package com.garageos.core.loader;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents one CSV record.
 *
 * Provides type-safe access using column names instead of indexes.
 *
 * Example:
 *
 * row.string("make");
 * row.integer("service_km");
 * row.bool("mandatory");
 */
public final class CsvRow {

    private final String[] values;

    private final Map<String, Integer> headers;

    CsvRow(String[] values,
           Map<String, Integer> headers) {

        this.values = values;
        this.headers = headers;
    }

    public String string(String column) {

        Integer index = headers.get(column);

        if (index == null) {
            throw new IllegalArgumentException(
                    "Unknown CSV column : " + column);
        }

        if (index >= values.length) {
            return "";
        }

        return values[index].trim();
    }

    public Integer integer(String column) {

        String value = string(column);

        if (value.isEmpty()) {
            return null;
        }

        return Integer.parseInt(value);
    }

    public Long longValue(String column) {

        String value = string(column);

        if (value.isEmpty()) {
            return null;
        }

        return Long.parseLong(value);
    }

    public Double doubleValue(String column) {

        String value = string(column);

        if (value.isEmpty()) {
            return null;
        }

        return Double.parseDouble(value);
    }

    public BigDecimal decimal(String column) {

        String value = string(column);

        if (value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid decimal value [" + value + "] in column [" + column + "]",
                    e);
        }
    }

    public Boolean bool(String column) {

        String value = string(column);

        if (value.isEmpty()) {
            return null;
        }

        return Boolean.parseBoolean(value);
    }

    public <E extends Enum<E>> E enumValue(
            Class<E> enumClass,
            String column) {

        String value = string(column);

        if (value.isEmpty()) {
            return null;
        }

        return Enum.valueOf(enumClass, value);
    }

    public boolean hasValue(String column) {

        return !string(column).isBlank();
    }

}