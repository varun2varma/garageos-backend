package com.garageos.modules.jobcard.service;

import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import org.springframework.data.domain.Page;


public interface JobCardService {

    JobCardResponse createJobCard(CreateJobCardRequest request);

    JobCardResponse getJobCard(Long id);

    JobCardResponse updateJobCard(Long id,
                                  CreateJobCardRequest request);

    void deleteJobCard(Long id);

    JobCardResponse getJobCardByNumber(String jobCardNumber);

    Page<JobCardResponse> getAllJobCards(
            int page,
            int size,
            String sortBy,
            String direction);

    JobCardResponse startInspection(String jobCardNumber);

    JobCardResponse completeInspection(String jobCardNumber);

    JobCardResponse prepareEstimate(String jobCardNumber);

    JobCardResponse approveEstimate(String jobCardNumber);

    JobCardResponse startRepair(String jobCardNumber);

    JobCardResponse completeRepair(String jobCardNumber);

    JobCardResponse performQualityCheck(String jobCardNumber);

    JobCardResponse readyForDelivery(String jobCardNumber);

    JobCardResponse closeJobCard(String jobCardNumber);
    JobCardResponse invoiceGenerated(String jobCardNumber);
}