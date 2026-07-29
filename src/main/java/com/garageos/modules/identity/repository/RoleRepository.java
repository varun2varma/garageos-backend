package com.garageos.modules.identity.repository;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.modules.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository
        extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(RoleCode code);
    boolean existsByCode(RoleCode code);

}