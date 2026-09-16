package com.garageos.modules.serviceworkflow.service.impl;

import com.garageos.modules.complaint.service.ComplaintService;
import com.garageos.modules.customer.service.CustomerService;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.estimateitem.service.EstimateItemService;
import com.garageos.modules.inspection.dto.response.InspectionResponse;
import com.garageos.modules.inspection.service.InspectionService;
import com.garageos.modules.invoice.service.InvoiceService;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.repairtask.service.RepairTaskService;
import com.garageos.modules.vehicle.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Covers the startInspection() orphan-safety fix: this orchestrator now
 * carries @Transactional so inspectionService.startInspection() (which
 * persists Inspection rows) and jobCardService.startInspection() (which
 * transitions the JobCard) share one transaction. A pure Mockito test
 * cannot observe an actual DB rollback - it can only prove the failure
 * is not swallowed here, which is what makes Spring's rollback trigger.
 * Actual DB-level atomicity was confirmed separately against a live
 * Postgres instance (see the E2E verification report).
 */
@ExtendWith(MockitoExtension.class)
class ServiceWorkflowServiceImplTest {

    @Mock private JobCardService jobCardService;
    @Mock private InspectionService inspectionService;
    @Mock private EstimateService estimateService;
    @Mock private InvoiceService invoiceService;
    @Mock private RepairTaskService repairTaskService;
    @Mock private JobCardRepository jobCardRepository;
    @Mock private CustomerService customerService;
    @Mock private VehicleService vehicleService;
    @Mock private ComplaintService complaintService;
    @Mock private EstimateItemService estimateItemService;

    private ServiceWorkflowServiceImpl service() {
        return new ServiceWorkflowServiceImpl(
                jobCardService,
                inspectionService,
                estimateService,
                invoiceService,
                repairTaskService,
                jobCardRepository,
                customerService,
                vehicleService,
                complaintService,
                estimateItemService
        );
    }

    @Test
    void startInspectionMethod_isAnnotatedTransactional() throws NoSuchMethodException {

        Method m = ServiceWorkflowServiceImpl.class.getMethod("startInspection", String.class);
        assertThat(m.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class))
                .as("startInspection must be transactional so the Inspection insert and the " +
                        "JobCard status transition commit/rollback together")
                .isTrue();
    }

    @Test
    void startInspection_success_callsInspectionThenJobCardTransition() {

        when(inspectionService.startInspection("JC-1")).thenReturn(List.of(mock(InspectionResponse.class)));

        service().startInspection("JC-1");

        InOrder order = inOrder(inspectionService, jobCardService);
        order.verify(inspectionService).startInspection("JC-1");
        order.verify(jobCardService).startInspection("JC-1");
    }

    @Test
    void startInspection_jobCardTransitionFailure_isNotSwallowed() {

        when(inspectionService.startInspection("JC-2")).thenReturn(List.of(mock(InspectionResponse.class)));
        doThrow(new IllegalStateException("Invalid Job Card status transition"))
                .when(jobCardService).startInspection("JC-2");

        assertThatThrownBy(() -> service().startInspection("JC-2"))
                .isInstanceOf(IllegalStateException.class);
    }
}
