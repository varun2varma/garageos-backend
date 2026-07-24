package com.garageos.modules.qualitycheck.service.impl;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.QualityCheckStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.qualitycheck.dto.request.CreateQualityCheckRequest;
import com.garageos.modules.qualitycheck.dto.response.QualityCheckResponse;
import com.garageos.modules.qualitycheck.entity.QualityCheck;
import com.garageos.modules.qualitycheck.mapper.QualityCheckMapper;
import com.garageos.modules.qualitycheck.repository.QualityCheckRepository;
import com.garageos.modules.qualitycheck.service.QualityCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class QualityCheckServiceImpl
        implements QualityCheckService {

    private final QualityCheckRepository repository;

    private final JobCardRepository jobCardRepository;

    private final QualityCheckMapper mapper;

    @Override
    @Transactional
    public void createQualityCheck(JobCard jobCard) {

        if (repository.existsByJobCardId(jobCard.getId())) {
            return;
        }

        QualityCheck qualityCheck =
                QualityCheck.builder()
                        .jobCard(jobCard)
                        .status(QualityCheckStatus.PENDING)
                        .build();

        repository.save(qualityCheck);

    }

    @Override
    public QualityCheckResponse getQualityCheck(String jobCardNumber) {

        QualityCheck qualityCheck =
                repository.findByJobCardJobCardNumber(jobCardNumber)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Quality Check not found for Job Card : "
                                                + jobCardNumber));

        return mapper.toResponse(qualityCheck);

    }

//    @Override
//    @Transactional
//    public QualityCheckResponse passQualityCheck(
//            String jobCardNumber,
//            CreateQualityCheckRequest request) {
//
//        QualityCheck qualityCheck =
//                repository.findByJobCardJobCardNumber(jobCardNumber)
//                        .orElseThrow(() ->
//                                new ResourceNotFoundException(
//                                        "Quality Check not found."));
//
//        if (qualityCheck.getStatus() == QualityCheckStatus.PASSED) {
//            throw new BusinessException(
//                    "Quality Check already passed.");
//        }
//
//        mapper.updateEntity(request, qualityCheck);
//
//        qualityCheck.setStatus(QualityCheckStatus.PASSED);
//
//        qualityCheck.setInspectedAt(LocalDateTime.now());
//
//        JobCard jobCard = qualityCheck.getJobCard();
//
//        jobCard.setStatus(JobCardStatus.READY_FOR_INVOICE);
//
//        jobCardRepository.save(jobCard);
//
//        qualityCheck = repository.save(qualityCheck);
//
//        return mapper.toResponse(qualityCheck);
//
//    }

    @Override
    @Transactional
    public QualityCheckResponse passQualityCheck(
            String jobCardNumber,
            CreateQualityCheckRequest request) {

        QualityCheck qualityCheck = repository
                .findByJobCardJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Quality Check not found."));

        JobCard jobCard = qualityCheck.getJobCard();

        if (jobCard.getStatus() != JobCardStatus.REPAIR_COMPLETED) {
            throw new BusinessException(
                    "Repair must be completed before Quality Check.");
        }

        mapper.updateEntity(request, qualityCheck);

        qualityCheck.setStatus(QualityCheckStatus.PASSED);
        qualityCheck.setInspectedAt(LocalDateTime.now());

        jobCard.setStatus(JobCardStatus.READY_FOR_INVOICE);

        repository.save(qualityCheck);
        jobCardRepository.save(jobCard);

        return mapper.toResponse(qualityCheck);
    }

    @Override
    @Transactional
    public QualityCheckResponse failQualityCheck(
            String jobCardNumber,
            CreateQualityCheckRequest request) {

        QualityCheck qualityCheck = repository
                .findByJobCardJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Quality Check not found."));

        JobCard jobCard = qualityCheck.getJobCard();

        if (jobCard.getStatus() != JobCardStatus.REPAIR_COMPLETED) {
            throw new BusinessException(
                    "Repair must be completed before Quality Check.");
        }

        mapper.updateEntity(request, qualityCheck);

        qualityCheck.setStatus(QualityCheckStatus.FAILED);
        qualityCheck.setInspectedAt(LocalDateTime.now());

        jobCard.setStatus(JobCardStatus.REPAIR_PENDING);

        repository.save(qualityCheck);
        jobCardRepository.save(jobCard);

        return mapper.toResponse(qualityCheck);
    }


//    @Override
//    @Transactional
//    public QualityCheckResponse failQualityCheck(
//            Long jobCardId,
//            CreateQualityCheckRequest request) {
//
//        QualityCheck qualityCheck =
//                repository.findByJobCardId(jobCardId)
//                        .orElseThrow(() ->
//                                new ResourceNotFoundException(
//                                        "Quality Check not found."));
//
//        mapper.updateEntity(request, qualityCheck);
//
//        qualityCheck.setStatus(QualityCheckStatus.FAILED);
//
//        qualityCheck.setInspectedAt(LocalDateTime.now());
//
//        JobCard jobCard = qualityCheck.getJobCard();
//
//        jobCard.setStatus(JobCardStatus.REPAIR_PENDING);
//
//        jobCardRepository.save(jobCard);
//
//        qualityCheck = repository.save(qualityCheck);
//
//        return mapper.toResponse(qualityCheck);
//
//    }

}