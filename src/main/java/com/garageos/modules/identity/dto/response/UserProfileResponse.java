package com.garageos.modules.identity.dto.response;

import com.garageos.core.enums.identity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
@Builder
public class UserProfileResponse {

    private Long id;

    private Long garageId;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String mobile;

    private UserStatus status;

    private Boolean firstLogin;

    private Set<String> roles;

    private Set<String> permissions;

}