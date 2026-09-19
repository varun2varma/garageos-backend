package com.garageos.modules.invoice.service.impl;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.InvoiceNumberGenerator;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.invoice.dto.request.CreateInvoiceRequest;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.mapper.InvoiceMapper;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.invoice.service.InvoiceService;
import com.garageos.modules.invoiceitem.entity.InvoiceItem;
import com.garageos.modules.invoiceitem.repository.InvoiceItemRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final EstimateRepository estimateRepository;
    private final InvoiceMapper invoiceMapper;
    private final JobCardRepository jobCardRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final EstimateItemRepository estimateItemRepository;
    private final JobCardStatusValidator statusValidator;
    private final JobCardService jobCardService;
    private final CustomerRepository customerRepository;

    /**
     * Canonical invoice-generation JobCard transition, shared by every
     * invoice-creation entry point. Idempotent: a JobCard already at or
     * past INVOICE_GENERATED is left untouched rather than re-validated,
     * so a legacy entry point invoked after the canonical one already
     * ran does not fail on a redundant call.
     */
    private void transitionJobCardToInvoiceGenerated(JobCard jobCard) {

        if (jobCard.getStatus() == JobCardStatus.INVOICE_GENERATED) {
            return;
        }

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.INVOICE_GENERATED
        );

        jobCard.setStatus(JobCardStatus.INVOICE_GENERATED);

        jobCardRepository.save(jobCard);
    }

    @Override
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {

        Estimate estimate = estimateRepository.findById(request.getEstimateId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : "
                                        + request.getEstimateId()));

        if (estimate.getStatus() != EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "Only approved estimates can be converted to invoice.");
        }

        if (invoiceRepository.existsByEstimateId(estimate.getId())) {
            throw new BusinessException(
                    "Invoice already exists for this estimate.");
        }

        JobCard jobCard = estimate.getJobCard();

        Long garageId = jobCard.getGarage().getId();

        Optional<Invoice> latestInvoice =
                invoiceRepository
                        .findTopByEstimateJobCardGarageIdOrderByIdDesc(garageId);

        String invoiceNumber =
                InvoiceNumberGenerator.generate(
                        jobCard.getGarage().getGarageCode(),
                        latestInvoice
                                .map(Invoice::getInvoiceNumber)
                                .orElse(null));

        Invoice invoice = invoiceMapper.toEntity(request);

        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setEstimate(estimate);

        invoice.setInvoiceStatus(InvoiceStatus.GENERATED);
        invoice.setPaymentStatus(PaymentStatus.PENDING);

        invoice.setSubtotal(estimate.getSubtotal());
        invoice.setDiscount(estimate.getDiscount());
        invoice.setGst(estimate.getGst());
        invoice.setGrandTotal(estimate.getGrandTotal());

        invoice = invoiceRepository.save(invoice);

        transitionJobCardToInvoiceGenerated(jobCard);

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoice(Long id) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found with id : " + id));

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public Page<InvoiceResponse> getAllInvoices(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return invoiceRepository.findAll(pageable)
                .map(invoiceMapper::toResponse);
    }

