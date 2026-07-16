package com.garageos.modules.inspection.mapper;

import com.garageos.modules.inspection.dto.request.CreateInspectionRequest;
import com.garageos.modules.inspection.dto.response.InspectionResponse;
import com.garageos.modules.inspection.entity.Inspection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface InspectionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "complaint", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Inspection toEntity(CreateInspectionRequest request);

    @Mapping(source = "complaint.id", target = "complaintId")
    @Mapping(source = "complaint.complaint", target = "complaint")
    @Mapping(target = "status", expression = "java(inspection.getStatus().name())")
    InspectionResponse toResponse(Inspection inspection);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "complaint", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            CreateInspectionRequest request,
            @MappingTarget Inspection inspection);
}