package com.garageos.modules.customer.service;

import com.garageos.modules.customer.dto.response.portal.CustomerLiveVehicleJourneySummary;

import java.util.List;

/**
 * Derives the authenticated customer's currently-active vehicle journeys
 * by aggregating (never mutating) the existing Booking, Navigation and
 * JobCard modules. See {@link com.garageos.modules.customer.service.impl.CustomerVehicleJourneyServiceImpl}
 * for the full derivation rules.
 */
public interface CustomerVehicleJourneyService {

    /**
     * One entry per vehicle belonging to the caller that currently has a
     * relevant, non-terminal booking, navigation trip or job card.
     * Vehicles with nothing active are simply omitted - never returned
     * with a {@code NO_ACTIVE_JOURNEY} placeholder entry.
     */
    List<CustomerLiveVehicleJourneySummary> getActiveJourneys();
}
