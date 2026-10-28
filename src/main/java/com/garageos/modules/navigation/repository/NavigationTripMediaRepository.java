package com.garageos.modules.navigation.repository;

import com.garageos.modules.navigation.entity.NavigationTripMedia;
import com.garageos.core.enums.navigation.TripMediaStage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NavigationTripMediaRepository
        extends JpaRepository<NavigationTripMedia, Long> {

    List<NavigationTripMedia>
    findByTripIdOrderByCapturedAtAsc(
            Long tripId
    );

    List<NavigationTripMedia>
    findByTripIdAndMediaStageOrderByCapturedAtAsc(
            Long tripId,
            TripMediaStage mediaStage
    );

    Optional<NavigationTripMedia> findByTripIdAndClientUploadId(Long tripId, String clientUploadId);

    /**
     * The row's CURRENT stored key as a scalar read: unlike {@code findById} inside a running transaction it is not
     * answered from the persistence context, so it sees a key switched by another transaction.
     */
    @Query("select m.storageKey from NavigationTripMedia m where m.id = :id")
    Optional<String> findStorageKeyById(@Param("id") Long id);

    /**
     * How many photos of this job card and stage (across all trips linked to it) have a lower id than {@code id}.
     * The canonical sequence of a photo is this count + 1: derived only from persisted rows, stable because ids only grow.
     */
    @Query("select count(m) from NavigationTripMedia m, NavigationTrip t, NavigationRequest r "
            + "where m.tripId = t.id and t.navigationRequestId = r.id and r.jobCardId = :jobCardId "
            + "and m.mediaStage = :stage and m.id < :id")
    long countStageRowsBefore(@Param("jobCardId") Long jobCardId,
                              @Param("stage") com.garageos.core.enums.navigation.TripMediaStage stage,
                              @Param("id") Long id);

    /**
     * Transaction-scoped advisory lock (PostgreSQL) that serialises uploads of one job card and stage, so two concurrent
     * uploads cannot both count the other as not yet existing and pick the same sequence. Released at commit/rollback.
     */
    @Query(value = "select cast(pg_advisory_xact_lock(:k1, :k2) as text)", nativeQuery = true)
    String lockStageSequence(@Param("k1") int k1, @Param("k2") int k2);

    /** How many rows reference this object key (used before an old evidence object is deleted). */
    long countByStorageKey(String storageKey);

    /**
     * Compare-and-set: repoints the row only if it still holds {@code oldKey}. Returns 1 for the one caller that
     * switched it, 0 for any other (a concurrent or repeated run), so only the winner may delete the old object.
     */
    @Transactional
    @Modifying
    @Query("update NavigationTripMedia m set m.storageKey = :newKey where m.id = :id and m.storageKey = :oldKey")
    int updateStorageKeyIfUnchanged(@Param("id") Long id, @Param("oldKey") String oldKey, @Param("newKey") String newKey);

    /**
     * Same compare-and-set, also recording what the new object is (canonical evidence is always a JPEG of a known size),
     * so the key, content type and size always change together or not at all.
     */
    @Transactional
    @Modifying
    @Query("update NavigationTripMedia m set m.storageKey = :newKey, m.contentType = :contentType, m.fileSize = :fileSize "
            + "where m.id = :id and m.storageKey = :oldKey")
    int updateEvidenceIfUnchanged(@Param("id") Long id, @Param("oldKey") String oldKey, @Param("newKey") String newKey,
                                  @Param("contentType") String contentType, @Param("fileSize") Long fileSize);

    /**
     * Job cards (ids after {@code afterJobCardId}, ascending) that have trip evidence still under a legacy
     * {@code trip-evidence/} key or a provisional {@code TRIP-<id>} folder although the request is linked to a job card.
     */
    @Query("select r.jobCardId from NavigationTripMedia m, NavigationTrip t, NavigationRequest r "
            + "where m.tripId = t.id and t.navigationRequestId = r.id and r.jobCardId is not null "
            + "and r.jobCardId > :afterJobCardId "
            + "and (m.storageKey like 'trip-evidence/%' or m.storageKey like 'GarageST/%/TRIP-%') "
            + "group by r.jobCardId order by r.jobCardId")
    List<Long> findJobCardIdsWithUnadoptedEvidence(@Param("afterJobCardId") Long afterJobCardId, Pageable pageable);
}
