package com.garageos.modules.identity.repository;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.modules.identity.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    /**
     * Pessimistically locks the user row. Used to serialise work that must be one-at-a-time per account, such as
     * registering a garage (so two identical simultaneous submissions cannot both pass the duplicate check).
     * Only call within an existing transaction.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);


    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByMobile(String mobile);

    /** Case-insensitive comparison only; stored emails are never rewritten. Used by the login identifier resolver. */
    List<User> findAllByEmailIgnoreCase(String email);

    List<User> findAllByMobileIn(Collection<String> mobiles);

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

    @EntityGraph(
            attributePaths = {
                    "userRoles",
                    "userRoles.role"
            }
    )
    @Query("""
        SELECT DISTINCT u
        FROM User u
        JOIN u.userRoles ur
        JOIN ur.role r
        WHERE u.garageId = :garageId
          AND r.code = :roleCode
          AND u.status = :status
        """)
    List<User> findByGarageIdAndRoleAndStatus(
            @Param("garageId") Long garageId,
            @Param("roleCode") RoleCode roleCode,
            @Param("status") UserStatus status
    );


    Page<User> findByGarageIdIn(Collection<Long> garageIds, Pageable pageable);

    List<User> findByGarageIdIn(Collection<Long> garageIds);
}
