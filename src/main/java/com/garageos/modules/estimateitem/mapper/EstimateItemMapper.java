package com.garageos.modules.estimateitem.mapper;

import com.garageos.modules.estimateitem.dto.request.CreateEstimateItemRequest;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface EstimateItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estimate", ignore = true)
    @Mapping(target = "complaint", ignore = true)
    @Mapping(target = "itemType", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "inspectionFinding", ignore = true)
    // Defaults to true via the entity's own field initializer; changed
    // only via EstimateItemService.setItemSelection, never at creation.
    @Mapping(target = "selected", ignore = true)
    EstimateItem toEntity(CreateEstimateItemRequest request);

    @Mapping(source = "estimate.id", target = "estimateId")
    @Mapping(source = "complaint.id", target = "complaintId")
    @Mapping(target = "itemType",
            expression = "java(item.getItemType().name())")
    EstimateItemResponse toResponse(EstimateItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estimate", ignore = true)
    @Mapping(target = "complaint", ignore = true)
    @Mapping(target = "itemType", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "inspectionFinding", ignore = true)
    @Mapping(target = "selected", ignore = true)
    void updateEntity(
            CreateEstimateItemRequest request,
            @MappingTarget EstimateItem item);
}