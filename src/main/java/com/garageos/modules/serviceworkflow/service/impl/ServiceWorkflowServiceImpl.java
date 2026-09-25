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
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.qualitycheck.service.QualityCheckService;
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
    private final JobCardStatusValidator statusValidator;
    private final QualityCheckService qualityCheckService;
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
    @Transactional
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

    /**
     * Corrective fix: this orchestrates two independently-transactional
     * writes (InspectionServiceImpl.completeInspection, which saves every
     * Inspection row, then JobCardServiceImpl.completeInspection, which
     * validates and transitions JobCardStatus) but was not itself
     * @Transactional. Without a shared transaction boundary, the first
     * call's writes commit on its own method return; if the JobCard
     * transition then throws (e.g. the JobCard was not actually at
     * INSPECTION_PENDING), the customer's "Save All Inspections" action
     * ends up with every inspection finding persisted as COMPLETED while
     * the JobCard itself never advances - an inconsistent, silently
     * half-applied state a retry cannot cleanly undo. Wrapping both calls
     * in one transaction makes the whole action atomic: either both
     * succeed, or neither is persisted.
     */
    @Override
    @Transactional
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

//    @Override
//    public WorkflowResponse prepareEstimate(String jobCardNumber) {
//
//        EstimateResponse estimate =
//                estimateService.createEstimate(jobCardNumber);
//
//        jobCardService.prepareEstimate(jobCardNumber);
//
//        return WorkflowResponse.builder()
//                .data(estimate)
//                .message("Estimate prepared successfully.")
//                .build();
//    }

    @Override
    public WorkflowResponse prepareEstimate(String jobCardNumber) {

        jobCardService.prepareEstimate(jobCardNumber);

        return WorkflowResponse.builder()
                .message("Estimate stage prepared successfully.")
                .build();
    }

    @Override
    public WorkflowResponse approveEstimate(String jobCardNumber) {

        // EstimateServiceImpl.approveEstimate(String) is now the
        // canonical operation: it approves the Estimate, creates the
        // RepairTasks, and transitions the JobCard to REPAIR_PENDING
        // atomically, so no separate JobCard-side call is needed here.
        EstimateResponse estimate =
                estimateService.approveEstimate(jobCardNumber);

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
    @Transactional
    public WorkflowResponse performQualityCheck(String jobCardNumber) {

        JobCard jobCard = getJobCard(jobCardNumber);

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.QUALITY_CHECK
        );

        jobCard.setStatus(JobCardStatus.QUALITY_CHECK);
        jobCardRepository.save(jobCard);

        qualityCheckService.createQualityCheck(jobCard);

        return WorkflowResponse.builder()
                .data(jobCardService.getJobCardByNumber(jobCardNumber))
                .message("Quality check started successfully.")
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
    public InvoiceResponse generateInvoice(
            String jobCardNumber) {

        // InvoiceServiceImpl.generateInvoice(String) now transitions the
        // JobCard to INVOICE_GENERATED internally (canonical operation),
        // so no separate JobCard-side call is needed here.
        return invoiceService.generateInvoice(jobCardNumber);
    }

    @Override
    public InvoiceResponse acceptInvoice(
            String jobCardNumber) {

        return invoiceService.acceptInvoice(jobCardNumber);
    }

    @Override
    public WorkflowResponse receivePayment(
            String jobCardNumber) {

        // InvoiceServiceImpl.receivePayment(String) now transitions the
        // JobCard to READY_FOR_DELIVERY internally, atomically with the
        // payment write, once it is validated as a legal transition.
        InvoiceResponse invoice =
                invoiceService.receivePayment(jobCardNumber);

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

        // These are always completed once a Job Card exists.
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

            /*
             * Repair work is technically completed, but the Manager still
             * needs to review the completed repair work/evidence and explicitly
             * proceed to Quality Check.
             *
             * Therefore REPAIR is intentionally NOT marked completed here.
             * WorkflowController will resolve the Repair screen.
             */
            case REPAIR_COMPLETED:
                steps.add("INSPECTION");
                steps.add("ESTIMATE");
                steps.add("ESTIMATE_ITEMS");
                steps.add("ESTIMATE_SUMMARY");
                steps.add("APPROVAL");
                break;

            /*
             * Quality Check has started and is currently pending.
             *
             * Repair is now completed because the Manager explicitly proceeded
             * from REPAIR_COMPLETED to QUALITY_CHECK.
             *
             * QUALITY_CHECK itself is NOT completed until PASS.
             */
            case QUALITY_CHECK:
                steps.add("INSPECTION");
                steps.add("ESTIMATE");
                steps.add("ESTIMATE_ITEMS");
                steps.add("ESTIMATE_SUMMARY");
                steps.add("APPROVAL");
                steps.add("REPAIR");
                break;

            /*
             * QC passed. Invoice is now the next active stage.
             */
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

        EstimateResponse estimate =
                estimateService.getEstimateByJobCard(jobCard.getId());

        List<EstimateItemResponse> estimateItems = List.of();

        if (estimate != null && estimate.getId() != null) {
            estimateItems =
                    estimateItemService.getItems(estimate.getId());
        }

        List<RepairTaskResponse> repairTasks =
                repairTaskService.getRepairTasks(jobCard.getId());

        InvoiceResponse invoice =
                invoiceService.getInvoiceByJobCard(jobCard.getId());

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