package com.garageos.modules.inspectionfinding.service.impl;

import com.garageos.core.enums.InspectionFindingStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.inspectionfinding.dto.request.CreateInspectionFindingRequest;
import com.garageos.modules.inspectionfinding.dto.request.RecommendationRequest;
import com.garageos.modules.inspectionfinding.dto.response.InspectionFindingResponse;
import com.garageos.modules.inspectionfinding.entity.InspectionFinding;
import com.garageos.modules.inspectionfinding.mapper.InspectionFindingMapper;
import com.garageos.modules.inspectionfinding.repository.InspectionFindingRepository;
import com.garageos.modules.inspectionfinding.service.InspectionFindingService;
import com.garageos.modules.inspectionmaster.dto.response.InspectionMasterItemResponse;
import com.garageos.modules.inspectionmaster.entity.InspectionMaster;
import com.garageos.modules.inspectionmaster.entity.InspectionMasterItem;
import com.garageos.modules.inspectionmaster.mapper.InspectionMasterItemMapper;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterItemRepository;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.vehicle.entity.Vehicle;
import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionFindingServiceImpl
        implements InspectionFindingService {

    private final InspectionFindingRepository repository;

    private final InspectionFindingMapper mapper;

    private final JobCardRepository jobCardRepository;

    private final InspectionMasterRepository inspectionMasterRepository;

    private final InspectionMasterItemRepository inspectionMasterItemRepository;

    private final VehicleRepository vehicleRepository;

    private final InspectionMasterItemMapper inspectionMasterItemMapper;

    @Override
    public InspectionFindingResponse createInspectionFinding(
            Long jobCardId,
            CreateInspectionFindingRequest request) {

        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + jobCardId));

        InspectionMasterItem masterItem =
                inspectionMasterItemRepository.findById(
                                request.getInspectionMasterItemId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Inspection Master Item not found with id : "
                                                + request.getInspectionMasterItemId()));

        InspectionFinding finding = mapper.toEntity(request);

        finding.setJobCard(jobCard);

        finding.setInspectionMasterItem(masterItem);

        finding = repository.save(finding);

        return mapper.toResponse(finding);
    }

    @Override
    public InspectionFindingResponse getInspectionFinding(Long id) {

        InspectionFinding finding = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection Finding not found with id : " + id));

        return mapper.toResponse(finding);
    }

    @Override
    public List<InspectionFindingResponse> getInspectionFindings(
            Long jobCardId) {

        return mapper.toResponseList(
                repository.findByJobCardIdOrderById(jobCardId));
    }

    @Override
    public InspectionFindingResponse updateInspectionFinding(
            Long id,
            CreateInspectionFindingRequest request) {

        InspectionFinding finding = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection Finding not found with id : " + id));

        InspectionMasterItem masterItem =
                inspectionMasterItemRepository.findById(
                                request.getInspectionMasterItemId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Inspection Master Item not found."));

        mapper.updateEntity(request, finding);

        finding.setInspectionMasterItem(masterItem);

        finding = repository.save(finding);

        return mapper.toResponse(finding);
    }

    @Override
    public void deleteInspectionFinding(Long id) {

        InspectionFinding finding = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection Finding not found with id : " + id));

        repository.delete(finding);
    }

    @Override
    public void loadInspectionTemplate(Long jobCardId) {

        if (repository.existsByJobCardId(jobCardId)) {

            throw new BusinessException(
                    "Inspection checklist already loaded.");
        }

        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + jobCardId));

        Vehicle vehicle = jobCard.getVehicle();

        System.out.println("Make        : " + vehicle.getBrand() + " (" + vehicle.getBrand().getClass() + ")");
        System.out.println("Model       : " + vehicle.getModel() + " (" + vehicle.getModel().getClass() + ")");
        System.out.println("Variant     : " + vehicle.getVariant() + " (" +
                (vehicle.getVariant() == null ? "null" : vehicle.getVariant().getClass()) + ")");
        System.out.println("FuelType    : " + vehicle.getFuelType());
        System.out.println("Transmission: " + vehicle.getTransmission());
        System.out.println("Year        : " + vehicle.getManufacturingYear());
        System.out.println("Odometer    : " + jobCard.getOdometerReading().intValue());

        InspectionMaster master =
                inspectionMasterRepository
                        .findApplicableInspectionMaster(
                                vehicle.getBrand(),
                                vehicle.getModel(),
                                vehicle.getVariant(),
                                vehicle.getFuelType(),
                                vehicle.getTransmission(),
                                vehicle.getManufacturingYear(),
                                jobCard.getOdometerReading().intValue())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Inspection Master not found."));

        List<InspectionMasterItem> masterItems =
                inspectionMasterItemRepository
                        .findByInspectionMasterIdOrderByDisplayOrder(
                                master.getId());

        List<InspectionFinding> findings =
                new ArrayList<>();

        for (InspectionMasterItem item : masterItems) {

//            InspectionFinding finding =
//                    InspectionFinding.builder()
//                            .jobCard(jobCard)
//                            .inspectionMasterItem(item)
//                            .status(InspectionFindingStatus.NOT_INSPECTED)
//                            .remarks(null)
//                            .build();

            InspectionFinding finding = new InspectionFinding();
            finding.setJobCard(jobCard);
            finding.setComplaint(null);
            finding.setInspectionMasterItem(item);
            finding.setStatus(InspectionFindingStatus.NOT_INSPECTED);
            finding.setRemarks(null);

            findings.add(finding);

        }

        repository.saveAll(findings);

    }

    @Override
    public void completeInspection(Long jobCardId) {

        long total = repository.countByJobCardId(jobCardId);

        long inspected =
                repository.countByJobCardIdAndStatus(
                        jobCardId,
                        InspectionFindingStatus.PASS)
                        +
                        repository.countByJobCardIdAndStatus(
                                jobCardId,
                                InspectionFindingStatus.FAIL)
                        +
                        repository.countByJobCardIdAndStatus(
                                jobCardId,
                                InspectionFindingStatus.REPAIR_REQUIRED);

        if (total != inspected) {

            throw new BusinessException(
                    "Complete all inspection items before finishing inspection.");
        }

    }

    @Override
    public List<InspectionMasterItemResponse> getRecommendations(
            RecommendationRequest request) {

//        String brand = request.getBrand();
//        String model = request.getModel();
//        String variant = request.getVariant();
//        FuelType fuelType = request.getFuelType();
//        TransmissionType transmission = request.getTransmission();
//        Integer manufacturingYear = request.getManufacturingYear();
//        Integer odometer = request.getOdometer();

        // existing recommendation logic
        System.out.println("==============================");
        System.out.println("Request        : " + request.toString());
        System.out.println("Make        : " + request.getBrand());
        System.out.println("Model       : " + request.getModel());
        System.out.println("Variant     : " + request.getVariant());
        System.out.println("FuelType    : " + request.getFuelType());
        System.out.println("Transmission: " + request.getTransmission());
        System.out.println("Year        : " + request.getManufacturingYear());
        System.out.println("Odometer    : " + request.getOdometer());
        System.out.println("==============================");

        InspectionMaster master =
                inspectionMasterRepository.findApplicableInspectionMaster(

                        request.getBrand(),

                        request.getModel(),

                        request.getVariant(),

                        request.getFuelType(),

                        request.getTransmission(),

                        request.getManufacturingYear(),

                        request.getOdometer()

                ).orElse(new InspectionMaster());

        return master.getItems()
                .stream()
                .map(inspectionMasterItemMapper::toResponse)
                .toList();

    }
}