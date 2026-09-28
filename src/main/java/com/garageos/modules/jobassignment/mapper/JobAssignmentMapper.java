package com.garageos.modules.jobassignment.mapper;

import com.garageos.modules.jobassignment.dto.response.JobAssignmentResponse;
import com.garageos.modules.jobassignment.dto.response.MyAssignmentResponse;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JobAssignmentMapper {

    @Mapping(target = "jobCardId", source = "jobCard.id")
    @Mapping(target = "jobCardNumber", source = "jobCard.jobCardNumber")
    @Mapping(target = "estimateItemId", source = "estimateItem.id")
    @Mapping(target = "repairTaskId", source = "repairTask.id")
    // Root-cause fix: RepairTask is per-Complaint (one task can cover
    // several PART/LABOUR items), so the linked estimateItem is only ever
    // a single "representative" line item kept for backward traceability
    // (see RepairTaskServiceImpl.createRepairTasks) and is frequently null
    // on the JobAssignment itself - sourcing serviceName from it produced
    // a null name, which the UI then displayed as a generic "Repair task"
    // placeholder. The Complaint's own text is what every task under it
    // actually represents.
    @Mapping(target = "serviceName", source = "repairTask.complaint.complaint")
    @Mapping(target = "employeeId", source = "user.id")

    @Mapping(
            target = "employeeName",
            expression =
                    "java(jobAssignment.getUser().getFirstName() + " +
                            "(jobAssignment.getUser().getLastName() != null ? " +
                            "\" \" + jobAssignment.getUser().getLastName() : \"\"))"
    )

    JobAssignmentResponse toResponse(JobAssignment jobAssignment);

    List<JobAssignmentResponse> toResponse(
            List<JobAssignment> assignments
    );

    @Mapping(target = "assignmentId", source = "id")
    @Mapping(target = "repairTaskId", source = "repairTask.id")
    @Mapping(target = "jobCardNumber",
            source = "jobCard.jobCardNumber")

    @Mapping(
            target = "customerName",
            expression =
                    "java(jobAssignment.getJobCard().getCustomer().getFirstName() + " +
                            "(jobAssignment.getJobCard().getCustomer().getLastName() != null ? " +
                            "\" \" + jobAssignment.getJobCard().getCustomer().getLastName() : \"\"))"
    )

    @Mapping(
            target = "vehicleName",
            expression =
                    "java(jobAssignment.getJobCard().getVehicle().getBrand() + \" \" + " +
                            "jobAssignment.getJobCard().getVehicle().getModel() + \" \" + " +
                            "jobAssignment.getJobCard().getVehicle().getVariant())"
    )

    @Mapping(
            target = "registrationNumber",
            source = "jobCard.vehicle.registrationNumber"
    )

    // See toResponse()'s serviceName mapping above for why this sources
    // from the Complaint rather than the (often-null) representative item.
    @Mapping(
            target = "serviceName",
            source = "repairTask.complaint.complaint"
    )

    /**
     * Root-cause fix: this field previously had no source mapped at all,
     * so it was always null at runtime (see MyAssignmentResponse's own
     * field) — Mission backlog #20's "technician receives repair tasks
     * with assigned priority" was silently unimplementable until
     * RepairTask actually had a priority field (V49) to map from.
     */
    @Mapping(
            target = "priority",
            source = "repairTask.priority"
    )

    MyAssignmentResponse toMyAssignment(
            JobAssignment jobAssignment
    );

    List<MyAssignmentResponse> toMyAssignment(
            List<JobAssignment> assignments
    );

}