package com.garageos.modules.delivery.preference;

import com.garageos.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Server-side enforcement that staff only follow the path the customer chose. Used by manual collection, delivery
 * request creation and driver assignment, so a manager cannot pick a delivery path (or skip the customer choice) from
 * the app or by calling the API directly.
 */
@Component
@RequiredArgsConstructor
public class ReturnPreferenceGate {

    public static final String WAITING_MESSAGE =
            "Waiting for the customer to choose how to receive the vehicle (collection or driver delivery).";

    private final ReturnPreferenceRepository repository;

    /** @throws BusinessException when the customer has not chosen, or chose the other method */
    public void require(Long jobCardId, ReturnMethod expected) {

        ReturnMethod chosen = repository.findByJobCardId(jobCardId)
                .map(ReturnPreference::getMethod)
                .orElse(null);

        if (chosen == null) {
            throw new BusinessException(WAITING_MESSAGE, "WAITING_FOR_CUSTOMER_PREFERENCE");
        }

        if (chosen != expected) {
            throw new BusinessException(
                    expected == ReturnMethod.COLLECT_AT_GARAGE
                            ? "The customer chose driver delivery, so the vehicle cannot be recorded as collected at the garage."
                            : "The customer chose to collect the vehicle at the garage, so no driver delivery can be arranged.",
                    "RETURN_PREFERENCE_MISMATCH");
        }
    }
}
