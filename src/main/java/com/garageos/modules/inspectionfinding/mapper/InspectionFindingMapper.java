package com.garageos.modules.inspectionfinding.mapper;

import com.garageos.modules.inspectionfinding.dto.request.CreateInspectionFindingRequest;
import com.garageos.modules.inspectionfinding.dto.response.InspectionFindingResponse;
import com.garageos.modules.inspectionfinding.entity.InspectionFinding;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface InspectionFindingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "inspectionMasterItem", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "complaint", ignore = true)
    InspectionFinding toEntity(CreateInspectionFindingRequest request);

    @Mapping(target = "jobCardId", source = "jobCard.id")
    @Mapping(target = "inspectionMasterItemId", source = "inspectionMasterItem.id")
    @Mapping(target = "category", source = "inspectionMasterItem.category")
    @Mapping(target = "checkItem", source = "inspectionMasterItem.checkItem")
    InspectionFindingResponse toResponse(InspectionFinding entity);

    List<InspectionFindingResponse> toResponseList(List<InspectionFinding> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "inspectionMasterItem", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "complaint", ignore = true)
    void updateEntity(
            CreateInspectionFindingRequest request,
            @MappingTarget InspectionFinding entity
    );
}