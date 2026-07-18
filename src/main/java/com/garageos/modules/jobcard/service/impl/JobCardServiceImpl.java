package com.garageos.modules.jobcard.service.impl;

import com.garageos.core.enums.ComplaintStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.JobCardNumberGenerator;
import com.garageos.modules.complaint.dto.response.ComplaintResponse;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.complaint.mapper.ComplaintMapper;
import com.garageos.modules.complaint.service.ComplaintService;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.mapper.JobCardMapper;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.vehicle.entity.Vehicle;
import com.garageos.modules.vehicle.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobCardServiceImpl implements JobCardService {

    private final JobCardRepository jobCardRepository;
    private final VehicleRepository vehicleRepository;
    private final JobCardMapper jobCardMapper;
    private final JobCardStatusValidator statusValidator;
    private final ComplaintService complaintService;
    private final ComplaintMapper complaintMapper;
    @Override
    public JobCardResponse createJobCard(CreateJobCardRequest request) {

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : "
                                        + request.getVehicleId()));

        Optional<JobCard> latestJobCard =
                jobCardRepository.findTopByOrderByIdDesc();

        String jobCardNumber =
                JobCardNumberGenerator.generate(
                        latestJobCard
                                .map(JobCard::getJobCardNumber)
                                .orElse(null));

        JobCard jobCard = jobCardMapper.toEntity(request);

        jobCard.setJobCardNumber(jobCardNumber);
        jobCard.setVehicle(vehicle);
        jobCard.setCustomer(vehicle.getCustomer());
        jobCard.setServiceDate(LocalDate.now());
        jobCard.setStatus(JobCardStatus.OPEN);
        List<Complaint> complaints = complaintMapper.toEntity(request.getComplaints());
        JobCard finalJobCard = jobCard;
        complaints.forEach(complaint -> {
            complaint.setJobCard(finalJobCard);
            complaint.setStatus(ComplaintStatus.OPEN);
        });
        jobCard.setComplaints(complaints);
        jobCard = jobCardRepository.save(jobCard);
//        List<ComplaintResponse> complaintResponseList = complaintService.createComplaintList(jobCard.getId(),request.getComplaints());
//        List<Complaint> complaints =
//                complaintService.createJobWorkComplaintList(
//                        jobCard,
//                        request.getComplaints()
//                );

//        jobCard.setComplaints(complaints);
        return jobCardMapper.toResponse(jobCard);
    }

    @Override
    public JobCardResponse getJobCard(Long id) {

        JobCard jobCard = getJobCardByIdOrThrow(id);

        return jobCardMapper.toResponse(jobCard);
    }

    @Override
    public JobCardResponse updateJobCard(
            Long id,
            CreateJobCardRequest request) {

        JobCard jobCard = getJobCardByIdOrThrow(id);

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : "
                                        + request.getVehicleId()));

        jobCardMapper.updateEntity(request, jobCard);

        jobCard.setVehicle(vehicle);
        jobCard.setCustomer(vehicle.getCustomer());

        jobCard = jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }

    @Override
    public void deleteJobCard(Long id) {

        JobCard jobCard = getJobCardByIdOrThrow(id);

        jobCardRepository.delete(jobCard);
    }

    @Override
    public JobCardResponse getJobCardByNumber(
            String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        return jobCardMapper.toResponse(jobCard);
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
                jobCardRepository.findAll(pageable);

        return jobCards.map(jobCardMapper::toResponse);
    }


    @Override
    @Transactional
    public JobCardResponse startInspection(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.INSPECTION_PENDING
        );

        jobCard.setStatus(JobCardStatus.INSPECTION_PENDING);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse completeInspection(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.INSPECTION_COMPLETED
        );

        jobCard.setStatus(JobCardStatus.INSPECTION_COMPLETED);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse prepareEstimate(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.ESTIMATE_PENDING
        );

        jobCard.setStatus(JobCardStatus.ESTIMATE_PENDING);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse approveEstimate(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.ESTIMATE_APPROVED
        );

        jobCard.setStatus(JobCardStatus.ESTIMATE_APPROVED);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse startRepair(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.REPAIR_IN_PROGRESS
        );

        jobCard.setStatus(JobCardStatus.REPAIR_IN_PROGRESS);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse completeRepair(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.WORK_COMPLETED
        );

        jobCard.setStatus(JobCardStatus.WORK_COMPLETED);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse performQualityCheck(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.QUALITY_CHECK
        );

        jobCard.setStatus(JobCardStatus.QUALITY_CHECK);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse readyForDelivery(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.READY_FOR_DELIVERY
        );

        jobCard.setStatus(JobCardStatus.READY_FOR_DELIVERY);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse invoiceGenerated(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.INVOICE_GENERATED
        );

        jobCard.setStatus(JobCardStatus.INVOICE_GENERATED);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }
    @Override
    @Transactional
    public JobCardResponse closeJobCard(String jobCardNumber) {

        JobCard jobCard = getJobCardByNumberOrThrow(jobCardNumber);

//        statusValidator.validate(
//                jobCard.getStatus(),
//                JobCardStatus.CLOSED
//        );

        jobCard.setStatus(JobCardStatus.CLOSED);

        jobCardRepository.save(jobCard);

        return jobCardMapper.toResponse(jobCard);
    }

    private JobCard getJobCardByIdOrThrow(Long id) {
        return jobCardRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + id));
    }
    private JobCard getJobCardByNumberOrThrow(String jobCardNumber) {

        return jobCardRepository.findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));
    }
}