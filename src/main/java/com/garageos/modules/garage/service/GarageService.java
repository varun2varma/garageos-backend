package com.garageos.modules.garage.service;

import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.request.UpdateGarageLocationRequest;
import com.garageos.modules.garage.dto.response.GarageResponse;
import com.garageos.modules.garage.dto.response.GarageSummaryResponse;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;

import java.util.List;

public interface GarageService {

    GarageResponse createGarage(
            Long userId,
            CreateGarageRequest request
    );

    /**
     * Full record (with PAN/GSTIN) for the garage's own staff/owner; a
     * summary without business identifiers for everyone else.
     */
    GarageSummaryResponse getGarage(GarageUserPrincipal principal, Long id);

    GarageResponse updateGarage(
            GarageUserPrincipal principal,
            Long id,
            CreateGarageRequest request
    );

    /**
     * Owner-only add/edit of just the map location, so an OWNER can set a
     * garage's coordinates after creation (or change them later) without
     * resending the full garage profile via updateGarage(). Ownership is
     * enforced against the authenticated principal, never trusted from the
     * request body.
     */
    GarageResponse updateGarageLocation(
            GarageUserPrincipal principal,
            Long id,
            UpdateGarageLocationRequest request
    );

    void deleteGarage(GarageUserPrincipal principal, Long id);

    /** Directory listing: summaries only, never PAN/GSTIN. */
    List<GarageSummaryResponse> getAllGarages();

}