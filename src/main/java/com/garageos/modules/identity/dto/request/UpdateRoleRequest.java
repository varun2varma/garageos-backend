package com.garageos.modules.identity.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateRoleRequest {

    private String displayName;

    private String description;

    private Set<Long> permissionIds;

}