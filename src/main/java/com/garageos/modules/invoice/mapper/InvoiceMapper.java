package com.garageos.modules.invoice.mapper;

import com.garageos.modules.invoice.dto.request.CreateInvoiceRequest;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.invoice.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface InvoiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "estimate", ignore = true)
    @Mapping(target = "invoiceStatus", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "discount", ignore = true)
    @Mapping(target = "gst", ignore = true)
    @Mapping(target = "grandTotal", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "generatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "taxSnapshot", ignore = true)
    @Mapping(target = "taxTotal", ignore = true)
    @Mapping(target = "cgstTotal", ignore = true)
    @Mapping(target = "sgstTotal", ignore = true)
    @Mapping(target = "igstTotal", ignore = true)
    @Mapping(target = "roundOff", ignore = true)
    @Mapping(target = "supplyType", ignore = true)
    @Mapping(target = "supplierGstin", ignore = true)
    @Mapping(target = "supplierState", ignore = true)
    @Mapping(target = "placeOfSupplyState", ignore = true)
    Invoice toEntity(CreateInvoiceRequest request);

    @Mapping(source = "estimate.id", target = "estimateId")
    @Mapping(target = "invoiceStatus",
            expression = "java(invoice.getInvoiceStatus().name())")
    @Mapping(target = "paymentStatus",
            expression = "java(invoice.getPaymentStatus().name())")
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "estimate", ignore = true)
    @Mapping(target = "invoiceStatus", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "discount", ignore = true)
    @Mapping(target = "gst", ignore = true)
    @Mapping(target = "grandTotal", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "generatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "taxSnapshot", ignore = true)
    @Mapping(target = "taxTotal", ignore = true)
    @Mapping(target = "cgstTotal", ignore = true)
    @Mapping(target = "sgstTotal", ignore = true)
    @Mapping(target = "igstTotal", ignore = true)
    @Mapping(target = "roundOff", ignore = true)
    @Mapping(target = "supplyType", ignore = true)
    @Mapping(target = "supplierGstin", ignore = true)
    @Mapping(target = "supplierState", ignore = true)
    @Mapping(target = "placeOfSupplyState", ignore = true)
    void updateEntity(
            CreateInvoiceRequest request,
            @MappingTarget Invoice invoice);
}