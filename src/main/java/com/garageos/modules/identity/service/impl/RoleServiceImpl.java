package com.garageos.modules.identity.service.impl;

import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.identity.dto.request.CreateRoleRequest;
import com.garageos.modules.identity.dto.request.UpdateRoleRequest;
import com.garageos.modules.identity.dto.response.RoleResponse;
import com.garageos.modules.identity.entity.Permission;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.RolePermission;
import com.garageos.modules.identity.mapper.RoleMapper;
import com.garageos.modules.identity.repository.PermissionRepository;
import com.garageos.modules.identity.repository.RolePermissionRepository;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final RoleMapper mapper;


    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {

        if (roleRepository.existsByCode(request.getCode())) {
            throw new BusinessException(
                    "Role already exists : " + request.getCode());
        }

        Role role = mapper.toEntity(request);

        role = roleRepository.save(role);

        if (request.getPermissionIds() != null &&
                !request.getPermissionIds().isEmpty()) {

            List<Permission> permissions =
                    permissionRepository.findAllById(
                            request.getPermissionIds());

            if (permissions.size() != request.getPermissionIds().size()) {
                throw new BusinessException(
                        "One or more permissions are invalid.");
            }

            Role finalRole = role;
            List<RolePermission> rolePermissions =
                    permissions.stream()

                            .map(permission -> RolePermission.builder()

                                    .role(finalRole)

                                    .permission(permission)

                                    .build())

                            .toList();

            rolePermissionRepository.saveAll(rolePermissions);

            role.setRolePermissions(rolePermissions);
        }

        return buildResponse(role);
    }

    private RoleResponse buildResponse(Role role) {

        return RoleResponse.builder()

                .id(role.getId())

                .code(role.getCode().name())

                .displayName(role.getDisplayName())

                .description(role.getDescription())

                .systemRole(role.getSystemRole())

                .permissions(

                        role.getRolePermissions()

                                .stream()

                                .map(rolePermission ->

                                        rolePermission

                                                .getPermission()

                                                .getCode()

                                                .name())

                                .sorted()

                                .toList()

                )

                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRole(Long id) {

        Role role = roleRepository.findById(id)

                .orElseThrow(() ->

                        new ResourceNotFoundException(
                                "Role not found : " + id));

        return buildResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(
            Long id,
            UpdateRoleRequest request) {

        Role role = roleRepository.findById(id)

                .orElseThrow(() ->

                        new ResourceNotFoundException(
                                "Role not found : " + id));

        mapper.updateEntity(request, role);

        role.getRolePermissions().clear();

        if (request.getPermissionIds() != null) {

            List<Permission> permissions =
                    permissionRepository.findAllById(
                            request.getPermissionIds());

            if (permissions.size() != request.getPermissionIds().size()) {
                throw new BusinessException(
                        "One or more permissions are invalid.");
            }

            Role finalRole = role;
            List<RolePermission> rolePermissions =
                    permissions.stream()

                            .map(permission -> RolePermission.builder()

                                    .role(finalRole)

                                    .permission(permission)

                                    .build())

                            .toList();

            role.getRolePermissions().addAll(rolePermissions);
        }

        role = roleRepository.save(role);

        return buildResponse(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {

        Role role = roleRepository.findById(id)

                .orElseThrow(() ->

                        new ResourceNotFoundException(
                                "Role not found : " + id));

        if (Boolean.TRUE.equals(role.getSystemRole())) {

            throw new BusinessException(
                    "System roles cannot be deleted.");
        }

        roleRepository.delete(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> getAllRoles(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")

                ? Sort.by(sortBy).descending()

                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Role> roles = roleRepository.findAll(pageable);

        return roles.map(this::buildResponse);
    }
}
