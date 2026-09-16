package com.garageos.modules.jobcard.dto.response;

import com.garageos.modules.complaint.dto.response.ComplaintResponse;
import com.garageos.modules.customer.dto.response.CustomerResponse;
import com.garageos.modules.delivery.dto.response.DeliveryResponse;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.jobassignment.dto.response.JobAssignmentResponse;
import com.garageos.modules.qualitycheck.dto.response.QualityCheckResponse;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.vehicle.dto.response.VehicleResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Server-computed, read-only GarageST JobCard projection
 * (GET /api/v1/jobcards/{id}/view). The caller's role, garage, customer
 * ownership and/or JobAssignment determine which of the section fields
 * below are populated (null = not visible to this caller) and what
 * {@code allowedActions} lists — the same shape the client renders,
 * never a broader payload the client is trusted to filter itself.
 */
@Getter
@Builder
public class JobCardViewResponse {

    private Long id;
    private String jobCardNumber;
    private String status;
    private int progress;

    private List<String> visibleSections;
    private List<String> allowedActions;

    private JobCardResponse header;
    private CustomerResponse customer;
    private VehicleResponse vehicle;
    private List<ComplaintResponse> complaints;
    private EstimateResponse estimate;
    private List<EstimateItemResponse> estimateItems;
    private List<RepairTaskResponse> repairTasks;
    private List<JobAssignmentResponse> assignments;
    private QualityCheckResponse qualityCheck;
    private InvoiceResponse invoice;
    private DeliveryResponse delivery;
}
