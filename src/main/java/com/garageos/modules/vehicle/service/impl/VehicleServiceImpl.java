package com.garageos.modules.vehicle.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.vehicle.RcVerificationStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.vehicle.dto.request.CreateVehicleRequest;
import com.garageos.modules.vehicle.dto.response.VehicleResponse;
import com.garageos.modules.vehicle.entity.Vehicle;
import com.garageos.modules.vehicle.mapper.VehicleMapper;
import com.garageos.modules.vehicle.repository.VehicleRepository;
import com.garageos.modules.vehicle.service.VehicleService;
import com.garageos.modules.vehicle.validator.VehicleRegistrationNumberValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository repository;
    private final CustomerRepository customerRepository;
    private final VehicleMapper mapper;

    @Override
    public VehicleResponse createVehicle(CreateVehicleRequest request) {

        request.setRegistrationNumber(
                VehicleRegistrationNumberValidator.normalizeAndValidate(request.getRegistrationNumber()));

        if (repository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new BusinessException(
                    "Vehicle already exists with registration number : "
                            + request.getRegistrationNumber());
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found with id : " + request.getCustomerId()));

        Vehicle vehicle = mapper.toEntity(request);

        vehicle.setCustomer(customer);

        vehicle = repository.save(vehicle);

        return mapper.toResponse(vehicle);
    }

    @Override
    public VehicleResponse getVehicle(Long id) {

        Vehicle vehicle = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : " + id));

        return mapper.toResponse(vehicle);
    }

    @Override
    public void deleteVehicle(Long id) {

        Vehicle vehicle = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : " + id));

        repository.delete(vehicle);
    }

    @Override
    public VehicleResponse updateVehicle(Long id,
                                         CreateVehicleRequest request) {

        request.setRegistrationNumber(
                VehicleRegistrationNumberValidator.normalizeAndValidate(request.getRegistrationNumber()));

        Vehicle vehicle = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : " + id));

        // A customer may only edit their own vehicle, and may not use this
        // call to reassign it to a different customer. Employee roles keep
        // the existing unrestricted behavior (garage-side vehicle CRUD).
        GarageUserPrincipal principal = (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal.getRoles().contains(RoleCode.CUSTOMER.name())) {

            Customer caller = customerRepository.findByMobileNumber(principal.getMobile())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));

            boolean ownsVehicle = vehicle.getCustomer() != null
                    && vehicle.getCustomer().getId().equals(caller.getId());

            if (!ownsVehicle || !caller.getId().equals(request.getCustomerId())) {
                throw new AccessDeniedException(
                        "You may only edit your own vehicle.");
            }
        }

        if (!vehicle.getRegistrationNumber().equals(request.getRegistrationNumber())
                && repository.existsByRegistrationNumber(request.getRegistrationNumber())) {

            throw new BusinessException(
                    "Vehicle already exists with registration number : "
                            + request.getRegistrationNumber());
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found with id : " + request.getCustomerId()));

        mapper.updateEntity(request, vehicle);

        vehicle.setCustomer(customer);

        vehicle = repository.save(vehicle);

        return mapper.toResponse(vehicle);
    }

    @Override
    public VehicleResponse getVehicleByRegistrationNumber(
            String registrationNumber) {

        Vehicle vehicle = repository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with registration number : "
                                        + registrationNumber));

        return mapper.toResponse(vehicle);
    }

    @Override
    public VehicleResponse setRcVerification(
            Long id,
            RcVerificationStatus status,
            String documentReference) {

        Vehicle vehicle = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : " + id));

        GarageUserPrincipal principal = (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        vehicle.setRcVerificationStatus(status);

        if (documentReference != null) {
            vehicle.setRcDocumentReference(documentReference);
        }

        if (status == RcVerificationStatus.VERIFIED || status == RcVerificationStatus.REJECTED) {
            vehicle.setRcVerifiedBy(principal.getId());
            vehicle.setRcVerifiedAt(java.time.LocalDateTime.now());
        }

        vehicle = repository.save(vehicle);

        return mapper.toResponse(vehicle);
    }

    @Override
    public Page<VehicleResponse> getAllVehicles(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Vehicle> vehicles = repository.findAll(pageable);

        return vehicles.map(mapper::toResponse);
    }
}