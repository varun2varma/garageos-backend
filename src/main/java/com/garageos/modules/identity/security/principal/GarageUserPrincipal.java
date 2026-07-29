package com.garageos.modules.identity.security.principal;

import com.garageos.core.enums.identity.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@Getter
public class GarageUserPrincipal implements UserDetails {

    private final Long id;
    private final Long garageId;

    private final String username;
    private final String password;

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String mobile;

    private final Boolean firstLogin;

    private final UserStatus status;

    private Set<String> roles;

    private Set<String> permissions;

    private Collection<? extends GrantedAuthority> authorities;

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public GarageUserPrincipal(
            Long id,
            Long garageId,
            String username,
            String password,
            String firstName,
            String lastName,
            String email,
            String mobile,
            Boolean firstLogin,
            UserStatus status,
            Set<String> roles,
            Set<String> permissions,
            Collection<? extends GrantedAuthority> authorities) {

        this.id = id;
        this.garageId = garageId;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobile = mobile;
        this.firstLogin = firstLogin;
        this.status = status;
        this.roles = roles;
        this.permissions = permissions;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}