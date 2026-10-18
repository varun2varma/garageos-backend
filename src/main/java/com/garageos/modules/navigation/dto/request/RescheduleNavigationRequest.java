package com.garageos.modules.navigation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RescheduleNavigationRequest {

    /** New wall-clock scheduled time for the pickup/delivery. */
    @NotNull
    private LocalDateTime scheduledAt;
}
