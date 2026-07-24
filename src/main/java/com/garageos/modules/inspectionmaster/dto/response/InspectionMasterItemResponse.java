package com.garageos.modules.inspectionmaster.dto.response;

import com.garageos.core.enums.InspectionPriority;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionMasterItemResponse {

    private Long id;

    private String category;

    private String checkItem;

    private String description;

    private InspectionPriority priority;

    private Double estimatedLabourHours;

    private Boolean mandatory;

    private Integer displayOrder;

}