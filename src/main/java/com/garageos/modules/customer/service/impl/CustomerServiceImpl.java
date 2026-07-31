package com.garageos.modules.customer.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.dto.request.CreateCustomerRequest;
import com.garageos.modules.customer.dto.response.CustomerResponse;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.mapper.CustomerMapper;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.customer.service.CustomerService;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

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


    @Override
    @Transactional
    public CustomerResponse activateCustomer(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));

        if (repository.existsByMobileNumber(user.getMobile())) {

            throw new BusinessException(
                    "Customer already activated.");
        }

        Customer customer = new Customer();

        customer.setFirstName(user.getFirstName());

        customer.setLastName(user.getLastName());

        customer.setMobileNumber(user.getMobile());

        customer.setEmail(user.getEmail());

        customer = repository.save(customer);

        user.setFirstLogin(false);

        userRepository.save(user);

        assignCustomerRole(user);

        return mapper.toResponse(customer);
    }

//    private void assignCustomerRole(User user) {
//
//        userRoleRepository.deleteByUserId(user.getId());
//
//        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException("CUSTOMER role not found."));
//
//        UserRole userRole = UserRole.builder()
//                .user(user)
//                .role(customerRole)
//                .build();
//
//        userRoleRepository.save(userRole);
//    }

    private void assignCustomerRole(User user) {

        boolean alreadyAssigned =
                userRoleRepository.existsByUserIdAndRoleCode(
                        user.getId(),
                        RoleCode.CUSTOMER
                );

        if (alreadyAssigned) {
            return;
        }

        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER)
                .orElseThrow(() ->
                        new ResourceNotFoundException("CUSTOMER role not found."));

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(customerRole)
                .build();

        userRoleRepository.save(userRole);
    }


}