package com.garageos.modules.handover.service;

import com.garageos.modules.handover.dto.response.HandoverCodeResponse;
import com.garageos.modules.handover.dto.response.HandoverStatusResponse;

public interface HandoverService {

    /** Customer only, for a trip they own. Returns the valid pending code, issuing one only if none is valid. */
    HandoverCodeResponse getOrCreateActiveCode(Long tripId);

    /** Customer only: explicitly replace the pending code; the previous one stops working immediately. */
    HandoverCodeResponse renewCode(Long tripId);

    /** Driver only, for a trip actually assigned to them. */
    HandoverStatusResponse verify(Long tripId, String code);

    /** Garage-scoped operational visibility - never exposes the code. */
    HandoverStatusResponse getStatus(Long tripId);

    /**
     * Mission Part L: customer-only, records that they reviewed the
     * pickup/delivery evidence for this trip before providing the
     * confirmation code. An auditable evidence-review event, not a legal
     * waiver - see CUSTOMER_VIEWED_EVIDENCE's own doc comment.
     */
    void recordEvidenceViewed(Long tripId);
}
