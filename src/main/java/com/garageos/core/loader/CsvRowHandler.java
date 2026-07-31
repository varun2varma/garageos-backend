package com.garageos.core.loader;

@FunctionalInterface
public interface CsvRowHandler {

    void handle(CsvRow row);

}