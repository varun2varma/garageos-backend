package com.garageos.modules.serviceworkflow.dto.response;

import com.garageos.modules.complaint.dto.response.ComplaintResponse;
import com.garageos.modules.customer.dto.response.CustomerResponse;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import com.garageos.modules.inspection.dto.response.InspectionResponse;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.vehicle.dto.response.VehicleResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResumeResponse {

    private WorkflowStatusResponse workflowStatus;

    private CustomerResponse customer;

    private VehicleResponse vehicle;

    private JobCardResponse job;

    private List<ComplaintResponse> complaints;

    private List<InspectionResponse> inspections;

    private EstimateResponse estimate;

    private List<EstimateItemResponse> estimateItems;

    private List<RepairTaskResponse> repairTasks;

    private InvoiceResponse invoice;

}