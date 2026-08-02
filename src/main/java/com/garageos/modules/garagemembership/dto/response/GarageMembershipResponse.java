package com.garageos.modules.garagemembership.dto.response;

import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class GarageMembershipResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String mobile;

    private String email;

    private Long garageId;

    private String garageCode;

    private String garageName;

    private Long userId;

    private String employeeCode;

    private GarageMembershipStatus status;

    private LocalDateTime joinedAt;

    private LocalDateTime approvedAt;

    private List<String> roles;

}