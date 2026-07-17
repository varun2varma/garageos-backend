package com.garageos.modules.estimateitem.service.impl;

import com.garageos.core.enums.EstimateItemType;
import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.MoneyCalculator;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.complaint.repository.ComplaintRepository;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimateitem.dto.request.CreateEstimateItemRequest;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.mapper.EstimateItemMapper;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.estimateitem.service.EstimateItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        if (request.getComplaintId() != null) {

            Complaint complaint =
                    complaintRepository.findById(
                                    request.getComplaintId())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Complaint not found with id : "
                                                    + request.getComplaintId()));

            item.setComplaint(complaint);
        }

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

        BigDecimal subtotal = items.stream()
                .map(EstimateItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = estimate.getDiscount();

        BigDecimal gst =
                MoneyCalculator.calculateGST(
                        subtotal,
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

}