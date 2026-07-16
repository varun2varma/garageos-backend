package com.garageos.modules.complaint.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComplaintResponse {

    private Long id;

    private Long jobCardId;

    private String complaint;

    private String status;
}