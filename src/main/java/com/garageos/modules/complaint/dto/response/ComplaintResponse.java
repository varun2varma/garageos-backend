package com.garageos.modules.complaint.dto.response;

import com.garageos.core.enums.ComplaintStatus;
import com.garageos.modules.complaint.dto.request.CreateComplaintRequest;
import com.garageos.modules.complaint.entity.Complaint;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintResponse {

    private Long id;
    private Long jobCardId;
    private String complaint;
    private ComplaintStatus status;
}