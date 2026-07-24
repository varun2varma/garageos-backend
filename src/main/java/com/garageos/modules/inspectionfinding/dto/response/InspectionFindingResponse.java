package com.garageos.modules.inspectionfinding.dto.response;

import com.garageos.core.enums.InspectionFindingStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionFindingResponse {

    private Long id;

    private Long jobCardId;

    private Long inspectionMasterItemId;

    private String category;

    private String checkItem;

    private InspectionFindingStatus status;

    private String remarks;

}