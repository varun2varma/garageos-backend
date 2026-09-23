package com.garageos.modules.estimateitem.service.impl;

import com.garageos.core.enums.EstimateItemType;
import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.MoneyCalculator;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Override
    public EstimateItemResponse addItem(
            Long estimateId,
            CreateEstimateItemRequest request) {

        Estimate estimate = estimateRepository.findById(estimateId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : "
                                        + estimateId));

        EstimateItem item = mapper.toEntity(request);

        item.setEstimate(estimate);

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

        item.setItemType(
                EstimateItemType.valueOf(
                        request.getItemType().toUpperCase()));

        item.setTotalPrice(
                MoneyCalculator.calculateItemTotal(
                        request.getQuantity(),
                        request.getUnitPrice()));

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

        return mapper.toResponse(item);
    }

    @Override
    public List<EstimateItemResponse> getItems(Long estimateId) {

        return repository.findByEstimateId(estimateId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<EstimateItemResponse> getItemsByComplaint(Long complaintId) {

        return repository.findByComplaintId(complaintId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public EstimateItemResponse updateItem(
            Long id,
            CreateEstimateItemRequest request) {

        EstimateItem item = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate Item not found with id : " + id));

        mapper.updateEntity(request, item);

        item.setItemType(
                EstimateItemType.valueOf(
                        request.getItemType().toUpperCase()));

        item.setTotalPrice(
                MoneyCalculator.calculateItemTotal(
                        request.getQuantity(),
                        request.getUnitPrice()));

        item = repository.save(item);

        recalculateEstimate(item.getEstimate());

        return mapper.toResponse(item);
    }

    @Override
    public void deleteItem(Long id) {

        EstimateItem item = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate Item not found with id : " + id));

        Estimate estimate = item.getEstimate();

        repository.delete(item);

        recalculateEstimate(estimate);
    }

    private void recalculateEstimate(
            Estimate estimate) {

        List<EstimateItem> items =
                repository.findByEstimateId(estimate.getId());

        // Mission: a deselected item must not continue to repair/invoice.
        // Excluding it here, at the one place subtotal/gst/grandTotal are
        // computed, means that guarantee holds everywhere downstream reads
        // the estimate's totals, rather than needing a second exclusion
        // list at invoice time.
        List<EstimateItem> selectedItems = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getSelected()))
                .toList();

        BigDecimal subtotal = selectedItems.stream()
                .map(EstimateItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Mission's tax rule: GST applies to LABOUR only. PART and
        // OTHERS items contribute to subtotal/grandTotal but never to
        // the GST base.
        BigDecimal labourSubtotal = selectedItems.stream()
                .filter(item -> item.getItemType() == com.garageos.core.enums.EstimateItemType.LABOUR)
                .map(EstimateItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = estimate.getDiscount();

        BigDecimal gst =
                MoneyCalculator.calculateGST(
                        labourSubtotal,
                        discount);

        BigDecimal grandTotal =
                MoneyCalculator.calculateGrandTotal(
                        subtotal,
                        discount,
                        gst);

        estimate.setSubtotal(subtotal);
        estimate.setGst(gst);
        estimate.setGrandTotal(grandTotal);
        estimate.setStatus(EstimateStatus.WAITING_FOR_APPROVAL);

        estimateRepository.save(estimate);
    }

    @Override
    @Transactional
    public EstimateItemResponse setItemSelection(Long itemId, boolean selected) {

        EstimateItem item = repository.findById(itemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate Item not found with id : " + itemId));

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

}