package com.garageos.modules.customer.service;

import com.garageos.modules.customer.dto.request.portal.UpdateCustomerProfileRequest;
import com.garageos.modules.customer.dto.response.portal.*;
import com.garageos.modules.media.dto.response.JobCardMediaResponse;
import com.garageos.modules.media.dto.response.MediaAccessResponse;
import com.garageos.modules.media.service.MediaContent;

import java.util.List;

public interface CustomerPortalService {

    CustomerProfileResponse getProfile();

    /** Mission backlog #14 — self-service profile edit, for the calling customer only. */
    CustomerProfileResponse updateProfile(UpdateCustomerProfileRequest request);

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

    /**
     * Provider-aware playback access for one media item on a job card the
     * calling customer owns — a presigned R2 URL, or (for legacy Drive rows)
     * the existing proxied content path. Same ownership/visibility
     * authorization as {@link #getJobCardMediaContent}; the difference is
     * this resolves through {@code MediaService.resolvePlaybackAccess}, so
     * R2-backed media (thumbnail/original/evidence) works for customers the
     * same way it already does for employees, instead of only through the
     * Drive-only {@code downloadContent} path.
     */
    MediaAccessResponse getJobCardMediaAccess(String jobCardNumber, Long mediaId, String variant);

}