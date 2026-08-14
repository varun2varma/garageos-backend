package com.garageos.modules.jobassignment.dto.response;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.JobAssignmentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class JobAssignmentResponse {

    private Long id;

    private Long jobCardId;

    private String jobCardNumber;

    private Long estimateItemId;

    private String serviceName;

    private Long employeeId;

    private String employeeName;

    private JobAssignmentType assignmentType;

    private JobAssignmentStatus status;

    private Double estimatedHours;

    private Double actualHours;

    private String remarks;

    private LocalDateTime assignedAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}