package com.garageos.modules.customer.service.impl;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.dto.response.portal.*;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.mapper.CustomerPortalMapper;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.customer.service.CustomerPortalService;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import com.garageos.modules.estimateitem.service.EstimateItemService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.media.dto.response.JobCardMediaResponse;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.mapper.JobCardMediaMapper;
import com.garageos.modules.media.repository.JobCardMediaRepository;
import com.garageos.modules.media.service.MediaContent;
import com.garageos.modules.media.service.MediaService;
import com.garageos.modules.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerPortalServiceImpl
        implements CustomerPortalService {

    private final CustomerRepository customerRepository;

    private final VehicleRepository vehicleRepository;

    private final JobCardRepository jobCardRepository;

    private final EstimateRepository estimateRepository;

    private final InvoiceRepository invoiceRepository;

    private final CustomerPortalMapper mapper;

    private final EstimateService estimateService;

    private final EstimateItemService estimateItemService;

    private final JobCardMediaRepository jobCardMediaRepository;

    private final JobCardMediaMapper jobCardMediaMapper;

    private final MediaService mediaService;

    private static final String VISIBILITY_CUSTOMER_VISIBLE = "CUSTOMER_VISIBLE";


    @Override
    public CustomerProfileResponse getProfile() {

        Customer customer = getCurrentCustomer();

        return mapper.toProfile(getCurrentCustomer());

    }

    private Customer getCurrentCustomer() {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return customerRepository
                .findByMobileNumber(principal.getMobile())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found."));
    }

    @Override
    public List<CustomerVehicleResponse> getVehicles() {

        Customer customer = getCurrentCustomer();

        return vehicleRepository
                .findByCustomer(customer)
                .stream()
                .map(mapper::toVehicle)
                .toList();

    }

    @Override
    public List<CustomerJobCardResponse> getJobCards() {

        Customer customer = getCurrentCustomer();

        return jobCardRepository
                .findByCustomer(getCurrentCustomer())
                .stream()
                .map(mapper::toJobCard)
                .toList();

    }

    @Override
    public List<CustomerEstimateResponse> getEstimates() {

        Customer customer = getCurrentCustomer();

        return estimateRepository
                .findByJobCardCustomer(customer)
                .stream()
                .map(mapper::toEstimate)
                .toList();

    }


    @Override
    public List<CustomerInvoiceResponse> getInvoices() {

        Customer customer = getCurrentCustomer();

        return invoiceRepository
                .findByEstimateJobCardCustomer(customer)
                .stream()
                .map(mapper::toInvoice)
                .toList();

    }

    @Override
    public CustomerInvoiceDetailsResponse getInvoiceDetails(Long invoiceId) {

        Customer customer = getCurrentCustomer();

        var invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invoice not found."));

        /*
         * Never expose an invoice just because the caller knows its ID.
         * The invoice belongs to an Estimate, which belongs to a Job Card,
         * which belongs to the Customer.
         */
        if (!invoice.getEstimate()
                .getJobCard()
                .getCustomer()
                .getId()
                .equals(customer.getId())) {

            throw new ResourceNotFoundException("Invoice not found.");
        }

        Long estimateId = invoice.getEstimate().getId();

        InvoiceResponse invoiceResponse =
                InvoiceResponse.builder()
                        .id(invoice.getId())
                        .invoiceNumber(invoice.getInvoiceNumber())
                        .estimateId(estimateId)
                        .invoiceStatus(invoice.getInvoiceStatus().name())
                        .paymentStatus(invoice.getPaymentStatus().name())
                        .subtotal(invoice.getSubtotal())
                        .discount(invoice.getDiscount())
                        .gst(invoice.getGst())
                        .grandTotal(invoice.getGrandTotal())
                        .remarks(invoice.getRemarks())
                        .build();

        List<EstimateItemResponse> items =
                estimateItemService.getItems(estimateId);

        return CustomerInvoiceDetailsResponse.builder()
                .invoice(invoiceResponse)
                .jobCardNumber(
                        invoice.getEstimate()
                                .getJobCard()
                                .getJobCardNumber()
                )
                .items(items)
                .build();
    }

    @Override
    public CustomerDashboardResponse getDashboard() {

        Customer customer = getCurrentCustomer();

        return CustomerDashboardResponse.builder()
                .vehicleCount(
                        vehicleRepository.countByCustomer(customer))
                .activeJobCount(
                        jobCardRepository.countByCustomer(customer))
                .pendingEstimateCount(
                        estimateRepository.countByJobCardCustomer(customer))
                .pendingInvoiceCount(
                        invoiceRepository.countByEstimateJobCardCustomer(customer))
                .build();


    }

    /**
     * Corrective fix: this method previously hardcoded
     * inspectionCompleted=true unconditionally, aliased estimateApproved
     * to "an estimate row exists" (true as soon as an estimate is merely
     * prepared, before the customer ever approves it), and hardcoded
     * repairCompleted/qualityChecked/paymentCompleted to false forever —
     * so the customer's own repair-tracking screen could never show those
     * three milestones as done no matter how far the job actually
     * progressed. Confirmed live: a job that had genuinely reached
     * READY_FOR_DELIVERY (repaired, QC passed, invoiced, paid) still
     * reported repairCompleted=false and qualityChecked=false to the
     * customer. Every milestone here is now derived from the JobCard's
     * real status, using the same canonical lifecycle
     * JobCardStatusValidator already encodes — "has the job reached at
     * least this point" — plus the actual Estimate.status for approval,
     * which is the one milestone with a more precise source than the
     * JobCard status string alone.
     */
    private static final Map<JobCardStatus, Integer> LIFECYCLE_RANK = Map.ofEntries(
            Map.entry(JobCardStatus.OPEN, 0),
            Map.entry(JobCardStatus.INSPECTION_PENDING, 1),
            Map.entry(JobCardStatus.INSPECTION_COMPLETED, 2),
            Map.entry(JobCardStatus.ESTIMATE_PENDING, 3),
            Map.entry(JobCardStatus.WAITING_FOR_APPROVAL, 4),
            Map.entry(JobCardStatus.ESTIMATE_APPROVED, 5),   // legacy synonym, see JobCardStatusValidator
            Map.entry(JobCardStatus.REPAIR_PENDING, 5),
            Map.entry(JobCardStatus.REPAIR_IN_PROGRESS, 6),
            Map.entry(JobCardStatus.REPAIR_COMPLETED, 7),
            Map.entry(JobCardStatus.WORK_COMPLETED, 7),      // legacy synonym
            Map.entry(JobCardStatus.QUALITY_CHECK, 7),       // legacy synonym (mid-QC, repair already done)
            Map.entry(JobCardStatus.READY_FOR_INVOICE, 8),
            Map.entry(JobCardStatus.INVOICE_GENERATED, 9),
            Map.entry(JobCardStatus.INVOICED, 9),            // legacy synonym
            Map.entry(JobCardStatus.PAYMENT_PENDING, 9),     // legacy synonym
            Map.entry(JobCardStatus.PAYMENT_COMPLETED, 10),
            Map.entry(JobCardStatus.READY_FOR_DELIVERY, 10),
            Map.entry(JobCardStatus.DELIVERED, 11),
            Map.entry(JobCardStatus.CLOSED, 12),
            Map.entry(JobCardStatus.CANCELLED, -1)
    );

    private static boolean reached(JobCardStatus current, JobCardStatus milestone) {
        return LIFECYCLE_RANK.getOrDefault(current, -1) >= LIFECYCLE_RANK.get(milestone);
    }

    @Override
    public CustomerRepairTrackingResponse trackRepair(String jobCardNumber) {

        Customer customer = getCurrentCustomer();

        JobCard jobCard =
                jobCardRepository.findByJobCardNumber(jobCardNumber)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Job Card not found."));

        if (!jobCard.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Job Card not found.");
        }

        JobCardStatus status = jobCard.getStatus();

        var estimate = estimateRepository.findByJobCardId(jobCard.getId());
        boolean estimatePrepared = estimate.isPresent();
        boolean estimateApproved = estimate
                .map(e -> e.getStatus() == EstimateStatus.APPROVED)
                .orElse(false);

        boolean invoiceGenerated =
                invoiceRepository.findByEstimateJobCardId(jobCard.getId()).isPresent();

        return CustomerRepairTrackingResponse.builder()
                .jobCardNumber(jobCard.getJobCardNumber())
                .registrationNumber(jobCard.getVehicle().getRegistrationNumber())
                .status(jobCard.getStatus())
                .serviceDate(jobCard.getServiceDate())
                .estimatedDeliveryDate(jobCard.getEstimatedDeliveryDate())

                .inspectionCompleted(reached(status, JobCardStatus.INSPECTION_COMPLETED))
                .estimatePrepared(estimatePrepared)
                .estimateApproved(estimateApproved)
                .repairCompleted(reached(status, JobCardStatus.REPAIR_COMPLETED))
                .qualityChecked(reached(status, JobCardStatus.READY_FOR_INVOICE))
                .invoiceGenerated(invoiceGenerated)
                .paymentCompleted(reached(status, JobCardStatus.PAYMENT_COMPLETED))

                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerEstimateDetailsResponse getEstimateDetails(
            Long estimateId) {

        Customer customer = getCurrentCustomer();

        Estimate estimateEntity =
                estimateRepository.findById(estimateId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Estimate not found."));

        if (!estimateEntity.getJobCard().getCustomer().getId()
                .equals(customer.getId())) {
            throw new ResourceNotFoundException(
                    "Estimate not found.");
        }

        EstimateResponse estimate =
                estimateService.getEstimate(
                        estimateId
                );

        List<EstimateItemResponse> items =
                estimateItemService.getItems(
                        estimateId
                );

        return CustomerEstimateDetailsResponse.builder()
                .estimate(estimate)
                .items(items)
                .build();

    }

    @Override
    public List<JobCardMediaResponse> getJobCardMedia(
            String jobCardNumber) {

        JobCard jobCard =
                ownedJobCard(jobCardNumber);

        List<JobCardMedia> media =
                jobCardMediaRepository
                        .findByJobCardIdAndVisibilityOrderByCreatedAtAsc(
                                jobCard.getId(),
                                VISIBILITY_CUSTOMER_VISIBLE
                        );

        return jobCardMediaMapper.toResponseList(media);
    }

    @Override
    public MediaContent getJobCardMediaContent(
            String jobCardNumber,
            Long mediaId) {

        JobCard jobCard =
                ownedJobCard(jobCardNumber);

        JobCardMedia media =
                jobCardMediaRepository.findById(mediaId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Media not found with id : " + mediaId));

        // Never trust mediaId alone: it must belong to a job card this
        // customer owns, and it must be customer-visible even if it does.
        if (!jobCard.getId().equals(media.getJobCardId())
                || !VISIBILITY_CUSTOMER_VISIBLE.equals(media.getVisibility())) {

            throw new ResourceNotFoundException(
                    "Media not found with id : " + mediaId);
        }

        return mediaService.downloadContent(media);
    }

    /**
     * Resolves a Job Card by number and verifies it belongs to the
     * currently authenticated customer, throwing the same
     * ResourceNotFoundException {@link #trackRepair} already throws on a
     * mismatch (never revealing that the job card exists but belongs to
     * someone else).
     */
    private JobCard ownedJobCard(String jobCardNumber) {

        Customer customer = getCurrentCustomer();

        JobCard jobCard =
                jobCardRepository.findByJobCardNumber(jobCardNumber)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Job Card not found."));

        if (!jobCard.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Job Card not found.");
        }

        return jobCard;
    }

}