package com.garageos.modules.inspection.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InspectionResponse {

    private Long id;

    private Long complaintId;

    private String complaint;

    private String inspectionNotes;

    private String recommendedWork;

    private String status;

}