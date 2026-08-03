package com.garageos.modules.serviceworkflow.service.impl;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.complaint.dto.response.ComplaintResponse;
import com.garageos.modules.complaint.service.ComplaintService;
import com.garageos.modules.customer.dto.response.CustomerResponse;
import com.garageos.modules.customer.service.CustomerService;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import com.garageos.modules.estimateitem.service.EstimateItemService;
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
import com.garageos.modules.serviceworkflow.dto.response.WorkflowResumeResponse;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowStatusResponse;
import com.garageos.modules.serviceworkflow.service.ServiceWorkflowService;
import com.garageos.modules.vehicle.dto.response.VehicleResponse;
import com.garageos.modules.vehicle.service.VehicleService;
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
    private final CustomerService customerService;

    private final VehicleService vehicleService;

    private final ComplaintService complaintService;

    private final EstimateItemService estimateItemService;

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

//        jobCardService.closeJobCard(jobCardNumber);

        jobCardService.readyForDelivery(jobCardNumber);

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

    private int resolveStep(JobCardStatus status) {

        return switch (status) {

            case OPEN,
                    INSPECTION_PENDING -> 3;   // Inspection

            case INSPECTION_COMPLETED,
                    ESTIMATE_PENDING -> 4;     // Estimate

            case WAITING_FOR_APPROVAL -> 6; // Estimate Summary

            case ESTIMATE_APPROVED,
                    REPAIR_PENDING,
                    REPAIR_IN_PROGRESS -> 8;   // Repair

            case REPAIR_COMPLETED -> 9;     // Quality Check

            case QUALITY_CHECK,
                    READY_FOR_INVOICE -> 10;   // Invoice

            case INVOICE_GENERATED,
                    PAYMENT_PENDING -> 11;     // Payment

            case PAYMENT_COMPLETED,
                    READY_FOR_DELIVERY,
                    DELIVERED,
                    CLOSED -> 12;              // Delivery

            default -> 1;
        };
    }

    private int resolveProgress(JobCardStatus status){

        return switch(status){

            case OPEN, INSPECTION_PENDING -> 20;

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

            case PAYMENT_COMPLETED,
                    READY_FOR_DELIVERY -> 98;

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


    @Override
    @Transactional(readOnly = true)
    public WorkflowResumeResponse resumeWorkflow(
            String jobCardNumber) {

        JobCard jobCard = getJobCard(jobCardNumber);

        JobCardResponse job =
                jobCardService.getJobCardByNumber(jobCardNumber);

        CustomerResponse customer =
                customerService.getCustomer(jobCard.getCustomer().getId());

        VehicleResponse vehicle =
                vehicleService.getVehicle(jobCard.getVehicle().getId());

        List<ComplaintResponse> complaints =
                complaintService.getComplaints(jobCard.getId());

        List<InspectionResponse> inspections =
                inspectionService.getInspectionsByJobCard(jobCard.getId());

        EstimateResponse estimate = null;
        List<EstimateItemResponse> estimateItems = List.of();

        try {

            estimate =
                    estimateService.getEstimateByJobCard(jobCard.getId());

            estimateItems =
                    estimateItemService.getItems(estimate.getId());

        } catch (Exception ignored) {
        }

        List<RepairTaskResponse> repairTasks =
                repairTaskService.getRepairTasks(jobCard.getId());

        InvoiceResponse invoice = null;

        try {

            invoice =
                    invoiceService.getInvoiceByJobCard(jobCard.getId());

        } catch (Exception ignored) {
        }

        return WorkflowResumeResponse.builder()
                .workflowStatus(getWorkflowStatus(jobCardNumber))
                .customer(customer)
                .vehicle(vehicle)
                .job(job)
                .complaints(complaints)
                .inspections(inspections)
                .estimate(estimate)
                .estimateItems(estimateItems)
                .repairTasks(repairTasks)
                .invoice(invoice)
                .build();
    }

}