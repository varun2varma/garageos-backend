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
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private final InspectionRepository inspectionRepository;
    private final ComplaintRepository complaintRepository;
    private final InspectionMapper inspectionMapper;
    private final JobCardRepository jobCardRepository;

    @Override
    public InspectionResponse createInspection(
            Long complaintId,
            CreateInspectionRequest request) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Complaint not found with id : " + complaintId));

        Inspection inspection = inspectionMapper.toEntity(request);

        inspection.setComplaint(complaint);
        inspection.setStatus(InspectionStatus.PENDING);

        inspection = inspectionRepository.save(inspection);

        return inspectionMapper.toResponse(inspection);
    }

    @Override
    public InspectionResponse getInspection(Long id) {

        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection not found with id : " + id));

        return inspectionMapper.toResponse(inspection);
    }

    @Override
    public List<InspectionResponse> getInspections(Long complaintId) {

        return inspectionRepository.findByComplaintId(complaintId)
                .stream()
                .map(inspectionMapper::toResponse)
                .toList();
    }

    @Override
    public InspectionResponse updateInspection(
            Long id,
            CreateInspectionRequest request) {

        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection not found with id : " + id));

        inspectionMapper.updateEntity(request, inspection);

        inspection.setStatus(InspectionStatus.COMPLETED);

        inspection = inspectionRepository.save(inspection);

        return inspectionMapper.toResponse(inspection);
    }

    @Override
    public void deleteInspection(Long id) {

        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inspection not found with id : " + id));

        inspectionRepository.delete(inspection);
    }

//    @Override
//    public InspectionResponse startInspection(Long complaintId) {
//
//        Complaint complaint = complaintRepository.findById(complaintId)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Complaint not found with id : " + complaintId));
//
//        Inspection inspection = new Inspection();
//
//        inspection.setComplaint(complaint);
//        inspection.setStatus(InspectionStatus.PENDING);
//
//        inspection = inspectionRepository.save(inspection);
//
//        return inspectionMapper.toResponse(inspection);
//    }
    @Override
    @Transactional
    public List<InspectionResponse> startInspection(String jobCardNumber) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));
        // Prevent duplicate inspection creation
        List<Inspection> existingInspections =
                inspectionRepository.findByComplaintJobCardId(jobCard.getId());

        if (!existingInspections.isEmpty()) {
            throw new IllegalStateException(
                    "Inspection already started for Job Card : "
                            + jobCardNumber);
        }
        List<Complaint> complaints =
                complaintRepository.findByJobCardId(jobCard.getId());

        if (complaints.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No complaints found for Job Card : " + jobCardNumber);
        }

        List<Inspection> inspections = new ArrayList<>();

        for (Complaint complaint : complaints) {

            Inspection inspection = new Inspection();

            inspection.setComplaint(complaint);
            inspection.setStatus(InspectionStatus.PENDING);

            inspections.add(inspection);
        }

        inspections = inspectionRepository.saveAll(inspections);

        return inspections.stream()
                .map(inspectionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<InspectionResponse> completeInspection(
            String jobCardNumber,
            List<CreateInspectionRequest> requests) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));

        List<Complaint> complaints =
                complaintRepository.findByJobCardId(jobCard.getId());

        if (complaints.size() != requests.size()) {
            throw new IllegalArgumentException(
                    "Inspection count must match complaint count.");
        }

        List<InspectionResponse> responses = new ArrayList<>();

        for (int i = 0; i < complaints.size(); i++) {

            Complaint complaint = complaints.get(i);

            Inspection inspection = inspectionRepository
                    .findByComplaintId(complaint.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inspection not found for complaint : "
                                            + complaint.getId()));

            CreateInspectionRequest request = requests.get(i);

            inspection.setInspectionNotes(request.getInspectionNotes());
            inspection.setRecommendedWork(request.getRecommendedWork());
            inspection.setStatus(InspectionStatus.COMPLETED);

            inspection = inspectionRepository.save(inspection);

            responses.add(inspectionMapper.toResponse(inspection));
        }

        return responses;
    }

//    @Override
//    public InspectionResponse completeInspection(
//            Long inspectionId,
//            CreateInspectionRequest request) {
//
//        Inspection inspection = inspectionRepository.findById(inspectionId)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Inspection not found with id : " + inspectionId));
//
//        inspection.setInspectionNotes(request.getInspectionNotes());
//        inspection.setRecommendedWork(request.getRecommendedWork());
//        inspection.setStatus(InspectionStatus.COMPLETED);
//
//        inspection = inspectionRepository.save(inspection);
//
//        return inspectionMapper.toResponse(inspection);
//    }

}