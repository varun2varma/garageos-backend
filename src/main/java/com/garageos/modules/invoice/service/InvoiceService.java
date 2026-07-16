package com.garageos.modules.invoice.service;

import com.garageos.modules.invoice.dto.request.CreateInvoiceRequest;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import org.springframework.data.domain.Page;

public interface InvoiceService {

    InvoiceResponse createInvoice(CreateInvoiceRequest request);

    InvoiceResponse getInvoice(Long id);

    Page<InvoiceResponse> getAllInvoices(
            int page,
            int size,
            String sortBy,
            String direction);

//    InvoiceResponse updateInvoice(
//            Long id,
//            CreateInvoiceRequest request);

    void deleteInvoice(Long id);

    InvoiceResponse getInvoiceByInvoiceNumber(
            String invoiceNumber);
}