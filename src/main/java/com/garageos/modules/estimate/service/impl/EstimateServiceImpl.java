package com.garageos.modules.estimate.service.impl;

import com.garageos.core.enums.*;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.EstimateNumberGenerator;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.estimate.dto.request.CreateEstimateRequest;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.mapper.EstimateMapper;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.inspection.entity.Inspection;
import com.garageos.modules.inspection.repository.InspectionRepository;
import com.garageos.modules.inspectionfinding.entity.InspectionFinding;
import com.garageos.modules.inspectionfinding.repository.InspectionFindingRepository;
import com.garageos.modules.inspectionmaster.entity.InspectionMasterItem;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.repairtask.service.RepairTaskService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EstimateServiceImpl implements EstimateService {

    private final EstimateRepository estimateRepository;
    private final JobCardRepository jobCardRepository;
    private final EstimateMapper estimateMapper;
    private final InspectionRepository inspectionRepository;
    private final EstimateItemRepository estimateItemRepository;
    private final InspectionFindingRepository inspectionFindingRepository;
    private final RepairTaskService repairTaskService;
    private final CustomerRepository customerRepository;
    private final JobCardStatusValidator statusValidator;

    @Override
    public EstimateResponse createEstimate(CreateEstimateRequest request) {

        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : "
                                        + request.getJobCardId()));

        Long garageId = jobCard.getGarage().getId();

        Optional<Estimate> latestEstimate =
                estimateRepository
                        .findTopByJobCardGarageIdOrderByIdDesc(garageId);

        String estimateNumber =
                EstimateNumberGenerator.generate(
                        jobCard.getGarage().getGarageCode(),
                        latestEstimate
                                .map(Estimate::getEstimateNumber)
                                .orElse(null));

        Estimate estimate = estimateMapper.toEntity(request);

        estimate.setEstimateNumber(estimateNumber);
        estimate.setJobCard(jobCard);
        estimate.setStatus(EstimateStatus.DRAFT);

        estimate.setSubtotal(BigDecimal.ZERO);
        estimate.setDiscount(BigDecimal.ZERO);
        estimate.setGst(BigDecimal.ZERO);
        estimate.setGrandTotal(BigDecimal.ZERO);

        estimate = estimateRepository.save(estimate);

        return estimateMapper.toResponse(estimate);
    }

    @Override
    public EstimateResponse getEstimate(Long id) {

        Estimate estimate = estimateRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        return estimateMapper.toResponse(estimate);
    }

    @Override
    public List<EstimateResponse> getAllEstimates() {

        return estimateRepository.findAll()
                .stream()
                .map(estimateMapper::toResponse)
                .toList();
    }

    /**
     * The JobCard transition to WAITING_FOR_APPROVAL only fires when the
     * JobCard is actually at ESTIMATE_PENDING — i.e. this specific update
     * is the one finishing an estimate draft and sending it for customer
     * approval, not an arbitrary later edit to an estimate whose JobCard
     * has already moved past that point. Previously this method mutated
     * a JobCard entity that was never re-saved (no @Transactional, no
     * explicit save), so the write was effectively lost; it is now
     * explicit and persisted.
     */
    @Override
    @Transactional
    public EstimateResponse updateEstimate(
            Long id,
            CreateEstimateRequest request) {

        Estimate estimate = estimateRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : "
                                        + request.getJobCardId()));

        if (jobCard.getStatus() == JobCardStatus.ESTIMATE_PENDING) {

            statusValidator.validate(
                    jobCard.getStatus(),
                    JobCardStatus.WAITING_FOR_APPROVAL
            );

            jobCard.setStatus(JobCardStatus.WAITING_FOR_APPROVAL);

            jobCardRepository.save(jobCard);
        }

        estimateMapper.updateEntity(request, estimate);

        estimate.setJobCard(jobCard);

        estimate = estimateRepository.save(estimate);

        return estimateMapper.toResponse(estimate);
    }

    @Override
    public void deleteEstimate(Long id) {

        Estimate estimate = estimateRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        estimateRepository.delete(estimate);
    }

    /**
     * PUT /estimates/{id}/approve and /reject are the estimate-approval
     * endpoints the customer portal (legacy JS and Flutter) actually calls.
     * No live employee UI calls this pair (the employee approval path goes
     * through ServiceWorkflowController instead), so this hotfix scopes
     * access to the authenticated customer who owns the estimate's job
     * card only. Every failure branch throws the identical not-found
     * response so a caller cannot distinguish "no such estimate" from
     * "exists, but isn't yours" — mirroring ownedJobCard()'s existing
     * not-found-over-forbidden convention in CustomerPortalServiceImpl.
     */
    private Estimate getOwnedCustomerEstimate(Long id) {

        Estimate estimate = estimateRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (!principal.getRoles().contains(RoleCode.CUSTOMER.name())) {
            throw new ResourceNotFoundException(
                    "Estimate not found with id : " + id);
        }

        Customer customer = customerRepository
                .findByMobileNumber(principal.getMobile())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        if (!estimate.getJobCard().getCustomer().getId()
                .equals(customer.getId())) {
            throw new ResourceNotFoundException(
                    "Estimate not found with id : " + id);
        }

        return estimate;
    }

    /**
     * Canonical estimate-approval domain operation. Every approval entry
     * point (customer PUT /estimates/{id}/approve, the employee workflow
     * POST /workflow/{jobCardNumber}/estimate/approve, and the JobCard
     * controller's POST /jobcards/{jobCardNumber}/estimate/approve, which
     * now delegates here via JobCardServiceImpl) converges on this method:
     * approve the Estimate, create the RepairTasks it authorizes, and
     * move the JobCard directly to REPAIR_PENDING — never the legacy
     * ESTIMATE_APPROVED state. Caller authorization/ownership is the
     * responsibility of each entry point before this is invoked (the
     * customer path's ownership check lives in getOwnedCustomerEstimate;
     * the employee workflow path's role check lives at its controller
     * layer per the existing MANAGER-only behavior).
     */
    private EstimateResponse approveEstimateCanonical(Estimate estimate) {

        if (estimate.getStatus() == EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "Estimate is already approved.");
        }

        estimate.setStatus(EstimateStatus.APPROVED);

        repairTaskService.createRepairTasks(estimate);

        JobCard jobCard = estimate.getJobCard();

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.REPAIR_PENDING
        );

        jobCard.setStatus(JobCardStatus.REPAIR_PENDING);

        jobCardRepository.save(jobCard);

        estimate = estimateRepository.save(estimate);

        return estimateMapper.toResponse(estimate);
    }

    @Override
    @Transactional
    public EstimateResponse approveEstimate(Long id) {

        Estimate estimate = getOwnedCustomerEstimate(id);

        return approveEstimateCanonical(estimate);
    }

    @Override
    public EstimateResponse rejectEstimate(Long id) {

        Estimate estimate = getOwnedCustomerEstimate(id);

        if (estimate.getStatus() == EstimateStatus.REJECTED) {
            throw new BusinessException(
                    "Estimate is already rejected.");
        }

        estimate.setStatus(EstimateStatus.REJECTED);

        estimate = estimateRepository.save(estimate);

        return estimateMapper.toResponse(estimate);
    }

    @Override
    @Transactional
    public EstimateResponse createEstimate(String jobCardNumber) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with number : " + jobCardNumber));

        List<Inspection> inspections =
                inspectionRepository.findByComplaintJobCardId(jobCard.getId());

        if (inspections.isEmpty()) {
            throw new BusinessException(
                    "Complete inspection before creating estimate.");
        }

        boolean allCompleted = inspections.stream()
                .allMatch(inspection ->
                        inspection.getStatus() == InspectionStatus.COMPLETED);

        if (!allCompleted) {
            throw new BusinessException(
                    "Complete all complaint inspections before creating estimate.");
        }

        /*
         * Estimate is created as an empty DRAFT.
         *
         * Estimate items are NOT generated automatically from
         * InspectionFinding / InspectionMasterItem.
         *
         * The service advisor adds Labour / Parts
         * complaint-by-complaint.
         */

        Long garageId = jobCard.getGarage().getId();

        Optional<Estimate> latestEstimate =
                estimateRepository
                        .findTopByJobCardGarageIdOrderByIdDesc(garageId);

        String estimateNumber =
                EstimateNumberGenerator.generate(
                        jobCard.getGarage().getGarageCode(),
                        latestEstimate
                                .map(Estimate::getEstimateNumber)
                                .orElse(null));

        Estimate estimate = new Estimate();

        estimate.setJobCard(jobCard);
        estimate.setEstimateNumber(estimateNumber);
        estimate.setStatus(EstimateStatus.DRAFT);

        estimate.setSubtotal(BigDecimal.ZERO);
        estimate.setDiscount(BigDecimal.ZERO);
        estimate.setGst(BigDecimal.ZERO);
        estimate.setGrandTotal(BigDecimal.ZERO);

        estimate = estimateRepository.save(estimate);

        return estimateMapper.toResponse(estimate);
    }

    private void createEstimateItems(Estimate estimate) {

        List<InspectionFinding> findings =
                inspectionFindingRepository.findByJobCardIdAndStatusIn(
                        estimate.getJobCard().getId(),
                        List.of(
                                InspectionFindingStatus.FAIL,
                                InspectionFindingStatus.REPAIR_REQUIRED
                        ));

        List<EstimateItem> items = new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (InspectionFinding finding : findings) {

            InspectionMasterItem masterItem =
                    finding.getInspectionMasterItem();

            BigDecimal labour =
                    masterItem.getLabourCost() == null
                            ? BigDecimal.ZERO
                            : masterItem.getLabourCost();

            BigDecimal parts =
                    masterItem.getPartCost() == null
                            ? BigDecimal.ZERO
                            : masterItem.getPartCost();

            BigDecimal total = labour.add(parts);

            EstimateItem item = new EstimateItem();

            item.setEstimate(estimate);

            // ✅ Keep complaint traceability
            item.setComplaint(finding.getComplaint());

            // ✅ Keep inspection traceability
            item.setInspectionFinding(finding);

            item.setItemType(EstimateItemType.LABOUR);

            item.setDescription(masterItem.getServiceName());

            item.setQuantity(BigDecimal.ONE);

            item.setUnitPrice(total);

            item.setTotalPrice(total);

            items.add(item);

            subtotal = subtotal.add(total);
        }

        estimateItemRepository.saveAll(items);

        estimate.setSubtotal(subtotal);

        estimate.setDiscount(BigDecimal.ZERO);

        BigDecimal gst = subtotal.multiply(new BigDecimal("0.18"));

        estimate.setGst(gst);

        estimate.setGrandTotal(subtotal.add(gst));

        estimateRepository.save(estimate);
    }

