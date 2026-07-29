package com.garageos.modules.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterResponse {

    private Long id;

    private String username;

    private String firstName;

    private String lastName;

}