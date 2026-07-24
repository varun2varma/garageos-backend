package com.garageos.modules.qualitycheck.service;

import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.qualitycheck.dto.request.CreateQualityCheckRequest;
import com.garageos.modules.qualitycheck.dto.response.QualityCheckResponse;

public interface QualityCheckService {

    void createQualityCheck(JobCard jobCard);

    QualityCheckResponse getQualityCheck(String jobCardNumber);

    QualityCheckResponse passQualityCheck(
            String jobCardNumber,
            CreateQualityCheckRequest request);

    QualityCheckResponse failQualityCheck(
            String jobCardNumber,
            CreateQualityCheckRequest request);
}