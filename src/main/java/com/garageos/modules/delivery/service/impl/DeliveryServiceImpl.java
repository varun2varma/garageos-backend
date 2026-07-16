package com.garageos.modules.delivery.service.impl;

import com.garageos.core.enums.DeliveryStatus;
import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.delivery.dto.request.CreateDeliveryRequest;
import com.garageos.modules.delivery.dto.response.DeliveryResponse;
import com.garageos.modules.delivery.entity.Delivery;
import com.garageos.modules.delivery.mapper.DeliveryMapper;
import com.garageos.modules.delivery.repository.DeliveryRepository;
import com.garageos.modules.delivery.service.DeliveryService;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository repository;
    private final JobCardRepository jobCardRepository;
    private final InvoiceRepository invoiceRepository;
    private final DeliveryMapper mapper;

    @Override
    public DeliveryResponse createDelivery(CreateDeliveryRequest request) {

        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : "
                                        + request.getJobCardId()));

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found with id : "
                                        + request.getInvoiceId()));

        if (repository.existsByJobCardId(jobCard.getId())) {
            throw new BusinessException(
                    "Delivery already exists for this Job Card.");
        }

        if (repository.existsByInvoiceId(invoice.getId())) {
            throw new BusinessException(
                    "Delivery already exists for this Invoice.");
        }

        if (invoice.getInvoiceStatus() != InvoiceStatus.GENERATED) {
            throw new BusinessException(
                    "Only generated invoices can be delivered.");
        }

        Delivery delivery = mapper.toEntity(request);

        delivery.setJobCard(jobCard);
        delivery.setInvoice(invoice);

        delivery.setDeliveryDateTime(LocalDateTime.now());

        delivery.setStatus(DeliveryStatus.DELIVERED);

        delivery = repository.save(delivery);

        return mapper.toResponse(delivery);
    }

    @Override
    public DeliveryResponse getDelivery(Long id) {

        Delivery delivery = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Delivery not found with id : " + id));

        return mapper.toResponse(delivery);
    }

    @Override
    public Page<DeliveryResponse> getAllDeliveries(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Override
    public DeliveryResponse updateDelivery(
            Long id,
            CreateDeliveryRequest request) {

        Delivery delivery = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Delivery not found with id : " + id));

        mapper.updateEntity(request, delivery);

        delivery = repository.save(delivery);

        return mapper.toResponse(delivery);
    }

    @Override
    public void deleteDelivery(Long id) {

        Delivery delivery = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Delivery not found with id : " + id));

        repository.delete(delivery);
    }
}