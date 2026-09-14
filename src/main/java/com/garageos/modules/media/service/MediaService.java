package com.garageos.modules.media.service;

import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.core.enums.media.MediaStage;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {

    JobCardMedia uploadMedia(
            Long jobCardId,
            MediaStage mediaStage,
            Long repairTaskId,
            MultipartFile file
    );
}