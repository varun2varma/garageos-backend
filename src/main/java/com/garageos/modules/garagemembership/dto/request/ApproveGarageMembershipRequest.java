package com.garageos.modules.garagemembership.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApproveGarageMembershipRequest {

    @NotEmpty(message = "At least one role is required.")
    private List<Long> roleIds;

}