package com.garageos.core.loader;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ImportStatistics {

    /**
     * Print progress every N rows.
     */
    private static final long LOG_INTERVAL = 100_000;

    private final String importName;

    private final long startTime;

    private long rowsRead;

    private long inserted;

    private long skipped;

    private long failed;

    public ImportStatistics(String importName) {

        this.importName = importName;
        this.startTime = System.currentTimeMillis();

    }

    public void rowRead() {

        rowsRead++;

        if (rowsRead % LOG_INTERVAL == 0) {

            logProgress();

        }

    }

    public void inserted() {

        inserted++;

    }

    public void skipped() {

        skipped++;

    }

    public void failed() {

        failed++;

    }

    private void logProgress() {

        long elapsed = System.currentTimeMillis() - startTime;

        double seconds = elapsed / 1000.0;

        long rowsPerSecond =
                seconds == 0
                        ? 0
                        : Math.round(rowsRead / seconds);

        log.info(
                "{} : {} rows processed ({} rows/sec)",
                importName,
                format(rowsRead),
                format(rowsPerSecond)
        );

    }

    public void printSummary() {

        long elapsed = System.currentTimeMillis() - startTime;

        double seconds = elapsed / 1000.0;

        long rowsPerSecond =
                seconds == 0
                        ? 0
                        : Math.round(rowsRead / seconds);

        log.info("");
        log.info("=================================================");
        log.info("{} Import Completed", importName);
        log.info("=================================================");
        log.info("Rows Read      : {}", format(rowsRead));
        log.info("Inserted       : {}", format(inserted));
        log.info("Skipped        : {}", format(skipped));
        log.info("Failed         : {}", format(failed));
        log.info("Elapsed Time   : {}", formatDuration(elapsed));
        log.info("Rows / Second  : {}", format(rowsPerSecond));
        log.info("=================================================");
        log.info("");

    }

    private String format(long number) {

        return String.format("%,d", number);

    }

    private String formatDuration(long millis) {

        long seconds = millis / 1000;

        long hours = seconds / 3600;

        long minutes = (seconds % 3600) / 60;

        long remainingSeconds = seconds % 60;

        return String.format(
                "%02d:%02d:%02d",
                hours,
                minutes,
                remainingSeconds
        );

    }

}