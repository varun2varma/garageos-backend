package com.garageos.modules.handover.dto.response;

import com.garageos.core.enums.navigation.TripType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Returned only to the owning customer - the one place the plaintext code
 * ever appears. It is never persisted; asking again before it expires
 * issues (and returns) a fresh code, invalidating the previous one.
 */
@Getter
@Builder
public class HandoverCodeResponse {

    private Long tripId;
    private TripType direction;
    private String code;
    private LocalDateTime expiresAt;
}
