package com.garageos.modules.customer.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.customer.dto.response.portal.*;
import com.garageos.modules.customer.service.CustomerPortalService;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.vehicle.dto.response.VehicleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerPortalController {

    private final CustomerPortalService service;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile() {

        return ApiResponseUtil.success(
                "Customer profile fetched successfully.",
                service.getProfile()
        );
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<CustomerDashboardResponse>> getDashboard() {

        return ApiResponseUtil.success(
                "Customer dashboard fetched successfully.",
                service.getDashboard()
        );
    }

    @GetMapping("/vehicles")
    public ResponseEntity<ApiResponse<List<CustomerVehicleResponse>>> getVehicles() {

        return ApiResponseUtil.success(
                "Vehicles fetched successfully.",
                service.getVehicles()
        );
    }

    @GetMapping("/jobcards")
    public ResponseEntity<ApiResponse<List<CustomerJobCardResponse>>> getJobCards() {

        return ApiResponseUtil.success(
                "Job cards fetched successfully.",
                service.getJobCards()
        );
    }

    @GetMapping("/estimates")
    public ResponseEntity<ApiResponse<List<CustomerEstimateResponse>>> getEstimates() {

        return ApiResponseUtil.success(
                "Estimates fetched successfully.",
                service.getEstimates()
        );
    }

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<CustomerInvoiceResponse>>> getInvoices() {

        return ApiResponseUtil.success(
                "Invoices fetched successfully.",
                service.getInvoices()
        );
    }

    @GetMapping("/repair-tracking/{jobCardNumber}")
    public ResponseEntity<ApiResponse<CustomerRepairTrackingResponse>> trackRepair(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Repair status fetched successfully.",
                service.trackRepair(jobCardNumber)
        );

    }

}