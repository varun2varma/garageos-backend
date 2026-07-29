package com.garageos.modules.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserResponse {

    private Long id;

    private Long garageId;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String mobile;

    private String status;

    private Boolean firstLogin;

    private List<String> roles;

}