package com.garageos.modules.customer.dto.response.portal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerProfileResponse {

    private Long id;

    private String name;

    private String mobileNumber;

    private String email;

    private String address;

}