package com.garageos.modules.jobassignment.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteJobRequest {

    /** Deprecated and ignored: actual duration is derived from the recorded start/completion timestamps. */
    @Deprecated
    private Double actualHours;

    private String remarks;

}