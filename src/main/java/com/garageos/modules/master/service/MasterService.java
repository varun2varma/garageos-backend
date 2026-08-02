package com.garageos.modules.master.service;

import com.garageos.modules.master.dto.response.EmployeeRoleResponse;

import java.util.List;

public interface MasterService {

    List<EmployeeRoleResponse> getEmployeeRoles();

}