package com.garageos.modules.serviceworkflow.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class WorkflowResponse {

    private Object data;

    private String message;

}