//    @Override
//    public InvoiceResponse updateInvoice(
//            Long id,
//            CreateInvoiceRequest request) {
//
//        throw new UnsupportedOperationException(
//                "Invoice update is not allowed after creation.");
//    }

    @Override
    public void deleteInvoice(Long id) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found with id : " + id));

        invoiceRepository.delete(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByInvoiceNumber(
            String invoiceNumber) {

        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found : " + invoiceNumber));

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse generateInvoice(String jobCardNumber) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));

        authorizeInvoiceAction(jobCard);

        Estimate estimate = estimateRepository
                .findByJobCardId(jobCard.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found for Job Card : "
                                        + jobCardNumber));

        if (estimate.getStatus() != EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "Estimate must be approved before invoice generation.");
        }

        if (invoiceRepository.existsByEstimateId(estimate.getId())) {
            throw new BusinessException(
                    "Invoice already exists for this Job Card.");
        }

        Long garageId = jobCard.getGarage().getId();

        Optional<Invoice> latestInvoice =
                invoiceRepository
                        .findTopByEstimateJobCardGarageIdOrderByIdDesc(garageId);

        String invoiceNumber =
                InvoiceNumberGenerator.generate(
                        jobCard.getGarage().getGarageCode(),
                        latestInvoice
                                .map(Invoice::getInvoiceNumber)
                                .orElse(null));

        Invoice invoice = new Invoice();

        invoice.setInvoiceNumber(invoiceNumber);

        invoice.setEstimate(estimate);

        invoice.setInvoiceStatus(InvoiceStatus.GENERATED);

        invoice.setPaymentStatus(PaymentStatus.PENDING);

        invoice.setSubtotal(estimate.getSubtotal());

        invoice.setDiscount(estimate.getDiscount());

        invoice.setGst(estimate.getGst());

        invoice.setGrandTotal(estimate.getGrandTotal());
        invoice.setGeneratedAt(LocalDateTime.now());
        invoice = invoiceRepository.save(invoice);

        transitionJobCardToInvoiceGenerated(jobCard);

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse generateInvoiceByEstimateId(Long estimateId) {

        Estimate estimate = estimateRepository.findById(estimateId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found"));

        if (estimate.getStatus() != EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "Only approved estimates can generate invoices.");
        }

        if (invoiceRepository.existsByEstimateId(estimateId)) {
            throw new BusinessException(
                    "Invoice already generated.");
        }

        JobCard jobCard = estimate.getJobCard();

        Long garageId = jobCard.getGarage().getId();

        Optional<Invoice> latestInvoice =
                invoiceRepository
                        .findTopByEstimateJobCardGarageIdOrderByIdDesc(garageId);

        String invoiceNumber =
                InvoiceNumberGenerator.generate(
                        jobCard.getGarage().getGarageCode(),
                        latestInvoice
                                .map(Invoice::getInvoiceNumber)
                                .orElse(null));

        Invoice invoice = new Invoice();

        invoice.setInvoiceNumber(invoiceNumber);

        invoice.setEstimate(estimate);

        invoice.setInvoiceStatus(InvoiceStatus.DRAFT);

        invoice.setPaymentStatus(PaymentStatus.PENDING);

        invoice.setSubtotal(estimate.getSubtotal());

        invoice.setDiscount(estimate.getDiscount());

        invoice.setGst(estimate.getGst());

        invoice.setGrandTotal(estimate.getGrandTotal());

        invoice.setRemarks(estimate.getRemarks());

        invoice.setGeneratedAt(LocalDateTime.now());

        invoice = invoiceRepository.save(invoice);

        copyEstimateItems(invoice, estimate);

        transitionJobCardToInvoiceGenerated(jobCard);

        return invoiceMapper.toResponse(invoice);
    }

    private void copyEstimateItems(
            Invoice invoice,
            Estimate estimate) {

        List<EstimateItem> estimateItems =
                estimateItemRepository.findByEstimateId(estimate.getId());

        List<InvoiceItem> invoiceItems = new ArrayList<>();

        for (EstimateItem estimateItem : estimateItems) {

            InvoiceItem invoiceItem = new InvoiceItem();

            invoiceItem.setInvoice(invoice);

            invoiceItem.setComplaint(estimateItem.getComplaint());

//            invoiceItem.setInspectionFinding(
//                    estimateItem.getInspectionFinding());

            invoiceItem.setItemType(estimateItem.getItemType());

            invoiceItem.setDescription(estimateItem.getDescription());

            invoiceItem.setQuantity(estimateItem.getQuantity());

            invoiceItem.setUnitPrice(estimateItem.getUnitPrice());

            invoiceItem.setTotalPrice(estimateItem.getTotalPrice());

            invoiceItems.add(invoiceItem);
        }

        invoiceItemRepository.saveAll(invoiceItems);
    }

    /**
     * Canonical payment transition: Invoice.paymentStatus is the
     * authoritative payment record (never JobCardStatus.PAYMENT_PENDING/
     * PAYMENT_COMPLETED, which remain unused legacy enum values). A
     * successful payment atomically advances the JobCard to
     * READY_FOR_DELIVERY via the same validated transition
     * JobCardServiceImpl.readyForDelivery already implements — this is
     * what makes INVOICE_GENERATED -> READY_FOR_DELIVERY a validator-legal
     * transition instead of the previously-confirmed contradiction where
     * ServiceWorkflowServiceImpl called this out-of-band after the fact.
     */
    @Override
    @Transactional
    public InvoiceResponse receivePayment(String jobCardNumber) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));

        authorizePaymentAction(jobCard);

        Invoice invoice = invoiceRepository
                .findByEstimateJobCardId(jobCard.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found for Job Card : "
                                        + jobCardNumber));

        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException(
                    "Invoice already paid.");
        }

        invoice.setPaymentStatus(PaymentStatus.PAID);

        invoice = invoiceRepository.save(invoice);

        if (jobCard.getStatus() != JobCardStatus.READY_FOR_DELIVERY) {
            jobCardService.readyForDelivery(jobCardNumber);
        }

        return invoiceMapper.toResponse(invoice);
    }

    /**
     * receivePayment()'s own authorization - deliberately separate from
     * authorizeInvoiceAction(), which stays exactly as-is for
     * generateInvoice() and every staff caller. A CUSTOMER principal is
     * not a garage employee (no meaningful principal.getGarageId() to
     * compare), so the garage-match check does not apply to them at all;
     * they are authorized instead by owning the Job Card itself, the same
     * Customer-by-mobile-number lookup JobCardProjectionServiceImpl
     * already uses for the customer job-card view. A customer who does
     * not own this Job Card gets the same ResourceNotFoundException the
     * rest of the app already uses to avoid confirming another
     * customer's Job Card/invoice exists.
     */
    private void authorizePaymentAction(JobCard jobCard) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (principal.getRoles().contains(RoleCode.CUSTOMER.name())) {

            Customer customer = customerRepository
                    .findByMobileNumber(principal.getMobile())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Job Card not found : "
                                            + jobCard.getJobCardNumber()));

            if (jobCard.getCustomer() == null
                    || !jobCard.getCustomer().getId().equals(customer.getId())) {

                throw new ResourceNotFoundException(
                        "Job Card not found : " + jobCard.getJobCardNumber());
            }

            return;
        }

        authorizeInvoiceAction(jobCard);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByJobCard(Long jobCardId) {

        return invoiceRepository
                .findByEstimateJobCardId(jobCardId)
                .map(invoiceMapper::toResponse)
                .orElse(null);
    }

    /**
     * Corrective fix for Defect #7: generateInvoice()/receivePayment()
     * relied solely on @PreAuthorize(WORKFLOW_OPERATIONAL_ROLES) at the
     * controller (role-only, no tenant scoping), so a MANAGER/OWNER/
     * SERVICE_ADVISOR from any garage could generate an invoice or record
     * payment for any other garage's JobCard - confirmed live against
     * Postgres. Mirrors the garage-match check already used successfully
     * in RepairTaskServiceImpl and QualityCheckServiceImpl.
     */
    private void authorizeInvoiceAction(JobCard jobCard) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (jobCard.getGarage() == null
                || principal.getGarageId() == null
                || !principal.getGarageId().equals(jobCard.getGarage().getId())) {

            throw new BusinessException(
                    "This Job Card does not belong to your garage.");
        }
    }

}