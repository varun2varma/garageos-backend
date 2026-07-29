package com.garageos.modules.identity.repository;

import com.garageos.core.enums.identity.PermissionCode;
import com.garageos.core.enums.identity.PermissionModule;
import com.garageos.modules.identity.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository
        extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(PermissionCode code);
    boolean existsByCode(PermissionCode code);
    List<Permission> findByModuleOrderByCode(
            PermissionModule module);

//    List<Permission> findByModuleOrderByCode(String module);

}
