package com.garageos.modules.identity.repository;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.modules.identity.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository
        extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    boolean existsByUserIdAndRoleCode(
            Long userId,
            RoleCode roleCode
    );

}