package com.garageos.modules.customer.service;

import com.garageos.modules.customer.dto.response.portal.*;
import com.garageos.modules.media.dto.response.JobCardMediaResponse;
import com.garageos.modules.media.service.MediaContent;

import java.util.List;

public interface CustomerPortalService {

    CustomerProfileResponse getProfile();

    CustomerDashboardResponse getDashboard();

    List<CustomerVehicleResponse> getVehicles();

    List<CustomerJobCardResponse> getJobCards();

    List<CustomerEstimateResponse> getEstimates();

    List<CustomerInvoiceResponse> getInvoices();

    CustomerInvoiceDetailsResponse getInvoiceDetails(Long invoiceId);

    CustomerRepairTrackingResponse trackRepair(String jobCardNumber);

    CustomerEstimateDetailsResponse getEstimateDetails(Long estimateId);

    /**
     * Customer-visible media for a job card the calling customer owns.
     * Only ever returns {@code CUSTOMER_VISIBLE} media — internal-only
     * media (e.g. DURING_REPAIR shots) is never exposed here, matching the
     * visibility already computed at upload time.
     */
    List<JobCardMediaResponse> getJobCardMedia(String jobCardNumber);

    /**
     * Content download for one media item on a job card the calling
     * customer owns. Rejects anything not {@code CUSTOMER_VISIBLE} even if
     * the media id happens to belong to that job card.
     */
    MediaContent getJobCardMediaContent(String jobCardNumber, Long mediaId);

}