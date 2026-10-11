package com.garageos.modules.identity.repository;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.modules.identity.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository
        extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    /** True when any account already holds the role (used to make the platform-admin bootstrap a one-time grant). */
    boolean existsByRoleCode(RoleCode roleCode);

    boolean existsByUserIdAndRoleCode(
            Long userId,
            RoleCode roleCode
    );

}