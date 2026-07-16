package com.garageos.modules.estimate.mapper;

import com.garageos.modules.estimate.dto.request.CreateEstimateRequest;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimate.entity.Estimate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface EstimateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estimateNumber", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "discount", ignore = true)
    @Mapping(target = "gst", ignore = true)
    @Mapping(target = "grandTotal", ignore =true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Estimate toEntity(CreateEstimateRequest request);

    @Mapping(source = "jobCard.id", target = "jobCardId")
    @Mapping(target = "status", expression = "java(estimate.getStatus().name())")
    EstimateResponse toResponse(Estimate estimate);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estimateNumber", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "discount", ignore = true)
    @Mapping(target = "gst", ignore = true)
    @Mapping(target = "grandTotal", ignore =true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            CreateEstimateRequest request,
            @MappingTarget Estimate estimate);
}