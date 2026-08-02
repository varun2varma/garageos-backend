package com.garageos.modules.garagemembership.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApproveGarageMembershipRequest {

    @NotNull(message = "Role is required.")
    private List<Long> roleIds;

    private String employeeCode;

}