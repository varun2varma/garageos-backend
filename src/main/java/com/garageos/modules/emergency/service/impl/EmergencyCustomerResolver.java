package com.garageos.modules.emergency.service.impl;

import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds the customer for a verified number or creates the minimum record (name and number only), in its own
 * transaction. It creates no login and grants no access to anything.
 */
@Component
public class EmergencyCustomerResolver {

    private final CustomerRepository customerRepository;

    public EmergencyCustomerResolver(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * May throw {@link org.springframework.dao.DataIntegrityViolationException} when a concurrent request creates the
     * same customer first (that aborts this transaction); the caller simply calls again in a fresh transaction, which
     * then finds the committed row.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long findOrCreate(String verifiedMobile, String contactName, String contactEmail) {

        return customerRepository.findByMobileNumber(verifiedMobile)
                .map(Customer::getId)
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.setFirstName(contactName);
                    c.setMobileNumber(verifiedMobile);
                    c.setEmail(contactEmail);
                    return customerRepository.saveAndFlush(c).getId();
                });
    }
}
