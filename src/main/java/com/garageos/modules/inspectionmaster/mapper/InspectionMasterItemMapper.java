package com.garageos.modules.inspectionmaster.mapper;

import com.garageos.modules.inspectionmaster.dto.request.CreateInspectionMasterItemRequest;
import com.garageos.modules.inspectionmaster.dto.response.InspectionMasterItemResponse;
import com.garageos.modules.inspectionmaster.entity.InspectionMasterItem;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface InspectionMasterItemMapper {

//    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inspectionMaster", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "serviceName", ignore = true)
    @Mapping(target = "serviceDescription", ignore = true)
    @Mapping(target = "labourCost", ignore = true)
    @Mapping(target = "partCost", ignore = true)
    InspectionMasterItem toEntity(CreateInspectionMasterItemRequest request);

    InspectionMasterItemResponse toResponse(InspectionMasterItem entity);

    List<InspectionMasterItemResponse> toResponseList(
            List<InspectionMasterItem> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inspectionMaster", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "serviceName", ignore = true)
    @Mapping(target = "serviceDescription", ignore = true)
    @Mapping(target = "labourCost", ignore = true)
    @Mapping(target = "partCost", ignore = true)
    void updateEntity(
            CreateInspectionMasterItemRequest request,
            @MappingTarget InspectionMasterItem entity);

}