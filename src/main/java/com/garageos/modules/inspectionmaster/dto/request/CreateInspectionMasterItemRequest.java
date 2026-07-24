package com.garageos.modules.inspectionmaster.dto.request;

import com.garageos.core.enums.InspectionPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInspectionMasterItemRequest {

    @NotBlank
    private String category;

    @NotBlank
    private String checkItem;

    private String description;

    @NotNull
    private InspectionPriority priority;

    private Double estimatedLabourHours;

    @NotNull
    private Boolean mandatory;

    @NotNull
    private Integer displayOrder;

}