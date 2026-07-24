package com.garageos.modules.qualitycheck.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQualityCheckRequest {

    @NotBlank(message = "Inspector name is required.")
    private String inspectedBy;

    private String remarks;

}