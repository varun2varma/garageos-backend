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
import org.springframework.stereotype.Service;

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
    public List<CustomerResponse> getAllCustomers() {

        List<Customer> customers = repository.findAll();

        return mapper.toResponseList(customers);
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

}