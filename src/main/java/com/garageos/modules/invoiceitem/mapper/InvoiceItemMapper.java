package com.garageos.modules.invoiceitem.mapper;

import com.garageos.modules.invoiceitem.dto.request.CreateInvoiceItemRequest;
import com.garageos.modules.invoiceitem.dto.response.InvoiceItemResponse;
import com.garageos.modules.invoiceitem.entity.InvoiceItem;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface InvoiceItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "complaint", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "catalogItemId", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "hsnSacCode", ignore = true)
    @Mapping(target = "taxRuleCode", ignore = true)
    @Mapping(target = "priceTaxMode", ignore = true)
    @Mapping(target = "taxSource", ignore = true)
    @Mapping(target = "gstRatePercent", ignore = true)
    @Mapping(target = "taxableValue", ignore = true)
    @Mapping(target = "cgstAmount", ignore = true)
    @Mapping(target = "sgstAmount", ignore = true)
    @Mapping(target = "igstAmount", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "lineTotal", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InvoiceItem toEntity(CreateInvoiceItemRequest request);

    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "complaintId", source = "complaint.id")
    @Mapping(target = "complaintTitle", ignore = true)
    InvoiceItemResponse toResponse(InvoiceItem entity);

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "complaint", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "catalogItemId", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "hsnSacCode", ignore = true)
    @Mapping(target = "taxRuleCode", ignore = true)
    @Mapping(target = "priceTaxMode", ignore = true)
    @Mapping(target = "taxSource", ignore = true)
    @Mapping(target = "gstRatePercent", ignore = true)
    @Mapping(target = "taxableValue", ignore = true)
    @Mapping(target = "cgstAmount", ignore = true)
    @Mapping(target = "sgstAmount", ignore = true)
    @Mapping(target = "igstAmount", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "lineTotal", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            CreateInvoiceItemRequest request,
            @MappingTarget InvoiceItem entity);

}