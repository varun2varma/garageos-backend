package com.garageos.modules.garagemembership.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssignRoleRequest {

    @NotEmpty
    private List<Long> roleIds;

}