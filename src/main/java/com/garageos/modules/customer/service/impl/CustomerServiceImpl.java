package com.garageos.modules.customer.service.impl;

import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.dto.request.CreateCustomerRequest;
import com.garageos.modules.customer.dto.response.CustomerResponse;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.mapper.CustomerMapper;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    @Override
    public CustomerResponse createCustomer(CreateCustomerRequest request) {

        if (repository.existsByMobileNumber(request.getMobileNumber())) {
            throw new BusinessException(
                    "Customer already exists with mobile number : "
                            + request.getMobileNumber());
        }

        Customer customer = mapper.toEntity(request);

        customer = repository.save(customer);

        return mapper.toResponse(customer);
    }



    @Override
    public CustomerResponse getCustomer(Long id) {

        Customer customer = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found with id : " + id));

        return mapper.toResponse(customer);
    }

    @Override
    public void deleteCustomer(Long id) {

        Customer customer = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found with id : " + id));

        repository.delete(customer);
    }

    @Override
    public CustomerResponse updateCustomer(Long id, CreateCustomerRequest request) {

        Customer customer = repository.findById(id)
                .orElseThrow(() ->
                        new BusinessException("Customer not found with id : " + id));

        if (!customer.getMobileNumber().equals(request.getMobileNumber())
                && repository.existsByMobileNumber(request.getMobileNumber())) {

            throw new BusinessException(
                    "Customer already exists with mobile number : "
                            + request.getMobileNumber());
        }

        mapper.updateEntity(request, customer);

        customer = repository.save(customer);

        return mapper.toResponse(customer);
    }

    @Override
    public CustomerResponse getCustomerByMobileNumber(String mobileNumber) {

        Customer customer = repository.findByMobileNumber(mobileNumber)
                .orElseThrow(() ->
                        new BusinessException(
                                "Customer not found with mobile number : " + mobileNumber));

        return mapper.toResponse(customer);
    }

    @Override
    public Page<CustomerResponse> getAllCustomers(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Customer> customers = repository.findAll(pageable);

        return customers.map(mapper::toResponse);
    }


}