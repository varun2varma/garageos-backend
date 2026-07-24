package com.garageos.modules.inspectionmaster.mapper;

import com.garageos.modules.inspectionmaster.dto.request.CreateInspectionMasterRequest;
import com.garageos.modules.inspectionmaster.dto.response.InspectionMasterResponse;
import com.garageos.modules.inspectionmaster.entity.InspectionMaster;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {
                InspectionMasterItemMapper.class
        },
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface InspectionMasterMapper {

//    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    InspectionMaster toEntity(CreateInspectionMasterRequest request);

    InspectionMasterResponse toResponse(InspectionMaster entity);

    List<InspectionMasterResponse> toResponseList(
            List<InspectionMaster> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(
            CreateInspectionMasterRequest request,
            @MappingTarget InspectionMaster entity);

}