package com.garageos.modules.media.service.impl;

import com.garageos.core.enums.media.MediaStage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concurrency-safe media sequence numbers. One atomic upsert per (job card, stage) hands out
 * strictly increasing numbers, so two concurrent uploads can never receive the same sequence (and
 * therefore never the same object-storage key), numbers are never reused after a deletion, and a
 * retry simply takes the next number. The counter row is locked by the upsert itself until the
 * surrounding transaction ends.
 *
 * Backed by {@code job_card_media_counter} (migration V80), seeded above every pre-existing sequence.
 */
@Component
public class MediaSequenceAllocator {

    static final String ALLOCATE_SQL =
            "INSERT INTO job_card_media_counter (job_card_id, media_stage, last_seq) VALUES (:jobCardId, :stage, 1) "
                    + "ON CONFLICT (job_card_id, media_stage) "
                    + "DO UPDATE SET last_seq = job_card_media_counter.last_seq + 1 "
                    + "RETURNING last_seq";

    @PersistenceContext
    private EntityManager entityManager;

    MediaSequenceAllocator() {
    }

    /** For unit tests. */
    public MediaSequenceAllocator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public int next(Long jobCardId, MediaStage stage) {

        Object value = entityManager.createNativeQuery(ALLOCATE_SQL)
                .setParameter("jobCardId", jobCardId)
                .setParameter("stage", stage.name())
                .getSingleResult();

        return ((Number) value).intValue();
    }
}
