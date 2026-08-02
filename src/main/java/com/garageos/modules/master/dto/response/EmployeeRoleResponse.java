package com.garageos.modules.master.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmployeeRoleResponse {

    private Long id;

    private String code;

    private String displayName;

}