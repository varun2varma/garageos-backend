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
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            CreateInvoiceItemRequest request,
            @MappingTarget InvoiceItem entity);

}