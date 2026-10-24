package com.garageos.modules.customer.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.customer.CustomerGarageSource;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.security.GarageScope;
import com.garageos.modules.customer.service.CustomerGarageService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final GarageScope garageScope;
    private final CustomerGarageService customerGarageService;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {

        GarageUserPrincipal principal = garageScope.principal();

        // Defence behind the controller's role guard: only a garage staff member (or the platform) registers customers.
        if (!garageScope.isUnrestricted(principal) && !garageScope.isStaff(principal)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only garage staff can register customers.");
        }

        Customer existing = repository.findByMobileNumber(request.getMobileNumber()).orElse(null);

        if (existing != null) {

            // Same-garage duplicate: unchanged behaviour.
            if (garageScope.isUnrestricted(principal)
                    || customerGarageService.isVisibleToAny(
                    existing.getId(), garageScope.allowedGarageIds(principal))) {

                throw new BusinessException(
                        "Customer already exists with mobile number : "
                                + request.getMobileNumber());
            }

            // The customer record is global (unique mobile) and may already
            // be used at another garage. Reuse it for this garage instead of
            // creating a duplicate; this only makes the customer visible to
            // this garage's staff - it is not a service relationship.
            if (principal.getGarageId() == null || !garageScope.isStaff(principal)) {
                throw new BusinessException(
                        "Customer already exists with mobile number : "
                                + request.getMobileNumber());
            }

            customerGarageService.link(existing.getId(), principal.getGarageId(),
                    CustomerGarageSource.STAFF_REGISTERED);

            // The record belongs to another garage's relationship until this link existed, so what comes back is
            // limited to what this caller just typed (plus the shared id): another garage's stored name, e-mail
            // and address for this number are not disclosed by "registering" it.
            Customer typed = mapper.toEntity(request);
            typed.setId(existing.getId());

            return mapper.toResponse(typed);
        }

        Customer customer = mapper.toEntity(request);

        customer = repository.save(customer);

        if (principal.getGarageId() != null && garageScope.isStaff(principal)) {
            customerGarageService.link(customer.getId(), principal.getGarageId(),
                    CustomerGarageSource.STAFF_REGISTERED);
        }

        return mapper.toResponse(customer);
    }

    /** Staff: only customers related to an accessible garage. Customer-only callers: only themselves. */
    private Customer requireAccessible(Long id) {

        GarageUserPrincipal principal = garageScope.principal();

        Customer customer = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found with id : " + id));

        if (garageScope.isUnrestricted(principal)) {
            return customer;
        }

        if (garageScope.isCustomerOnly(principal)) {
            if (principal.getMobile() != null
                    && principal.getMobile().equals(customer.getMobileNumber())) {
                return customer;
            }
            throw new ResourceNotFoundException("Customer not found with id : " + id);
        }

        if (!customerGarageService.isVisibleToAny(id, garageScope.allowedGarageIds(principal))) {
            throw new ResourceNotFoundException("Customer not found with id : " + id);
        }

        return customer;
    }

    private Customer requireStaffAccessible(Long id) {

        if (garageScope.isCustomerOnly(garageScope.principal())) {
            throw new ResourceNotFoundException("Customer not found with id : " + id);
        }

        return requireAccessible(id);
    }

    @Override
    public CustomerResponse getCustomer(Long id) {

        return mapper.toResponse(requireAccessible(id));
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {

        Customer customer = requireStaffAccessible(id);

        // The customer record is global; never delete one that other
        // garages also have a relationship with.
        GarageUserPrincipal principal = garageScope.principal();

        if (!garageScope.isUnrestricted(principal)
                && !otherGarages(id, principal).isEmpty()) {
            throw new BusinessException(
                    "This customer is also used by another garage and cannot be deleted.");
        }

        repository.delete(customer);
    }

    private Set<Long> otherGarages(Long customerId, GarageUserPrincipal principal) {

        Set<Long> mine = garageScope.allowedGarageIds(principal);

        return customerGarageService.garageIdsOf(customerId).stream()
                .filter(g -> !mine.contains(g))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public CustomerResponse updateCustomer(Long id, CreateCustomerRequest request) {

        Customer customer = requireStaffAccessible(id);

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

        GarageUserPrincipal principal = garageScope.principal();

        java.util.Optional<Customer> found;

        if (garageScope.isUnrestricted(principal)) {
            found = repository.findByMobileNumber(mobileNumber);
        } else if (garageScope.isCustomerOnly(principal)) {
            found = repository.findByMobileNumber(mobileNumber)
                    .filter(c -> c.getMobileNumber().equals(principal.getMobile()));
        } else {
            Set<Long> garageIds = garageScope.allowedGarageIds(principal);
            found = garageIds.isEmpty()
                    ? java.util.Optional.empty()
                    : repository.findVisibleByMobileNumber(mobileNumber, garageIds);
        }

        return mapper.toResponse(found.orElseThrow(() ->
                new BusinessException(
                        "Customer not found with mobile number : " + mobileNumber)));
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

        GarageUserPrincipal principal = garageScope.principal();

        Page<Customer> customers;

        if (garageScope.isUnrestricted(principal)) {
            customers = repository.findAll(pageable);
        } else {
            Set<Long> garageIds = garageScope.allowedGarageIds(principal);
            customers = garageIds.isEmpty() || garageScope.isCustomerOnly(principal)
                    ? Page.empty(pageable)
                    : repository.findVisibleToGarages(garageIds, pageable);
        }

        return customers.map(mapper::toResponse);
    }


    @Override
    @Transactional
    public CustomerResponse activateCustomer(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));

        /*
         * Employee may have already created this customer using
         * the same mobile number.
         *
         * In that case, reuse the existing Customer record instead
         * of creating another one or throwing "already activated".
         */
        Customer customer = repository
                .findByMobileNumber(user.getMobile())
                .orElseGet(() -> {

                    Customer newCustomer = new Customer();

                    newCustomer.setFirstName(user.getFirstName());
                    newCustomer.setLastName(user.getLastName());
                    newCustomer.setMobileNumber(user.getMobile());
                    newCustomer.setEmail(user.getEmail());

                    return repository.save(newCustomer);
                });

        /*
         * Registration/onboarding is now completed.
         */
        user.setFirstLogin(false);
        userRepository.save(user);

        /*
         * Make sure CUSTOMER role exists.
         * assignCustomerRole() is already idempotent.
         */
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