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

}