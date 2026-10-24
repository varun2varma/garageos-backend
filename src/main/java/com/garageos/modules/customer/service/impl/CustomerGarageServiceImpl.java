package com.garageos.modules.customer.service.impl;

import com.garageos.core.enums.customer.CustomerGarageSource;
import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.customer.dto.response.portal.MyGarageResponse;
import com.garageos.modules.customer.entity.CustomerGarage;
import com.garageos.modules.customer.repository.CustomerGarageRepository;
import com.garageos.modules.customer.service.CustomerGarageService;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerGarageServiceImpl implements CustomerGarageService {

    private final CustomerGarageRepository repository;
    private final GarageRepository garageRepository;

    /** Field-injected so existing positional construction is unchanged. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.modules.customer.repository.CustomerRepository customerRepository;

    /**
     * Customer-row lock taken before any check-then-write on that customer's garage relationships: two concurrent
     * "first garage" links, or two concurrent default changes, then queue instead of racing into the
     * one-default-per-customer unique index. The index stays as the last line of defence.
     */
    private void lockCustomer(Long customerId) {
        if (customerRepository != null) {
            customerRepository.findByIdForUpdate(customerId);
        }
    }

    @Override
    @Transactional
    public void link(Long customerId, Long garageId, CustomerGarageSource source) {

        if (customerId == null || garageId == null) {
            return;
        }

        lockCustomer(customerId);

        Optional<CustomerGarage> existing =
                repository.findByCustomerIdAndGarageId(customerId, garageId);

        if (existing.isPresent()) {

            CustomerGarage row = existing.get();

            // A job card / confirmed booking is stronger evidence than staff
            // registration: record it, but never create a second row.
            if (row.getSource() == CustomerGarageSource.STAFF_REGISTERED
                    && source != CustomerGarageSource.STAFF_REGISTERED) {
                row.setSource(source);
                repository.save(row);
            }
            return;
        }

        CustomerGarage row = new CustomerGarage();
        row.setCustomerId(customerId);
        row.setGarageId(garageId);
        row.setSource(source);
        // The customer's first garage becomes the default; later garages never take it over.
        row.setDefaultGarage(!repository.existsByCustomerIdAndDefaultGarageTrue(customerId));
        repository.save(row);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyGarageResponse> getMyGarages(Long customerId) {

        return repository
                .findByCustomerIdOrderByCreatedAtAsc(customerId)
                .stream()
                .map(row -> garageRepository.findById(row.getGarageId())
                        .map(garage -> toResponse(garage, row.isDefaultGarage()))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(MyGarageResponse::isDefaultGarage).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasServiceRelationship(Long customerId) {
        return repository.existsByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasServiceRelationship(Long customerId, Long garageId) {
        return repository.existsByCustomerIdAndGarageId(customerId, garageId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVisibleToAny(Long customerId, Collection<Long> garageIds) {

        if (customerId == null || garageIds == null || garageIds.isEmpty()) {
            return false;
        }

        return repository.existsByCustomerIdAndGarageIdIn(customerId, garageIds);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Set<Long> garageIdsOf(Long customerId) {
        return repository.findByCustomerId(customerId).stream()
                .map(CustomerGarage::getGarageId)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    @Transactional
    public List<MyGarageResponse> setDefaultGarage(Long customerId, Long garageId) {

        lockCustomer(customerId);

        if (!hasServiceRelationship(customerId, garageId)) {
            throw new BusinessException(
                    "You can only set a garage you have used as your default garage.");
        }

        repository.clearDefault(customerId);

        CustomerGarage row = repository.findByCustomerIdAndGarageId(customerId, garageId)
                .orElseThrow(() -> new BusinessException("Garage relationship not found."));

        row.setDefaultGarage(true);
        repository.saveAndFlush(row);

        return getMyGarages(customerId);
    }

    private MyGarageResponse toResponse(Garage garage, boolean isDefault) {

        return MyGarageResponse.builder()
                .garageId(garage.getId())
                .garageName(garage.getGarageName())
                .address(garage.getAddress())
                .city(garage.getCity())
                .state(garage.getState())
                .latitude(garage.getLatitude())
                .longitude(garage.getLongitude())
                .status(garage.getStatus() == null ? null : garage.getStatus().name())
                .bookable(garage.getStatus() == GarageStatus.ACTIVE)
                .unavailableReason(garage.getStatus() == GarageStatus.ACTIVE ? null : "GARAGE_INACTIVE")
                .defaultGarage(isDefault)
                .build();
    }
}
