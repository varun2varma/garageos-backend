package com.garageos.modules.estimateitem.service.impl;

import com.garageos.core.enums.EstimateItemType;
import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.RepairStatus;
import com.garageos.core.enums.catalog.CatalogItemType;
import com.garageos.core.enums.catalog.ClassificationStatus;
import com.garageos.core.enums.catalog.EntrySource;
import com.garageos.core.enums.catalog.ReviewStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.security.GarageScope;
import com.garageos.core.util.MoneyCalculator;
import com.garageos.modules.catalog.entity.CatalogItem;
import com.garageos.modules.catalog.service.CatalogService;
import com.garageos.modules.estimateitem.tax.EstimateTotalsCalculator;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.complaint.repository.ComplaintRepository;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimateitem.dto.request.CreateEstimateItemRequest;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.mapper.EstimateItemMapper;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.estimateitem.service.EstimateItemService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EstimateItemServiceImpl
        implements EstimateItemService {

    private final EstimateRepository estimateRepository;
    private final EstimateItemRepository repository;
    private final ComplaintRepository complaintRepository;
    private final EstimateItemMapper mapper;
    private final CustomerRepository customerRepository;
    private final RepairTaskRepository repairTaskRepository;
    private final CatalogService catalogService;
    private final EstimateTotalsCalculator totalsCalculator;
    private final GarageScope garageScope;

    /**
     * Read access to an estimate's lines: unrestricted, staff of the job card's garage (GarageScope, so an Owner's
     * other garages count), or the customer the job card belongs to. Anyone else gets the usual not-found, so
     * another garage's prices are never exposed by guessing an id.
     */
    private void requireReadable(Estimate estimate, String notFoundMessage) {

        GarageUserPrincipal principal = garageScope.principal();

        if (garageScope.isUnrestricted(principal)) {
            return;
        }

        var jobCard = estimate == null ? null : estimate.getJobCard();

        boolean staff = jobCard != null
                && jobCard.getGarage() != null
                && garageScope.canAccessGarage(principal, jobCard.getGarage().getId());

        boolean ownCustomer = jobCard != null
                && jobCard.getCustomer() != null
                && principal.getMobile() != null
                && principal.getMobile().equals(jobCard.getCustomer().getMobileNumber());

        if (!staff && !ownCustomer) {
            throw new ResourceNotFoundException(notFoundMessage);
        }
    }

    private void recalculateEstimate(Estimate estimate) {
        totalsCalculator.recalculate(estimate);
    }

    /**
     * Staff mutation guard: the caller must belong to the estimate's garage
     * (never a customer), and an approved estimate is final - changing a line
     * would silently un-approve it and drift from the invoice built on it.
     */
    private void requireEditable(Estimate estimate) {

        GarageUserPrincipal principal = garageScope.principal();

        if (garageScope.isCustomerOnly(principal)) {
            throw new ResourceNotFoundException("Estimate not found.");
        }

        Long garageId = estimate.getJobCard().getGarage() == null
                ? null
                : estimate.getJobCard().getGarage().getId();

        if (garageId != null && !garageScope.canAccessGarage(principal, garageId)) {
            throw new ResourceNotFoundException("Estimate not found.");
        }

        if (estimate.getStatus() == EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "This estimate has already been approved and can no longer be changed.");
        }
    }

    /**
     * Takes the estimate row lock (held until this transaction commits) and only then loads the line, so neither the
     * estimate state nor the line read afterwards can be a stale pre-lock copy.
     */
    private EstimateItem lockEstimateThenLoadItem(Long itemId) {

        Long estimateId = repository.findEstimateIdById(itemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate Item not found with id : " + itemId));

        estimateRepository.findByIdForUpdate(estimateId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate Item not found with id : " + itemId));

        return repository.findById(itemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate Item not found with id : " + itemId));
    }

    private static EstimateItemType parseItemType(String raw) {
        try {
            return EstimateItemType.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Item type must be LABOUR, PART or OTHERS.");
        }
    }

    private static void validateLine(CreateEstimateItemRequest request) {
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new BusinessException("Item description is required.");
        }
        if (request.getQuantity() == null || request.getQuantity().signum() <= 0) {
            throw new BusinessException("Quantity must be greater than zero.");
        }
        if (request.getUnitPrice() == null || request.getUnitPrice().signum() < 0) {
            throw new BusinessException("Unit price must not be negative.");
        }
    }

    /**
     * Catalog selection versus manual entry. A catalog id must reference an active
     * item of the matching kind; the classification is snapshotted onto the line.
     * Without an id the line is MANUAL and queued for review - no catalog id,
     * code or rate is ever invented, and provenance comes from the authenticated
     * principal and the job card, never from the request.
     */
    private void applyProvenance(
            EstimateItem item, CreateEstimateItemRequest request, Estimate estimate, boolean isNew) {

        EstimateItemType type = item.getItemType();

        item.setUnit(blankToNull(request.getUnit()));
        item.setManualNote(blankToNull(request.getManualNote()));

        if (!isNew && request.getCatalogItemId() == null) {
            // Editing description, quantity or price never changes where a line came from.
            return;
        }

        if (request.getCatalogItemId() != null) {

            if (type == EstimateItemType.OTHERS) {
                throw new BusinessException("Other charges are not catalog items.");
            }

            CatalogItem catalogItem = catalogService.requireUsable(
                    request.getCatalogItemId(),
                    type == EstimateItemType.PART ? CatalogItemType.PART : CatalogItemType.SERVICE);

            item.setCatalogItemId(catalogItem.getId());
            item.setEntrySource(EntrySource.MASTER);
            item.setReviewStatus(ReviewStatus.NOT_REQUIRED);

            if (item.getUnit() == null) {
                item.setUnit(catalogItem.getDefaultUnit());
            }

            item.setHsnSacCode(catalogItem.getHsnSacCode());
            item.setTaxRuleCode(catalogItem.getTaxRuleCode());
            item.setGstRatePercent(
                    catalogItem.getClassificationStatus() == ClassificationStatus.VERIFIED
                            ? catalogService.resolveRate(catalogItem.getTaxRuleCode(), LocalDate.now()).orElse(null)
                            : null);
            return;
        }

        item.setCatalogItemId(null);
        item.setEntrySource(EntrySource.MANUAL);
        item.setReviewStatus(type == EstimateItemType.OTHERS
                ? ReviewStatus.NOT_REQUIRED
                : ReviewStatus.NEEDS_REVIEW);
        // Manual lines carry no classification: nothing is guessed here.
        item.setHsnSacCode(null);
        item.setTaxRuleCode(null);
        item.setGstRatePercent(null);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    @Transactional
    public EstimateItemResponse addItem(
            Long estimateId,
            CreateEstimateItemRequest request) {

        // Lock first, so the status checked below is the committed state and cannot change until this commits.
        Estimate estimate = estimateRepository.findByIdForUpdate(estimateId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : "
                                        + estimateId));

        requireEditable(estimate);

        validateLine(request);

        EstimateItem item = mapper.toEntity(request);

        item.setEstimate(estimate);

        item.setGarageId(estimate.getJobCard().getGarage() == null
                ? null
                : estimate.getJobCard().getGarage().getId());
        item.setCreatedByUserId(garageScope.principal().getId());

        if (request.getComplaintId() == null) {
            throw new ResourceNotFoundException(
                    "Complaint is required for an estimate item.");
        }

        Complaint complaint = complaintRepository.findById(
                request.getComplaintId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Complaint not found with id : "
                                + request.getComplaintId()));

        if (!complaint.getJobCard().getId()
                .equals(estimate.getJobCard().getId())) {

            throw new ResourceNotFoundException(
                    "Complaint does not belong to this Job Card.");
        }

        item.setComplaint(complaint);

        item.setItemType(parseItemType(request.getItemType()));

        item.setTotalPrice(
                MoneyCalculator.calculateItemTotal(
                        request.getQuantity(),
                        request.getUnitPrice()));

        applyProvenance(item, request, estimate, true);

        item = repository.save(item);

        recalculateEstimate(estimate);

        return mapper.toResponse(item);
    }

    @Override
    public EstimateItemResponse getItem(Long id) {

        EstimateItem item = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate Item not found with id : " + id));

        requireReadable(item.getEstimate(), "Estimate Item not found with id : " + id);

        return mapper.toResponse(item);
    }

    @Override
    public List<EstimateItemResponse> getItems(Long estimateId) {

        estimateRepository.findById(estimateId)
                .ifPresent(e -> requireReadable(e, "Estimate not found with id : " + estimateId));

        return repository.findByEstimateId(estimateId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<EstimateItemResponse> getItemsByComplaint(Long complaintId) {

        List<EstimateItem> found = repository.findByComplaintId(complaintId);

        // every line of one complaint belongs to one job card, so checking each distinct estimate is enough
        found.stream()
                .map(EstimateItem::getEstimate)
                .distinct()
                .forEach(e -> requireReadable(e, "Complaint not found with id : " + complaintId));

        return found
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public EstimateItemResponse updateItem(
            Long id,
            CreateEstimateItemRequest request) {

        EstimateItem item = lockEstimateThenLoadItem(id);

        requireEditable(item.getEstimate());

        validateLine(request);

        mapper.updateEntity(request, item);

        item.setItemType(parseItemType(request.getItemType()));

        item.setTotalPrice(
                MoneyCalculator.calculateItemTotal(
                        request.getQuantity(),
                        request.getUnitPrice()));

        applyProvenance(item, request, item.getEstimate(), false);

        item = repository.save(item);

        recalculateEstimate(item.getEstimate());

        return mapper.toResponse(item);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {

        EstimateItem item = lockEstimateThenLoadItem(id);

        Estimate estimate = item.getEstimate();

        requireEditable(estimate);

        repository.delete(item);

        recalculateEstimate(estimate);
    }

    @Override
    @Transactional
    public EstimateItemResponse setItemSelection(Long itemId, boolean selected) {

        EstimateItem item = lockEstimateThenLoadItem(itemId);

        Estimate estimate = item.getEstimate();

        GarageUserPrincipal principal = (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (!principal.getRoles().contains(RoleCode.CUSTOMER.name())) {
            throw new ResourceNotFoundException("Estimate Item not found with id : " + itemId);
        }

        Customer customer = customerRepository.findByMobileNumber(principal.getMobile())
                .orElseThrow(() -> new ResourceNotFoundException("Estimate Item not found with id : " + itemId));

        if (estimate.getJobCard().getCustomer() == null
                || !estimate.getJobCard().getCustomer().getId().equals(customer.getId())) {

            throw new ResourceNotFoundException("Estimate Item not found with id : " + itemId);
        }

        // Mission: "the approved estimate is final for that workflow
        // cycle" - selection is only meaningful before that point.
        if (estimate.getStatus() == EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "This estimate has already been approved and can no longer be changed.");
        }

        item.setSelected(selected);

        item = repository.save(item);

        recalculateEstimate(estimate);

        return mapper.toResponse(item);
    }

    @Override
    @Transactional
    public List<EstimateItemResponse> setComplaintItemsSelection(
            Long complaintId,
            boolean selected) {

        List<Long> estimateIds = repository.findEstimateIdsByComplaintId(complaintId);

        if (estimateIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Estimate items not found for complaint id : " + complaintId);
        }

        // Lock first, then read the complaint's lines, so they are the committed state held under the lock.
        estimateRepository.findByIdForUpdate(estimateIds.get(0))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estimate items not found for complaint id : " + complaintId));

        List<EstimateItem> items = repository.findByComplaintId(complaintId);

        if (items.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Estimate items not found for complaint id : " + complaintId);
        }

        Estimate estimate = items.get(0).getEstimate();

        GarageUserPrincipal principal = (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (!principal.getRoles().contains(RoleCode.CUSTOMER.name())) {
            throw new ResourceNotFoundException(
                    "Complaint not found with id : " + complaintId);
        }

        Customer customer = customerRepository.findByMobileNumber(principal.getMobile())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complaint not found with id : " + complaintId));

        if (estimate.getJobCard().getCustomer() == null
                || !estimate.getJobCard().getCustomer().getId().equals(customer.getId())) {

            throw new ResourceNotFoundException(
                    "Complaint not found with id : " + complaintId);
        }

        if (estimate.getStatus() == EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "This estimate has already been approved and can no longer be changed.");
        }

        for (EstimateItem item : items) {
            item.setSelected(selected);
        }

        items = repository.saveAll(items);

        recalculateEstimate(estimate);

        // Mission: a rejected complaint's repair work must not remain
        // active. Any RepairTask already created for this complaint (e.g.
        // the customer changes their selection after an earlier approval
        // cycle already spawned one) is cancelled, never deleted -
        // preserving history exactly as RepairStatus.CANCELLED already
        // does for every other cancellation path in this codebase.
        if (!selected) {
            repairTaskRepository
                    .findByJobCardIdAndComplaintId(
                            estimate.getJobCard().getId(),
                            complaintId)
                    .filter(task -> task.getStatus() != RepairStatus.COMPLETED
                            && task.getStatus() != RepairStatus.CANCELLED)
                    .ifPresent(task -> {
                        task.setStatus(RepairStatus.CANCELLED);
                        repairTaskRepository.save(task);
                    });
        }

        return items.stream().map(mapper::toResponse).toList();
    }

}