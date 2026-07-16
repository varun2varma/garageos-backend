package com.garageos.modules.customer.service;

import com.garageos.modules.customer.dto.request.CreateCustomerRequest;
import com.garageos.modules.customer.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse getCustomer(Long id);

    void deleteCustomer(Long id);

    CustomerResponse updateCustomer(Long id, CreateCustomerRequest request);
    CustomerResponse getCustomerByMobileNumber(String mobileNumber);
    Page<CustomerResponse> getAllCustomers(
            int page,
            int size,
            String sortBy,
            String direction);}