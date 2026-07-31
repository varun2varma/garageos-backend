package com.garageos.core.loader;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractImporter<T> {

    protected final CsvLoader csvLoader;

    protected final EntityManager entityManager;

    protected final ImportStatistics statistics;

    private final BatchWriter<T> writer;

    protected AbstractImporter(
            CsvLoader csvLoader,
            EntityManager entityManager,
            JpaRepository<T, ?> repository,
            String importName) {

        this.csvLoader = csvLoader;
        this.entityManager = entityManager;
        this.statistics = new ImportStatistics(importName);
        this.writer = new BatchWriter<>(
                repository,
                entityManager);

    }

    protected void write(T entity) {

        writer.add(entity);

        statistics.inserted();

    }

    protected void skip() {

        statistics.skipped();

    }

    protected void fail() {

        statistics.failed();

    }

    protected void rowRead() {

        statistics.rowRead();

    }

    protected void finish() {

        writer.close();

        statistics.printSummary();

    }

}