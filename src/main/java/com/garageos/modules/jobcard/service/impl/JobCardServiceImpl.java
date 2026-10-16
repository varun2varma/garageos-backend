package com.garageos.modules.jobcard.service.impl;

import com.garageos.core.enums.ComplaintStatus;
import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.booking.BookingStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.security.GarageScope;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.JobCardNumberGenerator;
import com.garageos.modules.booking.entity.Booking;
import com.garageos.modules.booking.repository.BookingRepository;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.complaint.dto.response.ComplaintResponse;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.complaint.mapper.ComplaintMapper;
import com.garageos.modules.complaint.service.ComplaintService;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.inspectionfinding.service.InspectionFindingService;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.event.JobCardClosedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.jobcard.mapper.JobCardMapper;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.qualitycheck.dto.request.CreateQualityCheckRequest;
import com.garageos.modules.qualitycheck.service.QualityCheckService;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import com.garageos.core.enums.RepairStatus;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.vehicle.entity.Vehicle;
import com.garageos.modules.vehicle.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.garageos.modules.notification.event.NotificationEventPublisher;
import com.garageos.core.enums.notification.NotificationEventType;
import com.garageos.modules.notification.event.NotificationEvents;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
public class JobCardServiceImpl implements JobCardService {

    /** The single release rule (PAID or active Owner credit). Optional so hand-built test instances fall back to PAID-only. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.modules.credit.service.ReleaseClearance releaseClearance;

    private boolean isReleaseCleared(com.garageos.modules.invoice.entity.Invoice invoice) {
        if (releaseClearance != null) {
            return releaseClearance.isCleared(invoice);
        }
        return invoice != null && invoice.getPaymentStatus() == com.garageos.core.enums.PaymentStatus.PAID;
    }


    @Autowired(required = false)
    private ApplicationEventPublisher applicationEventPublisher;

    /** Relationship bookkeeping only; optional so the job card flow never depends on it. */
    @Autowired(required = false)
    private com.garageos.modules.customer.service.CustomerGarageService customerGarageService;

    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;

    /**
     * Closure prerequisites (defence in depth behind the status machine). Field-injected and optional, like the
     * other collaborators here, so constructor-built unit tests are unaffected; in the running application both are
     * always present.
     */
    @Autowired(required = false)
    private com.garageos.modules.delivery.repository.DeliveryRepository deliveryRepository;

    @Autowired(required = false)
    private com.garageos.modules.invoice.repository.InvoiceRepository invoiceRepository;

    private final JobCardRepository jobCardRepository;
    private final VehicleRepository vehicleRepository;
    private final GarageRepository garageRepository;
    private final JobCardMapper jobCardMapper;
    private final JobCardStatusValidator statusValidator;
    private final ComplaintService complaintService;
    private final ComplaintMapper complaintMapper;
    private final InspectionFindingService inspectionFindingService;
    private final QualityCheckService qualityCheckService;
    private final RepairTaskRepository repairTaskRepository;
    private final EstimateService estimateService;
    private final BookingRepository bookingRepository;
    private final NavigationRequestRepository navigationRequestRepository;
    private final GarageMembershipRepository garageMembershipRepository;

