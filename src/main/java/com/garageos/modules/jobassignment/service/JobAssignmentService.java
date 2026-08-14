package com.garageos.modules.jobassignment.service;

import com.garageos.modules.jobassignment.dto.request.AssignJobRequest;
import com.garageos.modules.jobassignment.dto.request.CompleteJobRequest;
import com.garageos.modules.jobassignment.dto.request.ReassignJobRequest;
import com.garageos.modules.jobassignment.dto.request.StartJobRequest;
import com.garageos.modules.jobassignment.dto.response.JobAssignmentResponse;
import com.garageos.modules.jobassignment.dto.response.MyAssignmentResponse;

import java.util.List;

public interface JobAssignmentService {

    JobAssignmentResponse assignJob(AssignJobRequest request);

    JobAssignmentResponse reassignJob(
            Long assignmentId,
            ReassignJobRequest request
    );

    JobAssignmentResponse getAssignment(Long assignmentId);

    List<JobAssignmentResponse> getAssignmentsByJobCard(Long jobCardId);

    List<MyAssignmentResponse> getMyAssignments(Long userId);

    List<MyAssignmentResponse> getMyDriverAssignments(Long userId);

    JobAssignmentResponse acceptJob(Long assignmentId, Long userId);

    JobAssignmentResponse startJob(
            Long assignmentId,
            StartJobRequest request,
            Long userId
    );

    JobAssignmentResponse completeJob(
            Long assignmentId,
            CompleteJobRequest request,
            Long userId
    );

}