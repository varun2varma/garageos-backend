package com.garageos.modules.estimateitem.repository;

import com.garageos.core.enums.catalog.ReviewStatus;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstimateItemRepository
        extends JpaRepository<EstimateItem, Long> {

    List<EstimateItem> findByEstimateId(Long estimateId);

    /**
     * Which estimate a line belongs to, without loading the line. Lets a writer take the estimate row lock BEFORE it
     * loads the line, so the line it then reads and writes is never a pre-lock (stale) copy.
     */
    @Query("select i.estimate.id from EstimateItem i where i.id = :id")
    Optional<Long> findEstimateIdById(@Param("id") Long id);

    @Query("select distinct i.estimate.id from EstimateItem i where i.complaint.id = :complaintId")
    List<Long> findEstimateIdsByComplaintId(@Param("complaintId") Long complaintId);

    List<EstimateItem> findByComplaintId(Long complaintId);

    /** Review queue (platform reviewers only): manual lines awaiting classification, oldest first. */
    Page<EstimateItem> findByReviewStatusOrderByIdAsc(ReviewStatus reviewStatus, Pageable pageable);
}
