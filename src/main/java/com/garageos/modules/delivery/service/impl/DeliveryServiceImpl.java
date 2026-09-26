package com.garageos.modules.delivery.service.impl;

import com.garageos.core.enums.DeliveryStatus;
import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.PaymentStatus;
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
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository repository;
    private final JobCardRepository jobCardRepository;
    private final InvoiceRepository invoiceRepository;
    private final DeliveryMapper mapper;
    private final JobCardStatusValidator statusValidator;

    /**
     * Existing semantics already treat Delivery creation as the actual
     * completed-delivery event (DeliveryStatus is set straight to
     * DELIVERED here, with no separate "mark delivered" step anywhere in
     * this service) — so this is the correct point to transition the
     * JobCard to DELIVERED, per the canonical lifecycle. Invoice-before-
     * delivery was already enforced (InvoiceStatus.GENERATED check);
     * payment-before-delivery is added here since it previously was not
     * enforced anywhere.
     */
    @Override
    @Transactional
    public DeliveryResponse createDelivery(CreateDeliveryRequest request) {

        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : "
                                        + request.getJobCardId()));

        authorizeDeliveryAction(jobCard);

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

        if (invoice.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BusinessException(
                    "Invoice must be paid before delivery can be completed.");
        }

        Delivery delivery = mapper.toEntity(request);

        delivery.setJobCard(jobCard);
        delivery.setInvoice(invoice);

        delivery.setDeliveryDateTime(LocalDateTime.now());

        delivery.setStatus(DeliveryStatus.DELIVERED);

        delivery = repository.save(delivery);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.DELIVERED
        );

        jobCard.setStatus(JobCardStatus.DELIVERED);

        jobCardRepository.save(jobCard);

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

    /**
     * Corrective fix for Defect #7: createDelivery() (which is the actual
     * completed-delivery event - see the class-level doc comment above)
     * relied solely on controller-level role protection, with no tenant
     * scoping in the service layer, so a MANAGER/OWNER/SERVICE_ADVISOR
     * from any garage could complete delivery for any other garage's
     * JobCard. Mirrors the garage-match check already used successfully
     * in RepairTaskServiceImpl and QualityCheckServiceImpl.
     */
    private void authorizeDeliveryAction(JobCard jobCard) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (jobCard.getGarage() == null
                || principal.getGarageId() == null
                || !principal.getGarageId().equals(jobCard.getGarage().getId())) {

            throw new BusinessException(
                    "E2: This Job Card does not belong to your garage.");
        }
    }
}