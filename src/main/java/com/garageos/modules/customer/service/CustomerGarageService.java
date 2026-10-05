package com.garageos.modules.customer.service;

import com.garageos.core.enums.customer.CustomerGarageSource;
import com.garageos.modules.customer.dto.response.portal.MyGarageResponse;

import java.util.Collection;
import java.util.List;

/** Customer <-> Garage relationship metadata (V66). */
public interface CustomerGarageService {

    /**
     * Idempotent: records the relationship if absent; an existing
     * STAFF_REGISTERED row is upgraded when a stronger source arrives.
     * Never creates a duplicate row.
     */
    void link(Long customerId, Long garageId, CustomerGarageSource source);

    /** Garages with an established service relationship (excludes STAFF_REGISTERED), default first. */
    List<MyGarageResponse> getMyGarages(Long customerId);

    boolean hasServiceRelationship(Long customerId);

    boolean hasServiceRelationship(Long customerId, Long garageId);

    /** Whether the customer is visible to staff of any of these garages. */
    boolean isVisibleToAny(Long customerId, Collection<Long> garageIds);

    /** Every garage this customer is related to (any source). */
    java.util.Set<Long> garageIdsOf(Long customerId);

    /** Only one default per customer; requires an established service relationship. */
    List<MyGarageResponse> setDefaultGarage(Long customerId, Long garageId);
}
