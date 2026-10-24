package com.garageos.modules.customer.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.customer.dto.request.CreateCustomerRequest;
import com.garageos.modules.customer.dto.response.CustomerResponse;
import com.garageos.modules.customer.service.CustomerService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    /** Garage staff who register customers (same roles that create job cards). Customers never create customers. */
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'SERVICE_ADVISOR')")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse response = service.createCustomer(request);

        return ApiResponseUtil.created(
                "Customer created successfully.",
                response
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Customer fetched successfully.",
                service.getCustomer(id)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @PathVariable Long id) {

        service.deleteCustomer(id);

        return ApiResponseUtil.success(
                "Customer deleted successfully."
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'SERVICE_ADVISOR')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CreateCustomerRequest request) {

        return ResponseEntity.ok(service.updateCustomer(id, request));
    }

    @GetMapping("/search")
    public ResponseEntity<CustomerResponse> getCustomerByMobileNumber(
            @RequestParam String mobileNumber) {

        return ResponseEntity.ok(service.getCustomerByMobileNumber(mobileNumber));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ApiResponseUtil.success(
                "Customers fetched successfully.",
                service.getAllCustomers(page, size, sortBy, direction)
        );
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<CustomerResponse>> activateCustomer(
            @AuthenticationPrincipal GarageUserPrincipal user) {

        return ApiResponseUtil.created(
                "Customer activated successfully.",
                service.activateCustomer(user.getId())
        );

    }


}