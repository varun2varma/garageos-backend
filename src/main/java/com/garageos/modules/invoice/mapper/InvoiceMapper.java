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
    void updateEntity(
            CreateInvoiceRequest request,
            @MappingTarget Invoice invoice);
}