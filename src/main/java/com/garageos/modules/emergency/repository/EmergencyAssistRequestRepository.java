package com.garageos.modules.emergency.repository;

import com.garageos.core.enums.emergency.EmergencyRequestStatus;
import com.garageos.modules.emergency.entity.EmergencyAssistRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmergencyAssistRequestRepository extends JpaRepository<EmergencyAssistRequest, Long> {

    Optional<EmergencyAssistRequest> findByRequestCode(String requestCode);

    Optional<EmergencyAssistRequest> findByContactMobileAndIdempotencyKey(String contactMobile, String idempotencyKey);

    Optional<EmergencyAssistRequest> findFirstByContactMobileAndStatusIn(
            String contactMobile, Collection<EmergencyRequestStatus> statuses);

    List<EmergencyAssistRequest> findByGarageIdOrderByCreatedAtDesc(Long garageId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from EmergencyAssistRequest r where r.id = :id")
    Optional<EmergencyAssistRequest> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from EmergencyAssistRequest r where r.requestCode = :code")
    Optional<EmergencyAssistRequest> findByRequestCodeForUpdate(@Param("code") String code);

    @Modifying(clearAutomatically = true)
    @Query("update EmergencyAssistRequest r set r.status = com.garageos.core.enums.emergency.EmergencyRequestStatus.EXPIRED "
            + "where r.status = com.garageos.core.enums.emergency.EmergencyRequestStatus.REQUESTED and r.expiresAt < :now")
    int expireRequested(@Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("update EmergencyAssistRequest r set r.status = com.garageos.core.enums.emergency.EmergencyRequestStatus.EXPIRED "
            + "where r.status = com.garageos.core.enums.emergency.EmergencyRequestStatus.ACCEPTED and r.decidedAt < :before")
    int expireStaleAccepted(@Param("before") LocalDateTime before);
}
