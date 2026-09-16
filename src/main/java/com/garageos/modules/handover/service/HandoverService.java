package com.garageos.modules.handover.service;

import com.garageos.modules.handover.dto.response.HandoverCodeResponse;
import com.garageos.modules.handover.dto.response.HandoverStatusResponse;

public interface HandoverService {

    /** Customer only, for a trip they own. Issues a fresh code every call. */
    HandoverCodeResponse getOrCreateActiveCode(Long tripId);

    /** Driver only, for a trip actually assigned to them. */
    HandoverStatusResponse verify(Long tripId, String code);

    /** Garage-scoped operational visibility - never exposes the code. */
    HandoverStatusResponse getStatus(Long tripId);
}
