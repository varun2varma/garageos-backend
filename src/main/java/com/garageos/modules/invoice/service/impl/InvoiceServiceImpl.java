package com.garageos.modules.invoice.service.impl;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.InvoiceNumberGenerator;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
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

        Optional<Invoice> latestInvoice =
                invoiceRepository.findTopByOrderByIdDesc();

        String invoiceNumber =
                InvoiceNumberGenerator.generate(
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

        JobCard jobCard = estimate.getJobCard();

        jobCard.setStatus(JobCardStatus.WORK_COMPLETED);

        jobCardRepository.save(jobCard);

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

        Optional<Invoice> latestInvoice =
                invoiceRepository.findTopByOrderByIdDesc();

        String invoiceNumber =
                InvoiceNumberGenerator.generate(
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

        Invoice invoice = new Invoice();

        invoice.setInvoiceNumber(generateInvoiceNumber());

        invoice.setEstimate(estimate);

//        invoice.setJobCard(estimate.getJobCard());

        invoice.setInvoiceStatus(InvoiceStatus.DRAFT);

        invoice.setPaymentStatus(PaymentStatus.PENDING);

        invoice.setSubtotal(estimate.getSubtotal());

        invoice.setDiscount(estimate.getDiscount());

        invoice.setGst(estimate.getGst());

        invoice.setGrandTotal(estimate.getGrandTotal());

        invoice.setRemarks(estimate.getRemarks());

        invoice = invoiceRepository.save(invoice);

        copyEstimateItems(invoice, estimate);

        JobCard jobCard = estimate.getJobCard();

        jobCard.setStatus(JobCardStatus.INVOICED);

        jobCardRepository.save(jobCard);

        return invoiceMapper.toResponse(invoice);

    }

    private String generateInvoiceNumber() {

        long count = invoiceRepository.count() + 1;

        return String.format(
                "INV-%d-%06d",
                java.time.Year.now().getValue(),
                count
        );
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

    @Override
    @Transactional
    public InvoiceResponse receivePayment(String jobCardNumber) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));

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

        return invoiceMapper.toResponse(invoice);
    }

}