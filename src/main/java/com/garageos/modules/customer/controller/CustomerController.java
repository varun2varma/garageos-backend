package com.garageos.modules.customer.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.customer.dto.request.CreateCustomerRequest;
import com.garageos.modules.customer.dto.response.CustomerResponse;
import com.garageos.modules.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse response = service.createCustomer(request);

        return ApiResponseUtil.created(
                "Customer created successfully.",
                response
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {

        return ApiResponseUtil.success(
                "Customers fetched successfully.",
                service.getAllCustomers()
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
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @PathVariable Long id) {

        service.deleteCustomer(id);

        return ApiResponseUtil.success(
                "Customer deleted successfully."
        );
    }

}