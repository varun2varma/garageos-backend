package com.garageos.modules.inspectionmaster.service.impl;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.inspectionmaster.dto.request.CreateInspectionMasterRequest;
import com.garageos.modules.inspectionmaster.dto.response.InspectionMasterResponse;
import com.garageos.modules.inspectionmaster.entity.InspectionMaster;
import com.garageos.modules.inspectionmaster.mapper.InspectionMasterMapper;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterRepository;
import com.garageos.modules.inspectionmaster.service.InspectionMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InspectionMasterServiceImpl
        implements InspectionMasterService {

    private final InspectionMasterRepository repository;

    private final InspectionMasterMapper mapper;

    @Override
    public InspectionMasterResponse createInspectionMaster(
            CreateInspectionMasterRequest request) {

        repository.findByMakeIgnoreCaseAndModelIgnoreCaseAndVariantIgnoreCaseAndFuelTypeAndTransmissionTypeAndActiveTrue(
                        request.getMake(),
                        request.getModel(),
                        request.getVariant(),
                        request.getFuelType(),
                        request.getTransmissionType())
                .ifPresent(master -> {
                    throw new BusinessException(
                            "Inspection Master already exists for this vehicle.");
                });

        InspectionMaster entity = mapper.toEntity(request);

        entity = repository.save(entity);

        return mapper.toResponse(entity);
    }

    @Override
    public InspectionMasterResponse getInspectionMaster(Long id) {

        InspectionMaster entity = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection Master not found with id : " + id));

        return mapper.toResponse(entity);
    }

    @Override
    public InspectionMasterResponse updateInspectionMaster(
            Long id,
            CreateInspectionMasterRequest request) {

        InspectionMaster entity = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection Master not found with id : " + id));

        mapper.updateEntity(request, entity);

        entity = repository.save(entity);

        return mapper.toResponse(entity);
    }

    @Override
    public void deleteInspectionMaster(Long id) {

        InspectionMaster entity = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection Master not found with id : " + id));

        repository.delete(entity);
    }

    @Override
    public Page<InspectionMasterResponse> getAllInspectionMasters(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<InspectionMaster> masters = repository.findAll(pageable);

        return masters.map(mapper::toResponse);
    }

    @Override
    public InspectionMasterResponse getInspectionMasterByVehicle(
            String make,
            String model,
            String variant,
            FuelType fuelType,
            TransmissionType transmissionType,
            Integer year,
            Integer odometer) {

        InspectionMaster entity = repository
                .findApplicableInspectionMaster(
                        make,
                        model,
                        variant,
                        fuelType,
                        transmissionType,
                        year,
                        odometer)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection Master not found for vehicle."));

        return mapper.toResponse(entity);
    }

}