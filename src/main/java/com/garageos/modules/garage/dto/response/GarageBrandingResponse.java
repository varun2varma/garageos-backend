package com.garageos.modules.garage.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Everything a client or document needs to brand itself as ONE specific
 * garage, resolved by garageId (never from the logged-in user). A garage
 * without a logo reports {@code hasLogo=false}; callers then fall back to
 * GarageST default branding.
 */
@Getter
@Builder
public class GarageBrandingResponse {

    private Long garageId;

    private String garageCode;

    private String garageName;

    private String address;

    private String landmark;

    private String city;

    private String state;

    private String pincode;

    private String phone;

    private String email;

    private String gstNumber;

    private Boolean hasLogo;

    private LocalDateTime logoUpdatedAt;

    /** Relative, authenticated path serving the logo bytes; null when there is no logo. */
    private String logoUrl;
}
