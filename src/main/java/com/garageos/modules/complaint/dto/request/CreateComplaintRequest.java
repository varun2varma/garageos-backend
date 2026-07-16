package com.garageos.modules.complaint.dto.request;

import com.garageos.core.enums.ComplaintStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateComplaintRequest {

    @NotBlank(message = "Complaint is required.")
    private String complaint;

    private ComplaintStatus status;

}