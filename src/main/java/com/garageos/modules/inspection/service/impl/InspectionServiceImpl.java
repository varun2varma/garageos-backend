package com.garageos.modules.inspection.service.impl;

import com.garageos.core.enums.InspectionStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.complaint.repository.ComplaintRepository;
import com.garageos.modules.inspection.dto.request.CreateInspectionRequest;
import com.garageos.modules.inspection.dto.response.InspectionResponse;
import com.garageos.modules.inspection.entity.Inspection;
import com.garageos.modules.inspection.mapper.InspectionMapper;
import com.garageos.modules.inspection.repository.InspectionRepository;
import com.garageos.modules.inspection.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private final InspectionRepository repository;
    private final ComplaintRepository complaintRepository;
    private final InspectionMapper mapper;

    @Override
    public InspectionResponse createInspection(
            Long complaintId,
            CreateInspectionRequest request) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Complaint not found with id : " + complaintId));

        Inspection inspection = mapper.toEntity(request);

        inspection.setComplaint(complaint);
        inspection.setStatus(InspectionStatus.PENDING);

        inspection = repository.save(inspection);

        return mapper.toResponse(inspection);
    }

    @Override
    public InspectionResponse getInspection(Long id) {

        Inspection inspection = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection not found with id : " + id));

        return mapper.toResponse(inspection);
    }

    @Override
    public List<InspectionResponse> getInspections(Long complaintId) {

        return repository.findByComplaintId(complaintId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public InspectionResponse updateInspection(
            Long id,
            CreateInspectionRequest request) {

        Inspection inspection = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection not found with id : " + id));

        mapper.updateEntity(request, inspection);

        inspection.setStatus(InspectionStatus.COMPLETED);

        inspection = repository.save(inspection);

        return mapper.toResponse(inspection);
    }

    @Override
    public void deleteInspection(Long id) {

        Inspection inspection = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection not found with id : " + id));

        repository.delete(inspection);
    }
}