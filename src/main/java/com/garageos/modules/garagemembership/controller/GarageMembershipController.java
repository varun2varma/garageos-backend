package com.garageos.modules.garagemembership.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.garagemembership.dto.request.ApproveGarageMembershipRequest;
import com.garageos.modules.garagemembership.dto.request.JoinGarageRequest;
import com.garageos.modules.garagemembership.dto.response.GarageMembershipResponse;
import com.garageos.modules.garagemembership.service.GarageMembershipService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/garage-memberships")
@RequiredArgsConstructor
public class GarageMembershipController {

    private final GarageMembershipService service;

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<GarageMembershipResponse>> joinGarage(
            @AuthenticationPrincipal GarageUserPrincipal principal,
            @Valid @RequestBody JoinGarageRequest request) {

        return ApiResponseUtil.success(
                "Garage join request submitted successfully.",
                service.joinGarage(
                        principal.getId(),
                        request
                )
        );
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<GarageMembershipResponse>>> getPendingMemberships(
            @AuthenticationPrincipal GarageUserPrincipal principal) {

        return ApiResponseUtil.success(
                "Pending memberships fetched successfully.",
                service.getPendingMemberships(
                        principal.getGarageId()
                )
        );
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<GarageMembershipResponse>>>
    getEmployees(
            @AuthenticationPrincipal
            GarageUserPrincipal principal) {

        return ApiResponseUtil.success(
                "Employees fetched successfully.",
                service.getEmployees(
                        principal.getGarageId()
                )
        );
    }

    @PutMapping("/{membershipId}/approve")
    public ResponseEntity<ApiResponse<GarageMembershipResponse>> approveMembership(
            @AuthenticationPrincipal GarageUserPrincipal principal,
            @PathVariable Long membershipId,
            @Valid @RequestBody ApproveGarageMembershipRequest request) {

        return ApiResponseUtil.success(
                "Employee approved successfully.",
                service.approveMembership(
                        membershipId,
                        principal.getId(),
                        request
                )
        );
    }

    @PutMapping("/{membershipId}/reject")
    public ResponseEntity<ApiResponse<GarageMembershipResponse>> rejectMembership(
            @PathVariable Long membershipId,
            @RequestParam String remarks) {

        return ApiResponseUtil.success(
                "Employee rejected successfully.",
                service.rejectMembership(
                        membershipId,
                        remarks
                )
        );
    }

    @DeleteMapping("/{membershipId}")
    public ResponseEntity<ApiResponse<Void>> removeMembership(
            @PathVariable Long membershipId) {

        service.removeMembership(membershipId);

        return ApiResponseUtil.success(
                "Employee removed successfully.",
                null
        );
    }

}