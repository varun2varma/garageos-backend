package com.garageos.modules.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RoleResponse {

    private Long id;

    private String code;

    private String displayName;

    private String description;

    private Boolean systemRole;

    private List<String> permissions;

}