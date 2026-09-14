package com.garageos.modules.media.repository;

import com.garageos.modules.media.entity.JobCardMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobCardMediaRepository
        extends JpaRepository<JobCardMedia, Long> {

    List<JobCardMedia> findByJobCardIdOrderByCreatedAtAsc(
            Long jobCardId
    );

    List<JobCardMedia> findByJobCardIdAndMediaStageOrderByCreatedAtAsc(
            Long jobCardId,
            String mediaStage
    );

    List<JobCardMedia> findByRepairTaskIdOrderByCreatedAtAsc(
            Long repairTaskId
    );

    List<JobCardMedia> findByJobCardIdAndVisibilityOrderByCreatedAtAsc(
            Long jobCardId,
            String visibility
    );
}