package com.garageos.modules.jobcard.service.impl;

import com.garageos.core.enums.ComplaintStatus;
import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.booking.BookingStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
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
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.jobcard.entity.JobCard;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobCardServiceImpl implements JobCardService {

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

        if (booking != null) {
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);
        }

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

        return jobCardMapper.toResponse(jobCard);
    }

    @Override
    public JobCardResponse updateJobCard(
            Long id,
            CreateJobCardRequest request) {

        JobCard jobCard = getJobCardByIdOrThrow(id);

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

        if (!principal.getRoles().contains(RoleCode.MANAGER.name())) {
            throw new BusinessException(
                    "Only a Manager can proceed a Job Card to repair.");
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
                CreateQualityCheckRequest.builder()
                        .inspectedBy("Legacy Quality Check Endpoint")
                        .build()
        );

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse readyForDelivery(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.READY_FOR_DELIVERY
        );

        jobCard.setStatus(JobCardStatus.READY_FOR_DELIVERY);

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

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.CLOSED
        );

        jobCard.setStatus(JobCardStatus.CLOSED);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
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
                || principal.getGarageId() == null
                || !principal.getGarageId().equals(jobCard.getGarage().getId())) {

            throw new BusinessException(
                    "E1: This Job Card does not belong to your garage.");
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