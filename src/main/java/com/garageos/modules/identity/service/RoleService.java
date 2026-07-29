package com.garageos.modules.identity.service;

import com.garageos.modules.identity.dto.request.CreateRoleRequest;
import com.garageos.modules.identity.dto.request.UpdateRoleRequest;
import com.garageos.modules.identity.dto.response.RoleResponse;
import org.springframework.data.domain.Page;

public interface RoleService {

    RoleResponse createRole(CreateRoleRequest request);

    RoleResponse getRole(Long id);

    RoleResponse updateRole(
            Long id,
            UpdateRoleRequest request);

    void deleteRole(Long id);

    Page<RoleResponse> getAllRoles(
            int page,
            int size,
            String sortBy,
            String direction);

}