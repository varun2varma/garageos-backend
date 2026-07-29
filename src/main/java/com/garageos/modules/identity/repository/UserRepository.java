package com.garageos.modules.identity.repository;

import com.garageos.core.enums.identity.UserStatus;
import com.garageos.modules.identity.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByMobile(String mobile);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    long countByStatus(UserStatus status);

    /*
    ,
                    "userRoles.role.rolePermissions",
                    "userRoles.role.rolePermissions.permission"
     */
    @EntityGraph(
            attributePaths = {
                    "userRoles",
                    "userRoles.role"
            }
    )
    Optional<User> findWithRolesByUsername(
            String username);

}