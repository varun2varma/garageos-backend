package com.garageos.modules.identity.security.service;

import com.garageos.modules.identity.entity.Permission;
import com.garageos.modules.identity.entity.RolePermission;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GarageUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Invalid username or password"));

        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        Set<GrantedAuthority> authorities = new HashSet<>();

        for (UserRole userRole : user.getUserRoles()) {

            String role = userRole.getRole().getCode().name();

            roles.add(role);

            authorities.add(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

//            for (RolePermission rolePermission : userRole.getRole().getRolePermissions()) {
//
//                Permission permission = rolePermission.getPermission();
//
//                String permissionCode = permission.getCode().name();
//
//                permissions.add(permissionCode);
//
//                authorities.add(
//                        new SimpleGrantedAuthority(permissionCode)
//                );
//            }
        }

        return new GarageUserPrincipal(

                user.getId(),

                user.getGarageId(),

                user.getUsername(),

                user.getPasswordHash(),

                user.getFirstName(),

                user.getLastName(),

                user.getEmail(),

                user.getMobile(),

                user.getFirstLogin(),

                user.getStatus(),

                roles,

                permissions,

                authorities
        );
    }
}