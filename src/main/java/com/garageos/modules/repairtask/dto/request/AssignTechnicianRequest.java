package com.garageos.modules.repairtask.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignTechnicianRequest {

    @NotBlank
    private String technicianName;

    /**
     * Optional, additive field (corrective fix for audit Finding T1).
     * Every currently-known caller of PUT /repair-tasks/{id}/assign
     * (both legacy JS trees, and Flutter's WorkflowService.assignTechnician
     * — confirmed unused by any live screen) omits this field entirely,
     * so its addition changes nothing for them. When a caller does supply
     * it, the endpoint converges into the authoritative JobAssignment
     * model instead of only setting the technicianName display snapshot.
     */
    private Long employeeId;

}