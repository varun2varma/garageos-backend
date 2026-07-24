package com.garageos.modules.inspectionfinding.dto.request;

import com.garageos.core.enums.InspectionFindingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInspectionFindingRequest {

    @NotNull
    private Long inspectionMasterItemId;

    @NotNull
    private InspectionFindingStatus status;

    private String remarks;

}