package com.garageos.modules.repairtask.dto.response;

import com.garageos.core.enums.RepairStatus;
import com.garageos.core.enums.RepairTaskPriority;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairTaskResponse {

    private Long id;

    private Long jobCardId;

    private String jobCardNumber;

    private Long complaintId;

    private String complaint;

    private Long estimateItemId;

    private String description;

    private Long jobAssignmentId;

    private RepairStatus status;

    private String technicianName;

    private RepairTaskPriority priority;

    private LocalDateTime assignedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String remarks;

}