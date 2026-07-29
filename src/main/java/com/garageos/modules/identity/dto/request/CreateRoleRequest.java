package com.garageos.modules.identity.dto.request;

import com.garageos.core.enums.identity.RoleCode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class CreateRoleRequest {

    @NotNull
    private RoleCode code;

    private String displayName;

    private String description;

    private Boolean systemRole;

    private Set<Long> permissionIds;

}