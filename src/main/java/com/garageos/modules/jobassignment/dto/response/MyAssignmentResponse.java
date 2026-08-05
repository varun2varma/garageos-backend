package com.garageos.modules.jobassignment.dto.response;

import com.garageos.core.enums.JobAssignmentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyAssignmentResponse {

    private Long assignmentId;

    private String jobCardNumber;

    private String customerName;

    private String vehicleName;

    private String registrationNumber;

    private String serviceName;

    private String priority;

    private JobAssignmentStatus status;

    private Double estimatedHours;

}