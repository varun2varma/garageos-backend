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
    @Mapping(target = "serviceName", source = "estimateItem.description")

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

    @Mapping(
            target = "serviceName",
            source = "estimateItem.description"
    )

    MyAssignmentResponse toMyAssignment(
            JobAssignment jobAssignment
    );

    List<MyAssignmentResponse> toMyAssignment(
            List<JobAssignment> assignments
    );

}