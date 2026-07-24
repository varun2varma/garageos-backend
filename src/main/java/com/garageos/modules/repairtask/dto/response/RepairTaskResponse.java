package com.garageos.modules.repairtask.dto.response;

import com.garageos.core.enums.RepairStatus;
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

    private Long estimateItemId;

    private String description;

    private RepairStatus status;

    private String technicianName;

    private LocalDateTime assignedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String remarks;

}