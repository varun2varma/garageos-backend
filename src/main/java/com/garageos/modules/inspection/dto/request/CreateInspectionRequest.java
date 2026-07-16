package com.garageos.modules.inspection.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInspectionRequest {

    @NotBlank(message = "Inspection notes are required.")
    private String inspectionNotes;

    @NotBlank(message = "Recommended work is required.")
    private String recommendedWork;

}