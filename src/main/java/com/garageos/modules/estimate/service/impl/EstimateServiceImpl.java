package com.garageos.modules.estimate.service.impl;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.EstimateNumberGenerator;
import com.garageos.modules.estimate.dto.request.CreateEstimateRequest;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.mapper.EstimateMapper;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EstimateServiceImpl implements EstimateService {

    private final EstimateRepository repository;
    private final JobCardRepository jobCardRepository;
    private final EstimateMapper mapper;

    @Override
    public EstimateResponse createEstimate(CreateEstimateRequest request) {

        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : "
                                        + request.getJobCardId()));

        Optional<Estimate> latestEstimate =
                repository.findTopByOrderByIdDesc();

        String estimateNumber =
                EstimateNumberGenerator.generate(
                        latestEstimate
                                .map(Estimate::getEstimateNumber)
                                .orElse(null));

        Estimate estimate = mapper.toEntity(request);

        estimate.setEstimateNumber(estimateNumber);
        estimate.setJobCard(jobCard);
        estimate.setStatus(EstimateStatus.DRAFT);

        estimate.setSubtotal(BigDecimal.ZERO);
        estimate.setDiscount(BigDecimal.ZERO);
        estimate.setGst(BigDecimal.ZERO);
        estimate.setGrandTotal(BigDecimal.ZERO);

        estimate = repository.save(estimate);

        return mapper.toResponse(estimate);
    }

    @Override
    public EstimateResponse getEstimate(Long id) {

        Estimate estimate = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        return mapper.toResponse(estimate);
    }

    @Override
    public List<EstimateResponse> getAllEstimates() {

        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public EstimateResponse updateEstimate(
            Long id,
            CreateEstimateRequest request) {

        Estimate estimate = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : "
                                        + request.getJobCardId()));

        mapper.updateEntity(request, estimate);

        estimate.setJobCard(jobCard);

        estimate = repository.save(estimate);

        return mapper.toResponse(estimate);
    }

    @Override
    public void deleteEstimate(Long id) {

        Estimate estimate = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        repository.delete(estimate);
    }

    @Override
    public EstimateResponse approveEstimate(Long id) {

        Estimate estimate = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        if (estimate.getStatus() == EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "Estimate is already approved.");
        }

        estimate.setStatus(EstimateStatus.APPROVED);

        JobCard jobCard = estimate.getJobCard();

        jobCard.setStatus(JobCardStatus.ESTIMATE_APPROVED);

        jobCardRepository.save(jobCard);

        estimate = repository.save(estimate);

        return mapper.toResponse(estimate);
    }

    @Override
    public EstimateResponse rejectEstimate(Long id) {

        Estimate estimate = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        if (estimate.getStatus() == EstimateStatus.REJECTED) {
            throw new BusinessException(
                    "Estimate is already rejected.");
        }

        estimate.setStatus(EstimateStatus.REJECTED);

        estimate = repository.save(estimate);

        return mapper.toResponse(estimate);
    }
}