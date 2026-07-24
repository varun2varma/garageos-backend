package com.garageos.modules.serviceworkflow.service;

import com.garageos.modules.inspection.dto.request.CreateInspectionRequest;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowResponse;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowStatusResponse;

import java.util.List;

public interface ServiceWorkflowService {

    WorkflowResponse createJob(CreateJobCardRequest request);

    WorkflowResponse startInspection(String jobCardNumber);

    WorkflowResponse completeInspection(String jobCardNumber);

    WorkflowResponse prepareEstimate(String jobCardNumber);

    WorkflowResponse approveEstimate(String jobCardNumber);

    WorkflowResponse startRepair(String jobCardNumber);

    WorkflowResponse completeRepair(String jobCardNumber);

    WorkflowResponse performQualityCheck(String jobCardNumber);

    WorkflowResponse readyForDelivery(String jobCardNumber);

    WorkflowResponse closeJob(String jobCardNumber);

    WorkflowResponse completeInspection(
            String jobCardNumber,
            List<CreateInspectionRequest> request);
    WorkflowResponse generateInvoice(String jobCardNumber);
    WorkflowResponse receivePayment(String jobCardNumber);
    List<RepairTaskResponse> getRepairTasks(String jobCardNumber);
    WorkflowStatusResponse getWorkflowStatus(
            String jobCardNumber);
}