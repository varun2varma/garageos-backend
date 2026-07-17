package com.garageos.modules.serviceworkflow.service.impl;

import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.inspection.dto.request.CreateInspectionRequest;
import com.garageos.modules.inspection.dto.response.InspectionResponse;
import com.garageos.modules.inspection.service.InspectionService;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.invoice.service.InvoiceService;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.serviceworkflow.dto.response.WorkflowResponse;
import com.garageos.modules.serviceworkflow.service.ServiceWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceWorkflowServiceImpl
        implements ServiceWorkflowService {

    private final JobCardService jobCardService;
    private final InspectionService inspectionService;
    private final EstimateService estimateService;
    private final InvoiceService invoiceService;


    @Override
    public WorkflowResponse createJob(CreateJobCardRequest request) {

        JobCardResponse response =
                jobCardService.createJobCard(request);

        return WorkflowResponse.builder()
                .data(response)
                .message("Job created successfully.")
                .build();
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

        jobCardService.readyForDelivery(jobCardNumber);

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


}