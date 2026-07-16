package com.garageos.modules.serviceworkflow.dto.request;

import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateJobWorkflowRequest {

    @Valid
    @NotNull
    private CreateJobCardRequest jobCard;

}