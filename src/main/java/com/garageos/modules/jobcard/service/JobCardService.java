package com.garageos.modules.jobcard.service;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import org.springframework.data.domain.Page;

import java.util.List;


public interface JobCardService {

    JobCardResponse createJobCard(CreateJobCardRequest request);

    JobCardResponse getJobCard(Long id);

    JobCardResponse updateJobCard(Long id,
                                  CreateJobCardRequest request);

    void deleteJobCard(Long id);

    JobCardResponse getJobCardByNumber(String jobCardNumber);

    /**
     * @param requestedGarageId null uses the caller's own garage context;
     *                          non-null lets a user with an ACTIVE
     *                          membership in another garage (e.g. a
     *                          multi-garage Owner) view that garage's job
     *                          cards instead - validated, never trusted
     *                          blindly.
     * @param allMyGarages      when true (and requestedGarageId is null),
     *                          returns job cards across every garage the
     *                          caller has an ACTIVE membership in - the
     *                          Owner "All Garages" view. Ignored for a
     *                          caller with only one garage membership.
     * @param statuses          null/empty returns every status (existing
     *                          behaviour); otherwise narrows the page to
     *                          those statuses, so operational Job filters
     *                          are applied in the database rather than in
     *                          the client.
     */
    Page<JobCardResponse> getAllJobCards(
            int page,
            int size,
            String sortBy,
            String direction,
            Long requestedGarageId,
            boolean allMyGarages,
            List<JobCardStatus> statuses
    );

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