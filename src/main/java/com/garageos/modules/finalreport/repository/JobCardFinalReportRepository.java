package com.garageos.modules.finalreport.repository;

import com.garageos.core.enums.report.FinalReportAudience;
import com.garageos.core.enums.report.FinalReportEmailStatus;
import com.garageos.modules.finalreport.entity.JobCardFinalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobCardFinalReportRepository extends JpaRepository<JobCardFinalReport, Long> {

    Optional<JobCardFinalReport> findFirstByJobCardIdAndAudienceOrderByVersionDesc(
            Long jobCardId, FinalReportAudience audience);

    /**
     * Latest version's metadata only. With a closed interface projection Spring Data selects
     * just these columns, so listing a Job Card's reports never loads the PDF bytes.
     */
    <T> Optional<T> findFirstByJobCardIdAndAudienceOrderByVersionDesc(
            Long jobCardId, FinalReportAudience audience, Class<T> projection);

    Optional<JobCardFinalReport> findByJobCardIdAndAudienceAndVersion(
            Long jobCardId, FinalReportAudience audience, int version);

    boolean existsByJobCardIdAndAudience(Long jobCardId, FinalReportAudience audience);

    /** Reports whose e-mail is due: waiting or failed with attempts left, or a crashed claim past its lease. */
    @Query("select r.id from JobCardFinalReport r where r.emailStatus in :statuses "
            + "and r.emailAttempts < :maxAttempts and r.generatedAt > :since "
            + "and (r.emailNextAttemptAt is null or r.emailNextAttemptAt <= :now) order by r.id")
    List<Long> findDueForEmail(
            @Param("statuses") Collection<FinalReportEmailStatus> statuses,
            @Param("maxAttempts") int maxAttempts,
            @Param("since") LocalDateTime since,
            @Param("now") LocalDateTime now);

    /**
     * Atomically claims a report for sending (also takes over a lapsed claim),
     * so two senders can never both e-mail the same report. Returns 1 when claimed.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update JobCardFinalReport r set r.emailStatus = com.garageos.core.enums.report.FinalReportEmailStatus.SENDING, "
            + "r.emailNextAttemptAt = :lease where r.id = :id and r.emailStatus in :claimable "
            + "and (r.emailNextAttemptAt is null or r.emailNextAttemptAt <= :now)")
    int claimForEmail(
            @Param("id") Long id,
            @Param("claimable") Collection<FinalReportEmailStatus> claimable,
            @Param("now") LocalDateTime now,
            @Param("lease") LocalDateTime lease);

    interface ReportMeta {
        FinalReportAudience getAudience();

        Integer getVersion();

        String getFileName();

        Long getFileSize();

        LocalDateTime getGeneratedAt();

        FinalReportEmailStatus getEmailStatus();
    }
}
