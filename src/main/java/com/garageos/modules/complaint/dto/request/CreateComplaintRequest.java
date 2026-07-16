package com.garageos.modules.complaint.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateComplaintRequest {

    @NotBlank(message = "Complaint is required.")
    private String complaint;
}