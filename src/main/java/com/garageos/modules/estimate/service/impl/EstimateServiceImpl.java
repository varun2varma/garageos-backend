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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EstimateServiceImpl implements EstimateService {

    private final EstimateRepository estimateRepository;
    private final JobCardRepository jobCardRepository;
    private final EstimateMapper estimateMapper;

    @Override
    public EstimateResponse createEstimate(CreateEstimateRequest request) {

        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : "
                                        + request.getJobCardId()));

        Optional<Estimate> latestEstimate =
                estimateRepository.findTopByOrderByIdDesc();

        String estimateNumber =
                EstimateNumberGenerator.generate(
                        latestEstimate
                                .map(Estimate::getEstimateNumber)
                                .orElse(null));

        Estimate estimate = estimateMapper.toEntity(request);

        estimate.setEstimateNumber(estimateNumber);
        estimate.setJobCard(jobCard);
        estimate.setStatus(EstimateStatus.DRAFT);

        estimate.setSubtotal(BigDecimal.ZERO);
        estimate.setDiscount(BigDecimal.ZERO);
        estimate.setGst(BigDecimal.ZERO);
        estimate.setGrandTotal(BigDecimal.ZERO);

        estimate = estimateRepository.save(estimate);

        return estimateMapper.toResponse(estimate);
    }

    @Override
    public EstimateResponse getEstimate(Long id) {

        Estimate estimate = estimateRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        return estimateMapper.toResponse(estimate);
    }

    @Override
    public List<EstimateResponse> getAllEstimates() {

        return estimateRepository.findAll()
                .stream()
                .map(estimateMapper::toResponse)
                .toList();
    }

    @Override
    public EstimateResponse updateEstimate(
            Long id,
            CreateEstimateRequest request) {

        Estimate estimate = estimateRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : "
                                        + request.getJobCardId()));

        estimateMapper.updateEntity(request, estimate);

        estimate.setJobCard(jobCard);

        estimate = estimateRepository.save(estimate);

        return estimateMapper.toResponse(estimate);
    }

    @Override
    public void deleteEstimate(Long id) {

        Estimate estimate = estimateRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        estimateRepository.delete(estimate);
    }

    @Override
    public EstimateResponse approveEstimate(Long id) {

        Estimate estimate = estimateRepository.findById(id)
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

        estimate = estimateRepository.save(estimate);

        return estimateMapper.toResponse(estimate);
    }

    @Override
    public EstimateResponse rejectEstimate(Long id) {

        Estimate estimate = estimateRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : " + id));

        if (estimate.getStatus() == EstimateStatus.REJECTED) {
            throw new BusinessException(
                    "Estimate is already rejected.");
        }

        estimate.setStatus(EstimateStatus.REJECTED);

        estimate = estimateRepository.save(estimate);

        return estimateMapper.toResponse(estimate);
    }

    @Override
    @Transactional
    public EstimateResponse createEstimate(String jobCardNumber) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));

        if (estimateRepository.existsByJobCardId(jobCard.getId())) {
            throw new BusinessException(
                    "Estimate already exists for Job Card : "
                            + jobCardNumber);
        }

        Optional<Estimate> latestEstimate =
                estimateRepository.findTopByOrderByIdDesc();

        String estimateNumber =
                EstimateNumberGenerator.generate(
                        latestEstimate
                                .map(Estimate::getEstimateNumber)
                                .orElse(null));

        Estimate estimate = new Estimate();

        estimate.setEstimateNumber(estimateNumber);
        estimate.setJobCard(jobCard);
        estimate.setStatus(EstimateStatus.DRAFT);

        estimate.setSubtotal(BigDecimal.ZERO);
        estimate.setDiscount(BigDecimal.ZERO);
        estimate.setGst(BigDecimal.ZERO);
        estimate.setGrandTotal(BigDecimal.ZERO);

        estimate = estimateRepository.save(estimate);

        return estimateMapper.toResponse(estimate);
    }

    @Override
    @Transactional
    public EstimateResponse approveEstimate(String jobCardNumber) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));

        Estimate estimate = estimateRepository
                .findByJobCardId(jobCard.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found for Job Card : "
                                        + jobCardNumber));

        if (estimate.getStatus() == EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "Estimate is already approved.");
        }

        estimate.setStatus(EstimateStatus.APPROVED);

        estimate = estimateRepository.save(estimate);

        return estimateMapper.toResponse(estimate);
    }
}