package com.garageos.modules.jobassignment.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteJobRequest {

    private Double actualHours;

    private String remarks;

}