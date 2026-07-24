package com.garageos.modules.inspectionfinding.service.impl;

import com.garageos.core.enums.InspectionFindingStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.inspectionfinding.dto.request.CreateInspectionFindingRequest;
import com.garageos.modules.inspectionfinding.dto.response.InspectionFindingResponse;
import com.garageos.modules.inspectionfinding.entity.InspectionFinding;
import com.garageos.modules.inspectionfinding.mapper.InspectionFindingMapper;
import com.garageos.modules.inspectionfinding.repository.InspectionFindingRepository;
import com.garageos.modules.inspectionfinding.service.InspectionFindingService;
import com.garageos.modules.inspectionmaster.entity.InspectionMaster;
import com.garageos.modules.inspectionmaster.entity.InspectionMasterItem;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterItemRepository;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}