package com.garageos.modules.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @NotBlank @Size(max = 512) String token,
        @Size(max = 20) String platform,
        @Size(max = 50) String appVersion) {
}
