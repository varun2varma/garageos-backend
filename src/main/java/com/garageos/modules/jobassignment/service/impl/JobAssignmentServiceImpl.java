package com.garageos.modules.jobassignment.service.impl;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.jobassignment.dto.request.AssignJobRequest;
import com.garageos.modules.jobassignment.dto.request.CompleteJobRequest;
import com.garageos.modules.jobassignment.dto.request.ReassignJobRequest;
import com.garageos.modules.jobassignment.dto.request.StartJobRequest;
import com.garageos.modules.jobassignment.dto.response.JobAssignmentResponse;
import com.garageos.modules.jobassignment.dto.response.MyAssignmentResponse;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import com.garageos.modules.jobassignment.mapper.JobAssignmentMapper;
import com.garageos.modules.jobassignment.repository.JobAssignmentRepository;
import com.garageos.modules.jobassignment.service.JobAssignmentService;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class JobAssignmentServiceImpl implements JobAssignmentService {

    private final JobAssignmentRepository jobAssignmentRepository;

    private final JobAssignmentMapper jobAssignmentMapper;

    private final JobCardRepository jobCardRepository;

    private final EstimateItemRepository estimateItemRepository;

    private final UserRepository userRepository;

    private final GarageRepository garageRepository;

    @Override
    @Transactional
    public JobAssignmentResponse assignJob(
            AssignJobRequest request) {

        JobCard jobCard =
                jobCardRepository.findById(request.getJobCardId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Job Card not found : "
                                                + request.getJobCardId()));

        EstimateItem estimateItem =
                estimateItemRepository.findById(request.getEstimateItemId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Estimate Item not found : "
                                                + request.getEstimateItemId()));

        User user =
                userRepository.findById(request.getEmployeeId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found : "
                                                + request.getEmployeeId()));

        Garage garage =
                garageRepository.findById(
                                user.getGarageId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Garage not found"
                                ));

        JobAssignment assignment =
                new JobAssignment();

        assignment.setGarage(garage);

        assignment.setJobCard(jobCard);

        assignment.setEstimateItem(estimateItem);

        assignment.setUser(user);

        assignment.setAssignedAt(LocalDateTime.now());

        assignment.setEstimatedHours(
                request.getEstimatedHours());

        assignment.setRemarks(
                request.getRemarks());

        assignment.setStatus(
                JobAssignmentStatus.ASSIGNED);

        assignment =
                jobAssignmentRepository.save(assignment);

        return jobAssignmentMapper.toResponse(
                assignment);

    }

    @Override
    @Transactional
    public JobAssignmentResponse acceptJob(
            Long assignmentId) {

        JobAssignment assignment =
                getAssignmentOrThrow(assignmentId);

        assignment.setStatus(
                JobAssignmentStatus.ACCEPTED);

        assignment.setAcceptedAt(
                LocalDateTime.now());

        assignment =
                jobAssignmentRepository.save(
                        assignment);

        return jobAssignmentMapper.toResponse(
                assignment);

    }

    @Override
    @Transactional
    public JobAssignmentResponse startJob(
            Long assignmentId,
            StartJobRequest request) {

        JobAssignment assignment =
                getAssignmentOrThrow(assignmentId);

        assignment.setStatus(
                JobAssignmentStatus.IN_PROGRESS);

        assignment.setStartedAt(
                LocalDateTime.now());

        assignment.setRemarks(
                request.getRemarks());

        assignment =
                jobAssignmentRepository.save(
                        assignment);

        return jobAssignmentMapper.toResponse(
                assignment);

    }

    @Override
    @Transactional
    public JobAssignmentResponse completeJob(
            Long assignmentId,
            CompleteJobRequest request) {

        JobAssignment assignment =
                getAssignmentOrThrow(assignmentId);

        assignment.setStatus(
                JobAssignmentStatus.COMPLETED);

        assignment.setCompletedAt(
                LocalDateTime.now());

        assignment.setActualHours(
                request.getActualHours());

        assignment.setRemarks(
                request.getRemarks());

        assignment =
                jobAssignmentRepository.save(
                        assignment);

        return jobAssignmentMapper.toResponse(
                assignment);

    }

    private JobAssignment getAssignmentOrThrow(
            Long assignmentId) {

        return jobAssignmentRepository
                .findById(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found : "
                                        + assignmentId));

    }

    @Override
    @Transactional
    public JobAssignmentResponse reassignJob(
            Long assignmentId,
            ReassignJobRequest request) {

        JobAssignment assignment =
                getAssignmentOrThrow(assignmentId);

        User user =
                userRepository.findById(request.getEmployeeId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found : "
                                                + request.getEmployeeId()));

        assignment.setUser(user);

        assignment.setAssignedAt(LocalDateTime.now());

        assignment.setRemarks(request.getRemarks());

        assignment.setStatus(JobAssignmentStatus.ASSIGNED);

        assignment =
                jobAssignmentRepository.save(assignment);

        return jobAssignmentMapper.toResponse(assignment);

    }

    @Override
    @Transactional(readOnly = true)
    public JobAssignmentResponse getAssignment(Long assignmentId) {

        JobAssignment assignment =
                getAssignmentOrThrow(assignmentId);

        return jobAssignmentMapper.toResponse(assignment);

    }

    @Override
    @Transactional(readOnly = true)
    public List<JobAssignmentResponse> getAssignmentsByJobCard(
            Long jobCardId) {

        List<JobAssignment> assignments =
                jobAssignmentRepository.findByJobCardId(jobCardId);

        return jobAssignmentMapper.toResponse(assignments);

    }

    @Override
    @Transactional(readOnly = true)
    public List<MyAssignmentResponse> getMyAssignments(Long userId) {

//        Long userId = 1L; // Temporary until JWT integration

        List<JobAssignment> assignments =
                jobAssignmentRepository.findByUserId(userId);

        return jobAssignmentMapper.toMyAssignment(assignments);

    }

}