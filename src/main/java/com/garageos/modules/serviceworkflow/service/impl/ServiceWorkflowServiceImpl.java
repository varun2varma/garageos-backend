package com.garageos.modules.serviceworkflow.service.impl;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.inspection.dto.request.CreateInspectionRequest;
import com.garageos.modules.inspection.dto.response.InspectionResponse;
import com.garageos.modules.inspection.service.InspectionService;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.invoice.service.InvoiceService;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.repairtask.service.RepairTaskService;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowResponse;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowStatusResponse;
import com.garageos.modules.serviceworkflow.service.ServiceWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceWorkflowServiceImpl
        implements ServiceWorkflowService {

    private final JobCardService jobCardService;
    private final InspectionService inspectionService;
    private final EstimateService estimateService;
    private final InvoiceService invoiceService;
    private final RepairTaskService repairTaskService;
    private final JobCardRepository jobCardRepository;


//    @Override
//    public WorkflowResponse createJob(CreateJobCardRequest request) {
//
//        JobCardResponse response =
//                jobCardService.createJobCard(request);
//
//        return WorkflowResponse.builder()
//                .data(response)
//                .message("Job created successfully.")
//                .build();
//    }

    @Override
    public WorkflowResponse createJob(CreateJobCardRequest request) {

        JobCardResponse response =
                jobCardService.createJobCard(request);

        System.out.println(response.getComplaints());

        WorkflowResponse workflowResponse =
                WorkflowResponse.builder()
                        .data(response)
                        .message("Job created successfully.")
                        .build();

        System.out.println(((JobCardResponse) workflowResponse.getData()).getComplaints());

        return workflowResponse;
    }

    @Override
    public WorkflowResponse startInspection(String jobCardNumber) {

        List<InspectionResponse> inspections =
                inspectionService.startInspection(jobCardNumber);

        jobCardService.startInspection(jobCardNumber);

        return WorkflowResponse.builder()
                .data(inspections)
                .message("Inspection started successfully.")
                .build();
    }

    @Override
    public WorkflowResponse completeInspection(String jobCardNumber) {
        return null;
    }

    @Override
    public WorkflowResponse completeInspection(
            String jobCardNumber,
            List<CreateInspectionRequest> request) {

        List<InspectionResponse> inspection =
                inspectionService.completeInspection(
                        jobCardNumber,
                        request);

        jobCardService.completeInspection(jobCardNumber);

        return WorkflowResponse.builder()
                .data(inspection)
                .message("Inspection completed successfully.")
                .build();
    }

    @Override
    public WorkflowResponse prepareEstimate(String jobCardNumber) {

        EstimateResponse estimate =
                estimateService.createEstimate(jobCardNumber);

        jobCardService.prepareEstimate(jobCardNumber);

        return WorkflowResponse.builder()
                .data(estimate)
                .message("Estimate prepared successfully.")
                .build();
    }

    @Override
    public WorkflowResponse approveEstimate(String jobCardNumber) {

        EstimateResponse estimate =
                estimateService.approveEstimate(jobCardNumber);

        jobCardService.approveEstimate(jobCardNumber);

        return WorkflowResponse.builder()
                .data(estimate)
                .message("Estimate approved successfully.")
                .build();
    }

    @Override
    public WorkflowResponse startRepair(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.startRepair(jobCardNumber))
                .message("Repair started successfully.")
                .build();
    }

    @Override
    public WorkflowResponse completeRepair(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.completeRepair(jobCardNumber))
                .message("Repair completed successfully.")
                .build();
    }

    @Override
    public WorkflowResponse performQualityCheck(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.performQualityCheck(jobCardNumber))
                .message("Quality check completed successfully.")
                .build();
    }

    @Override
    public WorkflowResponse readyForDelivery(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.readyForDelivery(jobCardNumber))
                .message("Vehicle is ready for delivery.")
                .build();
    }

    @Override
    public WorkflowResponse closeJob(String jobCardNumber) {

        return WorkflowResponse.builder()
                .data(jobCardService.closeJobCard(jobCardNumber))
                .message("Job closed successfully.")
                .build();
    }

    @Override
    public WorkflowResponse generateInvoice(
            String jobCardNumber) {

        InvoiceResponse invoice =
                invoiceService.generateInvoice(jobCardNumber);

        jobCardService.invoiceGenerated(jobCardNumber);

        return WorkflowResponse.builder()
                .data(invoice)
                .message("Invoice generated successfully.")
                .build();
    }

    @Override
    public WorkflowResponse receivePayment(
            String jobCardNumber) {

        InvoiceResponse invoice =
                invoiceService.receivePayment(jobCardNumber);

        jobCardService.closeJobCard(jobCardNumber);

        return WorkflowResponse.builder()
                .data(invoice)
                .message("Payment received successfully.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairTaskResponse> getRepairTasks(String jobCardNumber) {

        return repairTaskService.getRepairTasks(
                getJobCard(jobCardNumber).getId()
        );
    }

    private JobCard getJobCard(String jobCardNumber) {

        return jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found: " + jobCardNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowStatusResponse getWorkflowStatus(
            String jobCardNumber) {

        JobCard jobCard = getJobCard(jobCardNumber);

        return WorkflowStatusResponse.builder()
                .jobCardNumber(jobCard.getJobCardNumber())
                .status(jobCard.getStatus().name())
                .nextStep(resolveStep(jobCard.getStatus()))
                .progress(resolveProgress(jobCard.getStatus()))
                .completedSteps(resolveCompleted(jobCard.getStatus()))
                .build();

    }

    private int resolveStep(JobCardStatus status){

        return switch(status){

            case OPEN -> 1;

            case INSPECTION_PENDING -> 4;

            case INSPECTION_COMPLETED -> 5;

            case ESTIMATE_PENDING -> 5;

            case WAITING_FOR_APPROVAL -> 8;

            case ESTIMATE_APPROVED -> 9;

            case REPAIR_PENDING -> 9;

            case REPAIR_IN_PROGRESS -> 9;

            case REPAIR_COMPLETED -> 10;

            case QUALITY_CHECK -> 11;

            case READY_FOR_INVOICE -> 11;

            case INVOICE_GENERATED -> 12;

            case PAYMENT_PENDING -> 12;

            case PAYMENT_COMPLETED -> 13;

            case READY_FOR_DELIVERY -> 13;

            case DELIVERED -> 13;

            case CLOSED -> 13;

            default -> 1;

        };

    }

    private int resolveProgress(JobCardStatus status){

        return switch(status){

            case OPEN -> 5;

            case INSPECTION_PENDING -> 15;

            case INSPECTION_COMPLETED -> 25;

            case ESTIMATE_PENDING -> 35;

            case WAITING_FOR_APPROVAL -> 45;

            case ESTIMATE_APPROVED -> 55;

            case REPAIR_PENDING -> 60;

            case REPAIR_IN_PROGRESS -> 70;

            case REPAIR_COMPLETED -> 80;

            case QUALITY_CHECK -> 85;

            case READY_FOR_INVOICE -> 90;

            case INVOICE_GENERATED -> 95;

            case PAYMENT_COMPLETED -> 98;

            case CLOSED -> 100;

            default -> 0;

        };

    }

    private List<String> resolveCompleted(JobCardStatus status) {

        List<String> steps = new ArrayList<>();

        // These are always completed once a Job Card exists
        steps.add("CUSTOMER");
        steps.add("VEHICLE");
        steps.add("JOB_CARD");

        switch (status) {

            case OPEN:
            case INSPECTION_PENDING:
                break;

            case INSPECTION_COMPLETED:
            case ESTIMATE_PENDING:
                steps.add("INSPECTION");
                break;

            case WAITING_FOR_APPROVAL:
                steps.add("INSPECTION");
                steps.add("ESTIMATE");
                steps.add("ESTIMATE_ITEMS");
                steps.add("ESTIMATE_SUMMARY");
                break;

            case ESTIMATE_APPROVED:
            case REPAIR_PENDING:
            case REPAIR_IN_PROGRESS:
                steps.add("INSPECTION");
                steps.add("ESTIMATE");
                steps.add("ESTIMATE_ITEMS");
                steps.add("ESTIMATE_SUMMARY");
                steps.add("APPROVAL");
                break;

            case REPAIR_COMPLETED:
                steps.add("INSPECTION");
                steps.add("ESTIMATE");
                steps.add("ESTIMATE_ITEMS");
                steps.add("ESTIMATE_SUMMARY");
                steps.add("APPROVAL");
                steps.add("REPAIR");
                break;

            case QUALITY_CHECK:
            case READY_FOR_INVOICE:
                steps.add("INSPECTION");
                steps.add("ESTIMATE");
                steps.add("ESTIMATE_ITEMS");
                steps.add("ESTIMATE_SUMMARY");
                steps.add("APPROVAL");
                steps.add("REPAIR");
                steps.add("QUALITY_CHECK");
                break;

            case INVOICE_GENERATED:
            case INVOICED:
            case PAYMENT_PENDING:
                steps.add("INSPECTION");
                steps.add("ESTIMATE");
                steps.add("ESTIMATE_ITEMS");
                steps.add("ESTIMATE_SUMMARY");
                steps.add("APPROVAL");
                steps.add("REPAIR");
                steps.add("QUALITY_CHECK");
                steps.add("INVOICE");
                break;

            case PAYMENT_COMPLETED:
            case READY_FOR_DELIVERY:
                steps.add("INSPECTION");
                steps.add("ESTIMATE");
                steps.add("ESTIMATE_ITEMS");
                steps.add("ESTIMATE_SUMMARY");
                steps.add("APPROVAL");
                steps.add("REPAIR");
                steps.add("QUALITY_CHECK");
                steps.add("INVOICE");
                steps.add("PAYMENT");
                break;

            case DELIVERED:
            case CLOSED:
                steps.add("INSPECTION");
                steps.add("ESTIMATE");
                steps.add("ESTIMATE_ITEMS");
                steps.add("ESTIMATE_SUMMARY");
                steps.add("APPROVAL");
                steps.add("REPAIR");
                steps.add("QUALITY_CHECK");
                steps.add("INVOICE");
                steps.add("PAYMENT");
                steps.add("DELIVERY");
                break;

            default:
                break;
        }

        return steps;
    }

}