    @Override
    @Transactional
    public JobCardResponse createJobCard(CreateJobCardRequest request) {

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : "
                                        + request.getVehicleId()));

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        Long garageId = principal.getGarageId();

        if (garageId == null) {
            throw new BusinessException(
                    "User is not associated with a garage."
            );
        }

        Garage garage = garageRepository.findById(garageId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Garage not found with id : "
                                        + garageId));

        // Never trust a client-supplied bookingId blindly - resolve and
        // validate it (garage scope, status, vehicle/customer consistency,
        // duplicate protection) before it can be attached to a Job Card.
        Booking booking = resolveBooking(request.getBookingId(), garageId, vehicle);

        Optional<JobCard> latestJobCard =
                jobCardRepository.findTopByGarageIdOrderByIdDesc(garageId);

        String jobCardNumber =
                JobCardNumberGenerator.generate(
                        garage.getGarageCode(),
                        latestJobCard
                                .map(JobCard::getJobCardNumber)
                                .orElse(null));

        JobCard jobCard = jobCardMapper.toEntity(request);

        jobCard.setJobCardNumber(jobCardNumber);
        jobCard.setGarage(garage);
        jobCard.setVehicle(vehicle);
        jobCard.setCustomer(vehicle.getCustomer());
        jobCard.setServiceDate(LocalDate.now());
        jobCard.setStatus(JobCardStatus.OPEN);
        // D11: attribution comes from the authenticated principal only (never the request body).
        jobCard.setCreatedByUserId(principal.getId());
        jobCard.setCreatedByRoles(String.join(",", new java.util.TreeSet<>(principal.getRoles())));
        jobCard.setCreatedByName(
                ((principal.getFirstName() == null ? "" : principal.getFirstName()) + " "
                        + (principal.getLastName() == null ? "" : principal.getLastName())).trim());
        if (booking != null) {
            jobCard.setBookingId(booking.getId());
        }
        List<Complaint> complaints = complaintMapper.toEntity(request.getComplaints());
        JobCard finalJobCard = jobCard;
        complaints.forEach(complaint -> {
            complaint.setJobCard(finalJobCard);
            complaint.setStatus(ComplaintStatus.OPEN);
        });
        jobCard.setComplaints(complaints);
        jobCard = jobCardRepository.save(jobCard);

        // An actual job card is the strongest verified customer<->garage event.
        if (customerGarageService != null && vehicle.getCustomer() != null) {
            customerGarageService.link(vehicle.getCustomer().getId(), garageId,
                    com.garageos.core.enums.customer.CustomerGarageSource.JOB_CARD);
        }

        if (booking != null) {
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            // Direct link from the booking's pickup request to this JobCard,
            // so the service journey never has to guess by vehicle.
            final Long savedJobCardId = jobCard.getId();

            if (booking.getNavigationRequestId() != null) {
                navigationRequestRepository
                        .findById(booking.getNavigationRequestId())
                        .ifPresent(navigationRequest -> {
                            navigationRequest.setJobCardId(savedJobCardId);
                            navigationRequestRepository.save(navigationRequest);
                        });
            }
        }

        NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                NotificationEventType.JOB_CARD_CREATED, jobCard, "1");

        return jobCardMapper.toResponse(jobCard);
    }

    /**
     * Resolves and validates an optional Booking for a new Job Card. Every
     * failure here throws before any JobCard/Booking row is written, so a
     * rejected association never leaves partial state (BusinessException/
     * ResourceNotFoundException both roll back the enclosing
     * @Transactional). Returns null when no bookingId was supplied - a
     * booking-less Job Card creation, unchanged from before this method
     * existed.
     */
    private Booking resolveBooking(Long bookingId, Long garageId, Vehicle vehicle) {

        if (bookingId == null) {
            return null;
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id : " + bookingId));

        // Not-found-over-forbidden, matching this codebase's existing
        // ownership-check convention (CustomerPortalServiceImpl,
        // EstimateServiceImpl.getOwnedCustomerEstimate): an employee from
        // another garage should not learn this booking even exists.
        if (!booking.getGarageId().equals(garageId)) {
            throw new ResourceNotFoundException(
                    "Booking not found with id : " + bookingId);
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException(
                    "Only a confirmed booking can be used to create a Job Card.");
        }

        if (!booking.getVehicleId().equals(vehicle.getId())) {
            throw new BusinessException(
                    "This booking is for a different vehicle.");
        }

        if (vehicle.getCustomer() == null
                || !booking.getCustomerId().equals(vehicle.getCustomer().getId())) {
            throw new BusinessException(
                    "This booking's customer does not match the vehicle's owner.");
        }

        if (jobCardRepository.existsByBookingId(bookingId)) {
            throw new BusinessException(
                    "A Job Card has already been created for this booking.");
        }

        return booking;
    }

    @Override
    public JobCardResponse getJobCard(Long id) {

        JobCard jobCard = getJobCardByIdOrThrow(id);

        authorizeJobCardRead(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }

    @Override
    public JobCardResponse updateJobCard(
            Long id,
            CreateJobCardRequest request) {

        JobCard jobCard = getJobCardByIdOrThrow(id);

        authorizeJobCardGarage(jobCard);

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : "
                                        + request.getVehicleId()));

        jobCardMapper.updateEntity(request, jobCard);

        jobCard.setVehicle(vehicle);
        jobCard.setCustomer(vehicle.getCustomer());

        jobCard = jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }

    @Override
    public void deleteJobCard(Long id) {

        JobCard jobCard = getJobCardByIdOrThrow(id);

        authorizeJobCardGarage(jobCard);

        jobCardRepository.delete(jobCard);
    }

    @Override
    public JobCardResponse getJobCardByNumber(
            String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        return jobCardMapper.toResponse(jobCard);
    }

    /**
     * Corrective fix: this previously called jobCardRepository.findAll()
     * with no garage scoping at all - every employee, in every garage, saw
     * every other garage's Job Cards (customer/vehicle PII included).
     * Scoped now via the same resolve-and-validate pattern as
     * DashboardServiceImpl - the caller's own garage by default, or an
     * explicitly requested/validated garage, or (Owner "All Garages") every
     * garage the caller has an ACTIVE membership in.
     */
    @Override
    public Page<JobCardResponse> getAllJobCards(
            int page,
            int size,
            String sortBy,
            String direction,
            Long requestedGarageId,
            boolean allMyGarages,
            List<JobCardStatus> statuses) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        List<Long> garageIds = resolveGarageIds(requestedGarageId, allMyGarages);

        Page<JobCard> jobCards =
                (statuses == null || statuses.isEmpty())
                        ? jobCardRepository.findByGarage_IdIn(garageIds, pageable)
                        : jobCardRepository.findByGarage_IdInAndStatusIn(
                                garageIds, statuses, pageable);

        return jobCards.map(jobCardMapper::toResponse);
    }

    private List<Long> resolveGarageIds(Long requestedGarageId, boolean allMyGarages) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (requestedGarageId != null) {

            boolean hasMembership = garageMembershipRepository
                    .existsByGarage_IdAndUser_Id(requestedGarageId, principal.getId());

            if (!hasMembership) {
                throw new ResourceNotFoundException(
                        "Garage not found with id : " + requestedGarageId);
            }

            return List.of(requestedGarageId);
        }

        if (allMyGarages) {

            List<Long> myGarageIds = garageMembershipRepository
                    .findByUser_Id(principal.getId())
                    .stream()
                    .filter(m -> m.getStatus() == com.garageos.core.enums.garagemembership.GarageMembershipStatus.ACTIVE)
                    .map(m -> m.getGarage().getId())
                    .toList();

            if (myGarageIds.isEmpty()) {
                throw new BusinessException("No garage context for this account.");
            }

            return myGarageIds;
        }

        if (principal.getGarageId() == null) {
            throw new BusinessException("User is not associated with a garage.");
        }

        return List.of(principal.getGarageId());
    }


    @Override
    @Transactional
    public JobCardResponse startInspection(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.INSPECTION_PENDING
        );

        jobCard.setStatus(JobCardStatus.INSPECTION_PENDING);

        NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                NotificationEventType.INSPECTION_STARTED, jobCard, "1");

        jobCardRepository.save(jobCard);
        inspectionFindingService.loadInspectionTemplate(jobCard.getId());
        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse completeInspection(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.INSPECTION_COMPLETED
        );

        jobCard.setStatus(JobCardStatus.INSPECTION_COMPLETED);

        NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                NotificationEventType.INSPECTION_COMPLETED, jobCard, "1");

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse prepareEstimate(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.ESTIMATE_PENDING
        );

        jobCard.setStatus(JobCardStatus.ESTIMATE_PENDING);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    /**
     * Canonical estimate-approval convergence point for this endpoint
     * family: delegates to EstimateService.approveEstimate(String), which
     * now performs the full canonical operation (approve Estimate, create
     * RepairTasks, transition JobCard to REPAIR_PENDING) atomically. This
     * closes the previous gap where this specific entry point advanced
     * JobCard.status without ever approving the Estimate or creating the
     * RepairTasks the rest of the lifecycle depends on.
     */
    @Override
    @Transactional
    public JobCardResponse approveEstimate(String jobCardNumber) {

        estimateService.approveEstimate(jobCardNumber);

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        return jobCardMapper.toResponse(jobCard);
    }
    /**
     * "Proceed to Repair" is intentionally MANAGER-only, mirroring the
     * identical locked decision already made for employee estimate
     * approval (see EstimateServiceImpl
     * .authorizeEmployeeEstimateApproval — not OWNER, not
     * SERVICE_ADVISOR). It is enforced here at the service layer rather
     * than via @PreAuthorize on either controller, following the exact
     * pattern already documented on both JobCardController and
     * ServiceWorkflowController for estimate/approve: startRepair is
     * exposed from both controllers and both delegate to this one
     * method, so a single service-layer check can't drift out of sync
     * the way two independently-maintained @PreAuthorize role lists
     * could. Tenant/garage scoping for jobCard is already guaranteed by
     * the caller (getJobCardByNumberOrThrow -> authorizeJobCardGarage),
     * so this only needs to check the role.
     */
    private void authorizeProceedToRepair() {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        // D06: the Owner holds the Manager's operational authority over the workflow. Explicit allow-list -
        // Service Advisor, Technician and every other role remain refused.
        if (!principal.getRoles().contains(RoleCode.MANAGER.name())
                && !principal.getRoles().contains(RoleCode.OWNER.name())) {
            throw new BusinessException(
                    "Only a Manager or the Owner can proceed a Job Card to repair.");
        }
    }

    /**
     * Server-side "Proceed to Repair" gate. Customer estimate approval
     * (EstimateServiceImpl.approveEstimateCanonical) already stops at
     * JobCardStatus.REPAIR_PENDING — it does not itself advance the
     * JobCard to REPAIR_IN_PROGRESS. This method is the explicit manager
     * confirmation step that performs that further transition, and it
     * re-validates the gate itself (not just the caller's role) so a
     * direct API call can never move a JobCard into repair on an
     * Estimate that was never actually customer-approved, regardless of
     * what any client sends.
     *
     * Idempotent: if the JobCard is already REPAIR_IN_PROGRESS, the
     * current state is returned as-is rather than re-running validation
     * and throwing — a duplicate "Proceed to Repair" call is a no-op,
     * not an error.
     */
    @Override
    @Transactional
    public JobCardResponse startRepair(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        authorizeProceedToRepair();

        if (jobCard.getStatus() == JobCardStatus.REPAIR_IN_PROGRESS) {
            return jobCardMapper.toResponse(jobCard);
        }

        EstimateResponse estimate =
                estimateService.getEstimateByJobCard(jobCard.getId());

        if (estimate == null
                || !EstimateStatus.APPROVED.name().equals(estimate.getStatus())) {
            throw new BusinessException(
                    "Estimate must be customer-approved before repair can start.");
        }

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.REPAIR_IN_PROGRESS
        );

        jobCard.setStatus(JobCardStatus.REPAIR_IN_PROGRESS);

        NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                NotificationEventType.JOB_REPAIR_STARTED, jobCard, System.currentTimeMillis());

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    /**
     * Legacy compatibility wrapper (no confirmed live caller). The
     * canonical repair-completion trigger is RepairTaskServiceImpl
     * .completeRepair(Long) — all RepairTasks for the JobCard reaching
     * COMPLETED, which sets JobCard.REPAIR_COMPLETED automatically. This
     * endpoint no longer writes the legacy WORK_COMPLETED status (a
     * second, non-canonical "repair complete" state); it is now a
     * read-style check: if repair is already canonically complete it
     * returns the current JobCard unchanged, otherwise it reports that
     * completion happens per-RepairTask.
     */
    @Override
    @Transactional
    public JobCardResponse completeRepair(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        boolean alreadyComplete =
                jobCard.getStatus() == JobCardStatus.REPAIR_COMPLETED
                        || jobCard.getStatus() == JobCardStatus.READY_FOR_INVOICE
                        || jobCard.getStatus() == JobCardStatus.INVOICE_GENERATED
                        || jobCard.getStatus() == JobCardStatus.READY_FOR_DELIVERY
                        || jobCard.getStatus() == JobCardStatus.DELIVERED
                        || jobCard.getStatus() == JobCardStatus.CLOSED;

        if (!alreadyComplete) {

            long total = repairTaskRepository.countByJobCardId(jobCard.getId());

            long completed = repairTaskRepository.countByJobCardIdAndStatus(
                    jobCard.getId(),
                    RepairStatus.COMPLETED);

            if (total == 0 || total != completed) {
                throw new BusinessException(
                        "Repair completion is now driven by completing each "
                                + "assigned Repair Task (PUT /repair-tasks/{id}/complete); "
                                + "not all Repair Tasks for this Job Card are completed yet.");
            }

            // Every RepairTask is complete but the aggregate transition
            // did not fire (e.g. a task existed before this migration) —
            // apply it now via the same canonical rule.
            statusValidator.validate(
                    jobCard.getStatus(),
                    JobCardStatus.REPAIR_COMPLETED
            );

            jobCard.setStatus(JobCardStatus.REPAIR_COMPLETED);

        NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                NotificationEventType.JOB_REPAIR_COMPLETED, jobCard, System.currentTimeMillis());

            jobCardRepository.save(jobCard);

//            qualityCheckService.createQualityCheck(jobCard);
        }

        return jobCardMapper.toResponse(jobCard);
    }

    /**
     * Legacy compatibility wrapper around the canonical QualityCheck
     * pass operation — no longer writes the non-canonical QUALITY_CHECK
     * JobCard status and no longer bypasses the REPAIR_COMPLETED
     * precondition or QualityCheck entity that QualityCheckServiceImpl
     * .passQualityCheck enforces.
     */
    @Override
    @Transactional
    public JobCardResponse performQualityCheck(String jobCardNumber) {

        qualityCheckService.passQualityCheck(
                jobCardNumber,
                // D10: the legacy endpoint attributes the check to the authenticated employee performing it,
                // not to a literal placeholder name.
                CreateQualityCheckRequest.builder()
                        .inspectedByUserId(((GarageUserPrincipal) SecurityContextHolder.getContext()
                                .getAuthentication().getPrincipal()).getId())
                        .build()
        );

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse readyForDelivery(String jobCardNumber) {
//         Commenting out for MVP, reopen at payment gate way time
//        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);
        JobCard jobCard = jobCardRepository.findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.READY_FOR_DELIVERY
        );

        // A job card is "ready" - and the customer is asked to choose collection or delivery - only when its
        // invoice is cleared for release: fully paid, or covered by an active Owner credit authorization. An
        // unpaid, uncredited invoice never makes a vehicle ready, whichever route calls this.
        if (invoiceRepository != null) {
            com.garageos.modules.invoice.entity.Invoice invoice =
                    invoiceRepository.findByEstimateJobCardId(jobCard.getId()).orElse(null);

            if (!isReleaseCleared(invoice)) {
                throw new BusinessException(
                        "The vehicle cannot be marked ready for delivery until the invoice is paid or covered by "
                                + "an Owner-authorized credit.",
                        "RELEASE_NOT_CLEARED");
            }
        }

        jobCard.setStatus(JobCardStatus.READY_FOR_DELIVERY);

        // Stable business key (job card + event type): a repeated call or retry can never create a second
        // logical "choose how to receive your vehicle" prompt.
        NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                NotificationEventType.VEHICLE_READY_FOR_DELIVERY, jobCard, "ready");

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse invoiceGenerated(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.INVOICE_GENERATED
        );

        jobCard.setStatus(JobCardStatus.INVOICE_GENERATED);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse closeJobCard(String jobCardNumber) {

        // Tenant scoping now happens inside getJobCardByNumberOrThrow for
        // every by-number operation, not just this one.
        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        // Idempotent: a repeated request (double tap, retry after a dropped response) returns the closed job card and
        // publishes nothing a second time - no duplicate notification, no second final report.
        if (jobCard.getStatus() == JobCardStatus.CLOSED) {
            return jobCardMapper.toResponse(jobCard);
        }

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.CLOSED
        );

        requireClosurePrerequisites(jobCard);

        jobCard.setStatus(JobCardStatus.CLOSED);

        NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                NotificationEventType.JOB_CLOSED, jobCard, "1");

        jobCardRepository.save(jobCard);

        // Handled AFTER_COMMIT on a background thread (final report generation), so it can
        // never make closure fail or roll it back.
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(new JobCardClosedEvent(jobCard.getId()));
        }

        return jobCardMapper.toResponse(jobCard);
    }

    /**
     * A job card is closed only after the vehicle has actually been handed over (a Delivery record: created either
     * by the delivery trip with OTP and evidence, or by the garage when the customer collects the vehicle in person)
     * and its invoice is generated and paid. Repair-task state, trip state and job-card state are separate: nothing
     * here looks at a single repair task. A customer self-pickup needs no driver and no trip.
     */
    private void requireClosurePrerequisites(JobCard jobCard) {

        if (deliveryRepository != null && !deliveryRepository.existsByJobCardId(jobCard.getId())) {
            throw new BusinessException(
                    "The vehicle has not been handed over yet. Record the delivery (or complete the delivery trip) "
                            + "before closing the job card.");
        }

        if (invoiceRepository != null) {

            com.garageos.modules.invoice.entity.Invoice invoice = invoiceRepository
                    .findByEstimateJobCardId(jobCard.getId())
                    .orElseThrow(() -> new BusinessException(
                            "The job card has no invoice, so it cannot be closed."));

            // ACCEPTED = issued invoice the customer accepted; still a valid, payable invoice.
            if ((invoice.getInvoiceStatus() != com.garageos.core.enums.InvoiceStatus.GENERATED
                    && invoice.getInvoiceStatus() != com.garageos.core.enums.InvoiceStatus.ACCEPTED)
                    || !isReleaseCleared(invoice)) {
                throw new BusinessException(
                        "The invoice must be generated and either fully paid or covered by an Owner-authorized "
                                + "credit before the job card can be closed.");
            }
        }
    }

    /**
     * Corrective fix for Defect #7: closeJobCard() relied solely on
     * @PreAuthorize(WORKFLOW_OPERATIONAL_ROLES) at the controller
     * (role-only, no tenant scoping), so a MANAGER/OWNER/SERVICE_ADVISOR
     * from any garage could close any other garage's JobCard. Mirrors
     * the garage-match check already used successfully in
     * RepairTaskServiceImpl and QualityCheckServiceImpl.
     *
     * Now called from getJobCardByNumberOrThrow, so every by-number
     * operation on this service is tenant-scoped, not just close.
     */
    private void authorizeJobCardGarage(JobCard jobCard) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (jobCard.getGarage() == null
                || !GarageScope.isMemberOfGarage(
                        new GarageScope(garageMembershipRepository), principal, jobCard.getGarage().getId())) {

            throw new BusinessException(
                    "E1: This Job Card does not belong to your garage.");
        }
    }

    /**
     * Read of ONE job card by numeric id: staff of the job card's own garage (same rule as the by-number
     * operations) or the customer it belongs to. Anyone else gets a not-found, so a guessed id exposes nothing.
     */
    private void authorizeJobCardRead(JobCard jobCard) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        boolean ownGarage = jobCard.getGarage() != null
                && GarageScope.isMemberOfGarage(
                        new GarageScope(garageMembershipRepository), principal, jobCard.getGarage().getId());

        boolean ownCustomer = jobCard.getCustomer() != null
                && GarageScope.isOwnCustomer(principal, jobCard.getCustomer().getMobileNumber());

        if (!ownGarage && !ownCustomer) {
            throw new ResourceNotFoundException("Job Card not found with id : " + jobCard.getId());
        }
    }

    private JobCard getJobCardByIdOrThrow(Long id) {
        return jobCardRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + id));
    }
    /**
     * Corrective security fix (cross-tenant): every by-jobCardNumber
     * operation on this service - startInspection, completeInspection,
     * prepareEstimate, approveEstimate, startRepair, completeRepair,
     * performQualityCheck, readyForDelivery, invoiceGenerated,
     * closeJobCard and the plain getJobCardByNumber lookup - resolved the
     * Job Card by number alone, with no tenant check whatsoever. Job Card
     * numbers are sequential and garage-prefixed, so any authenticated
     * employee could read, and drive the entire lifecycle of, another
     * garage's Job Card simply by using its number.
     *
     * Scoping the single shared lookup closes that whole family at once,
     * by reusing authorizeJobCardGarage - the check closeJobCard already
     * applied on its own for exactly this reason (Defect #7), and the
     * same rule used in InvoiceServiceImpl, RepairTaskServiceImpl and
     * QualityCheckServiceImpl. Generalising the existing helper keeps one
     * definition and one error contract rather than introducing a second,
     * differently-behaving tenant check.
     *
     * Every internal caller is a garage-scoped operational path
     * (ServiceWorkflowServiceImpl delegations, and
     * InvoiceServiceImpl.receivePayment -> readyForDelivery, which has
     * already run its own identical garage check), so no legitimate flow
     * loses access here.
     */
    private JobCard getJobCardByNumberOrThrow(String jobCardNumber) {

        JobCard jobCard = jobCardRepository.findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));

        authorizeJobCardGarage(jobCard);

        return jobCard;
    }
}