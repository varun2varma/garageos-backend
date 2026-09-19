package com.garageos.modules.repairtask.mapper;

import com.garageos.modules.repairtask.dto.request.CreateRepairTaskRequest;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.repairtask.entity.RepairTask;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface RepairTaskMapper {

//    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "estimateItem", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "technicianName", ignore = true)
    @Mapping(target = "jobAssignment", ignore = true)
    @Mapping(target = "complaint", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "remarks", ignore = true)
    RepairTask toEntity(CreateRepairTaskRequest request);

    @Mapping(target = "jobCardId", source = "jobCard.id")
    @Mapping(target = "jobCardNumber", source = "jobCard.jobCardNumber")
    @Mapping(target = "complaintId", source = "complaint.id")
    @Mapping(target = "complaint", source = "complaint.complaint")
    @Mapping(target = "estimateItemId", source = "estimateItem.id")
    @Mapping(target = "description", source = "complaint.complaint")
    @Mapping(target = "jobAssignmentId", source = "jobAssignment.id")
    RepairTaskResponse toResponse(RepairTask entity);

    List<RepairTaskResponse> toResponseList(
            List<RepairTask> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "estimateItem", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "technicianName", ignore = true)
    @Mapping(target = "jobAssignment", ignore = true)
    @Mapping(target = "complaint", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "remarks", ignore = true)
    void updateEntity(
            CreateRepairTaskRequest request,
            @MappingTarget RepairTask entity);

}