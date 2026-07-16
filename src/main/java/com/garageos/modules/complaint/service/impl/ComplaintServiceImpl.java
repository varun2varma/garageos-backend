package com.garageos.modules.complaint.service.impl;

import com.garageos.core.enums.ComplaintStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.complaint.dto.request.CreateComplaintRequest;
import com.garageos.modules.complaint.dto.response.ComplaintResponse;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.complaint.mapper.ComplaintMapper;
import com.garageos.modules.complaint.repository.ComplaintRepository;
import com.garageos.modules.complaint.service.ComplaintService;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository repository;
    private final JobCardRepository jobCardRepository;
    private final ComplaintMapper mapper;

    @Override
    public ComplaintResponse createComplaint(
            Long jobCardId,
            CreateComplaintRequest request) {

        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + jobCardId));

        Complaint complaint = mapper.toEntity(request);

        complaint.setJobCard(jobCard);
        complaint.setStatus(ComplaintStatus.OPEN);

        complaint = repository.save(complaint);

        return mapper.toResponse(complaint);
    }

    @Override
    public List<ComplaintResponse> createComplaintList(
            Long jobCardId,
            List<CreateComplaintRequest> request) {

        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + jobCardId));

        List<Complaint> complaints = mapper.toEntity(request);

        complaints.forEach(complaint -> {
            complaint.setJobCard(jobCard);
            complaint.setStatus(ComplaintStatus.OPEN);


        });
        complaints = repository.saveAll(complaints);


        return mapper.toResponse(complaints);
    }

    @Override
    public ComplaintResponse getComplaint(Long id) {

        Complaint complaint = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Complaint not found with id : " + id));

        return mapper.toResponse(complaint);
    }

    @Override
    public List<ComplaintResponse> getComplaints(Long jobCardId) {

        return repository.findByJobCardId(jobCardId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public ComplaintResponse updateComplaint(
            Long id,
            CreateComplaintRequest request) {

        Complaint complaint = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Complaint not found with id : " + id));

        mapper.updateEntity(request, complaint);

        complaint = repository.save(complaint);

        return mapper.toResponse(complaint);
    }

    @Override
    public void deleteComplaint(Long id) {

        Complaint complaint = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Complaint not found with id : " + id));

        repository.delete(complaint);
    }
}