package com.garageos.modules.master.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.master.dto.response.EmployeeRoleResponse;
import com.garageos.modules.master.service.MasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MasterServiceImpl
        implements MasterService {

    private final RoleRepository roleRepository;

    private static final Set<RoleCode> EMPLOYEE_ROLES = Set.of(

            RoleCode.MANAGER,

            RoleCode.SERVICE_ADVISOR,

            RoleCode.TECHNICIAN,

            RoleCode.DRIVER,

            RoleCode.INVENTORY_MANAGER,

            RoleCode.ACCOUNTANT,

            RoleCode.CASHIER

    );

    @Override
    public List<EmployeeRoleResponse> getEmployeeRoles() {

        return roleRepository

                .findAll()

                .stream()

                .filter(role ->

                        EMPLOYEE_ROLES.contains(

                                role.getCode()

                        )

                )

                .map(this::map)

                .sorted(

                        (a, b) ->

                                a.getDisplayName()

                                        .compareTo(

                                                b.getDisplayName()

                                        )

                )

                .toList();

    }

    private EmployeeRoleResponse map(Role role) {

        return EmployeeRoleResponse

                .builder()

                .id(role.getId())

                .code(role.getCode().name())

                .displayName(role.getDisplayName())

                .build();

    }

}