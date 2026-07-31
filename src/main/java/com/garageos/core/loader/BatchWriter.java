package com.garageos.core.loader;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic batch writer for JPA entities.
 *
 * Features:
 * - Generic
 * - Memory efficient
 * - Automatic flush
 * - Automatic clear
 * - Reusable for every importer
 */
@RequiredArgsConstructor
public class BatchWriter<T> {

    /**
     * Default batch size.
     *
     * Recommended:
     * 500 - Small entities
     * 1000 - Most imports
     * 2000 - Very small entities
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final JpaRepository<T, ?> repository;

    private final EntityManager entityManager;

    private final List<T> batch = new ArrayList<>(DEFAULT_BATCH_SIZE);

    public void add(T entity) {

        batch.add(entity);

        if (batch.size() >= DEFAULT_BATCH_SIZE) {
            flush();
        }

    }

    @Transactional
    public void flush() {

        if (batch.isEmpty()) {
            return;
        }

        long start = System.currentTimeMillis();

        repository.saveAllAndFlush(batch);

        entityManager.clear();

        batch.clear();

        System.out.println(
                "Flushed batch in "
                        + (System.currentTimeMillis() - start)
                        + " ms");
    }

    public void close() {

        flush();

    }

}