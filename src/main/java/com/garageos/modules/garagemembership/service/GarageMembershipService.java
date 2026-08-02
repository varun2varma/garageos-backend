package com.garageos.modules.garagemembership.service;

import com.garageos.modules.garagemembership.dto.request.ApproveGarageMembershipRequest;
import com.garageos.modules.garagemembership.dto.request.JoinGarageRequest;
import com.garageos.modules.garagemembership.dto.response.GarageMembershipResponse;

import java.util.List;

public interface GarageMembershipService {

    GarageMembershipResponse joinGarage(
            Long userId,
            JoinGarageRequest request
    );

    List<GarageMembershipResponse> getPendingMemberships(
            Long garageId
    );

    List<GarageMembershipResponse> getEmployees(
            Long garageId
    );

    GarageMembershipResponse approveMembership(
            Long membershipId,
            Long ownerId,
            ApproveGarageMembershipRequest request
    );

    GarageMembershipResponse rejectMembership(
            Long membershipId,
            String remarks
    );

    void removeMembership(
            Long membershipId
    );

}