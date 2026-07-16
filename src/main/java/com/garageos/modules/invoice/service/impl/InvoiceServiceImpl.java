package com.garageos.modules.invoice.service.impl;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.InvoiceNumberGenerator;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.invoice.dto.request.CreateInvoiceRequest;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.mapper.InvoiceMapper;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository repository;
    private final EstimateRepository estimateRepository;
    private final InvoiceMapper mapper;

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

        if (repository.existsByEstimateId(estimate.getId())) {
            throw new BusinessException(
                    "Invoice already exists for this estimate.");
        }

        Optional<Invoice> latestInvoice =
                repository.findTopByOrderByIdDesc();

        String invoiceNumber =
                InvoiceNumberGenerator.generate(
                        latestInvoice
                                .map(Invoice::getInvoiceNumber)
                                .orElse(null));

        Invoice invoice = mapper.toEntity(request);

        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setEstimate(estimate);

        invoice.setInvoiceStatus(InvoiceStatus.GENERATED);
        invoice.setPaymentStatus(PaymentStatus.PENDING);

        invoice.setSubtotal(estimate.getSubtotal());
        invoice.setDiscount(estimate.getDiscount());
        invoice.setGst(estimate.getGst());
        invoice.setGrandTotal(estimate.getGrandTotal());

        invoice = repository.save(invoice);

        return mapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoice(Long id) {

        Invoice invoice = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found with id : " + id));

        return mapper.toResponse(invoice);
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

        return repository.findAll(pageable)
                .map(mapper::toResponse);
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

        Invoice invoice = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found with id : " + id));

        repository.delete(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByInvoiceNumber(
            String invoiceNumber) {

        Invoice invoice = repository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found : " + invoiceNumber));

        return mapper.toResponse(invoice);
    }
}