//    private void createEstimateItems(
//            Estimate estimate,
//            List<Inspection> inspections) {
//
//        List<EstimateItem> items = new ArrayList<>();
//
//        for (Inspection inspection : inspections) {
//
//            if (inspection.getRecommendedWork() == null
//                    || inspection.getRecommendedWork().isBlank()) {
//                continue;
//            }
//
//            EstimateItem item = new EstimateItem();
//
//            item.setEstimate(estimate);
//
//            item.setComplaint(inspection.getComplaint());
//
//            item.setItemType(EstimateItemType.LABOUR);
//
//            item.setDescription(
//                    inspection.getRecommendedWork());
//
//            item.setQuantity(BigDecimal.ONE);
//
//            item.setUnitPrice(BigDecimal.ZERO);
//
//            item.setTotalPrice(BigDecimal.ZERO);
//
//            items.add(item);
//        }
//
//        estimateItemRepository.saveAll(items);
//    }

    /**
     * Locked decision: employee estimate approval is MANAGER-only — not
     * OWNER, not SERVICE_ADVISOR (the lock explicitly preserves this
     * distinction even though Owner/Manager are equivalent for other
     * operational JobCard actions), scoped to the manager's own garage.
     * This governs only the employee workflow path (called from both
     * JobCardController and ServiceWorkflowController, which converge on
     * this same method); the customer path (approveEstimate(Long)) uses
     * getOwnedCustomerEstimate exclusively and is untouched by this check.
     */
    private void authorizeEmployeeEstimateApproval(JobCard jobCard) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (!principal.getRoles().contains(RoleCode.MANAGER.name())) {
            throw new BusinessException(
                    "Only a Manager can approve this estimate.");
        }

        if (jobCard.getGarage() == null
                || principal.getGarageId() == null
                || !jobCard.getGarage().getId().equals(principal.getGarageId())) {
            throw new BusinessException(
                    "You are not authorized to approve estimates for this garage.");
        }
    }

    @Override
    @Transactional
    public EstimateResponse approveEstimate(String jobCardNumber) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));

        authorizeEmployeeEstimateApproval(jobCard);

        Estimate estimate = estimateRepository
                .findByJobCardId(jobCard.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found for Job Card : "
                                        + jobCardNumber));

        return approveEstimateCanonical(estimate);
    }

    @Override
    @Transactional(readOnly = true)
    public EstimateResponse getEstimateByJobCard(Long jobCardId) {

        return estimateRepository
                .findByJobCardId(jobCardId)
                .map(estimateMapper::toResponse)
                .orElse(null);
    }
}