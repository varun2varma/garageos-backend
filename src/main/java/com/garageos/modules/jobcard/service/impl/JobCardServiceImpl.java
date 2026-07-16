package com.garageos.modules.jobcard.service.impl;

import com.garageos.core.enums.ComplaintStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.JobCardNumberGenerator;
import com.garageos.modules.complaint.service.ComplaintService;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.mapper.JobCardMapper;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.vehicle.entity.Vehicle;
import com.garageos.modules.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobCardServiceImpl implements JobCardService {

    private final JobCardRepository repository;
    private final VehicleRepository vehicleRepository;
    private final JobCardMapper mapper;
    @Autowired
    private final ComplaintService complaintService;
    @Override
    public JobCardResponse createJobCard(CreateJobCardRequest request) {

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : "
                                        + request.getVehicleId()));

        Optional<JobCard> latestJobCard =
                repository.findTopByOrderByIdDesc();

        String jobCardNumber =
                JobCardNumberGenerator.generate(
                        latestJobCard
                                .map(JobCard::getJobCardNumber)
                                .orElse(null));

        JobCard jobCard = mapper.toEntity(request);

        jobCard.setJobCardNumber(jobCardNumber);
        jobCard.setVehicle(vehicle);
        jobCard.setCustomer(vehicle.getCustomer());
        jobCard.setServiceDate(LocalDate.now());
        jobCard.setStatus(JobCardStatus.OPEN);
        jobCard = repository.save(jobCard);
        complaintService.createComplaintList(jobCard.getId(),request.getComplaints());

        return mapper.toResponse(jobCard);
    }

    @Override
    public JobCardResponse getJobCard(Long id) {

        JobCard jobCard = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + id));

        return mapper.toResponse(jobCard);
    }

    @Override
    public JobCardResponse updateJobCard(
            Long id,
            CreateJobCardRequest request) {

        JobCard jobCard = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + id));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : "
                                        + request.getVehicleId()));

        mapper.updateEntity(request, jobCard);

        jobCard.setVehicle(vehicle);
        jobCard.setCustomer(vehicle.getCustomer());

        jobCard = repository.save(jobCard);

        return mapper.toResponse(jobCard);
    }

    @Override
    public void deleteJobCard(Long id) {

        JobCard jobCard = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + id));

        repository.delete(jobCard);
    }

    @Override
    public JobCardResponse getJobCardByNumber(
            String jobCardNumber) {

        JobCard jobCard = repository.findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : "
                                        + jobCardNumber));

        return mapper.toResponse(jobCard);
    }

    @Override
    public Page<JobCardResponse> getAllJobCards(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<JobCard> jobCards =
                repository.findAll(pageable);

        return jobCards.map(mapper::toResponse);
    }

    @Override
    public JobCardResponse completeJobCard(Long id) {

        JobCard jobCard = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + id));

        jobCard.setStatus(JobCardStatus.WORK_COMPLETED);

        jobCard = repository.save(jobCard);

        return mapper.toResponse(jobCard);
    }

    @Override
    public JobCardResponse readyForDelivery(Long id) {

        JobCard jobCard = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + id));

        jobCard.setStatus(JobCardStatus.READY_FOR_DELIVERY);

        jobCard = repository.save(jobCard);

        return mapper.toResponse(jobCard);
    }

    @Override
    public JobCardResponse closeJobCard(Long id) {

        JobCard jobCard = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + id));

        jobCard.setStatus(JobCardStatus.CLOSED);

        jobCard = repository.save(jobCard);

        return mapper.toResponse(jobCard);
    }

}