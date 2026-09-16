package com.garageos.modules.handover.dto.response;

import com.garageos.core.enums.navigation.HandoverStatus;
import com.garageos.core.enums.navigation.TripType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Operational visibility (Manager/Advisor/Owner) - never carries the code or its hash. */
@Getter
@Builder
public class HandoverStatusResponse {

    private Long tripId;
    private TripType direction;
    private HandoverStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
}
