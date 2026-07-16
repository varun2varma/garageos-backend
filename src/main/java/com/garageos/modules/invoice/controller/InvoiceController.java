package com.garageos.modules.invoice.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.invoice.dto.request.CreateInvoiceRequest;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request) {

        return ApiResponseUtil.created(
                "Invoice created successfully.",
                service.createInvoice(request)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Invoice fetched successfully.",
                service.getInvoice(id)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByInvoiceNumber(
            @RequestParam String invoiceNumber) {

        return ApiResponseUtil.success(
                "Invoice fetched successfully.",
                service.getInvoiceByInvoiceNumber(invoiceNumber)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ApiResponseUtil.success(
                "Invoices fetched successfully.",
                service.getAllInvoices(page, size, sortBy, direction)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(
            @PathVariable Long id) {

        service.deleteInvoice(id);

        return ApiResponseUtil.success(
                "Invoice deleted successfully."
        );
    }
}