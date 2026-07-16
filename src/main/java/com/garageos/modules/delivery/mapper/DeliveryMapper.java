package com.garageos.modules.delivery.mapper;

import com.garageos.modules.delivery.dto.request.CreateDeliveryRequest;
import com.garageos.modules.delivery.dto.response.DeliveryResponse;
import com.garageos.modules.delivery.entity.Delivery;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface DeliveryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "deliveryDateTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Delivery toEntity(CreateDeliveryRequest request);

    @Mapping(source = "jobCard.id", target = "jobCardId")
    @Mapping(source = "jobCard.jobCardNumber", target = "jobCardNumber")
    @Mapping(source = "invoice.id", target = "invoiceId")
    @Mapping(source = "invoice.invoiceNumber", target = "invoiceNumber")
    @Mapping(target = "status",
            expression = "java(delivery.getStatus().name())")
    DeliveryResponse toResponse(Delivery delivery);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "deliveryDateTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            CreateDeliveryRequest request,
            @MappingTarget Delivery delivery);

}