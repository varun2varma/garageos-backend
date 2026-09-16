package com.garageos.modules.jobcard.service;

import com.garageos.modules.jobcard.dto.response.JobCardViewResponse;

public interface JobCardProjectionService {

    JobCardViewResponse getJobCardView(Long jobCardId);
}
