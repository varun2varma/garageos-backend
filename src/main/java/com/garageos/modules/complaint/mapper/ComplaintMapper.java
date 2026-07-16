package com.garageos.modules.complaint.mapper;

import com.garageos.modules.complaint.dto.request.CreateComplaintRequest;
import com.garageos.modules.complaint.dto.response.ComplaintResponse;
import com.garageos.modules.complaint.entity.Complaint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ComplaintMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Complaint toEntity(CreateComplaintRequest request);

    @Mapping(source = "jobCard.id", target = "jobCardId")
    ComplaintResponse toResponse(Complaint complaint);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateComplaintRequest request,
                      @MappingTarget Complaint complaint);
}