package com.garageos.modules.jobcard.service.impl;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.complaint.dto.response.ComplaintResponse;
import com.garageos.modules.complaint.service.ComplaintService;
import com.garageos.modules.customer.dto.response.CustomerResponse;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.customer.service.CustomerService;
import com.garageos.modules.delivery.dto.response.DeliveryResponse;
import com.garageos.modules.delivery.mapper.DeliveryMapper;
import com.garageos.modules.delivery.repository.DeliveryRepository;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import com.garageos.modules.estimateitem.service.EstimateItemService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.invoice.service.InvoiceService;
import com.garageos.modules.jobassignment.dto.response.JobAssignmentResponse;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import com.garageos.modules.jobassignment.repository.JobAssignmentRepository;
import com.garageos.modules.jobassignment.service.JobAssignmentService;
import com.garageos.modules.jobcard.dto.response.JobCardViewResponse;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.mapper.JobCardMapper;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.service.JobCardProjectionService;
import com.garageos.modules.qualitycheck.dto.response.QualityCheckResponse;
import com.garageos.modules.qualitycheck.entity.QualityCheck;
import com.garageos.modules.qualitycheck.mapper.QualityCheckMapper;
import com.garageos.modules.qualitycheck.repository.QualityCheckRepository;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;
import com.garageos.modules.repairtask.service.RepairTaskService;
import com.garageos.modules.vehicle.dto.response.VehicleResponse;
import com.garageos.modules.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-computed JobCard projection (Phase H of GarageST JobCard
 * Architecture v1). Authorization happens once, here, rather than being
 * re-implemented at every future client call site: garage scope for
 * employees, JobAssignment for technicians, ownership for customers.
 *
 * Scope note (reported, not silently omitted): OPERATIONAL viewer
 * (Manager/Owner/Service Advisor and, conservatively, every other
 * garage-matched non-technician employee role not given a narrower
 * locked definition in this task) currently all receive the same full
 * section/action visibility. Finer per-role slicing for
 * Accountant/Cashier/Inventory Manager/Driver (e.g. Accountant seeing
 * only financial sections) was not specified in the locked architecture
 * for this phase and was not implemented — see completion report.
 * Media is intentionally not embedded as a projection section; existing
 * MediaController endpoints remain the client's media access path.
 */
@Service
@RequiredArgsConstructor
public class JobCardProjectionServiceImpl implements JobCardProjectionService {

    private final JobCardRepository jobCardRepository;
    private final JobCardMapper jobCardMapper;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private final ComplaintService complaintService;
    private final EstimateService estimateService;
    private final EstimateItemService estimateItemService;
    private final RepairTaskService repairTaskService;
    private final JobAssignmentService jobAssignmentService;
    private final JobAssignmentRepository jobAssignmentRepository;
    private final QualityCheckRepository qualityCheckRepository;
    private final QualityCheckMapper qualityCheckMapper;
    private final InvoiceService invoiceService;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryMapper deliveryMapper;

    private enum Viewer { CUSTOMER, TECHNICIAN, OPERATIONAL }

    @Override
    @Transactional(readOnly = true)
    public JobCardViewResponse getJobCardView(Long jobCardId) {

        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found with id : " + jobCardId));

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        Viewer viewer = resolveViewer(jobCard, principal);

        JobCardViewResponse.JobCardViewResponseBuilder builder =
                JobCardViewResponse.builder()
                        .id(jobCard.getId())
                        .jobCardNumber(jobCard.getJobCardNumber())
                        .status(jobCard.getStatus().name())
                        .progress(resolveProgress(jobCard.getStatus()))
                        .header(jobCardMapper.toResponse(jobCard));

        List<String> visibleSections = new ArrayList<>();
        List<String> allowedActions = new ArrayList<>();

        EstimateResponse estimate =
                estimateService.getEstimateByJobCard(jobCard.getId());

        InvoiceResponse invoice =
                invoiceService.getInvoiceByJobCard(jobCard.getId());

        switch (viewer) {

            case CUSTOMER -> {

                visibleSections.addAll(List.of(
                        "header", "vehicle", "complaints",
                        "estimate", "invoice", "payment", "delivery",
                        "timeline"));

                builder.vehicle(vehicleResponseOrNull(jobCard))
                        .complaints(complaintService.getComplaints(jobCard.getId()))
                        .estimate(estimate)
                        .invoice(invoice);

                if (estimate != null
                        && EstimateStatus.WAITING_FOR_APPROVAL.name().equals(estimate.getStatus())) {
                    allowedActions.add("approve_estimate");
                    allowedActions.add("reject_estimate");
                }

                allowedActions.add("view");
            }

            case TECHNICIAN -> {

                visibleSections.addAll(List.of(
                        "header", "vehicle", "assignments", "repairTasks"));

                List<RepairTaskResponse> allTasks =
                        repairTaskService.getRepairTasks(jobCard.getId());

                List<RepairTaskResponse> ownTasks = allTasks.stream()
                        .filter(task -> task.getJobAssignmentId() != null)
                        .filter(task -> isOwnAssignment(
                                task.getJobAssignmentId(), principal.getId()))
                        .toList();

                // Corrective fix for audit Finding P1: a technician must
                // only see their own assignment records on this JobCard,
                // not every technician's — mirrors the repairTasks filter
                // immediately above, which was already correctly scoped.
                List<JobAssignmentResponse> ownAssignments =
                        jobAssignmentService
                                .getAssignmentsByJobCard(jobCard.getId())
                                .stream()
                                .filter(a -> a.getEmployeeId() != null
                                        && a.getEmployeeId().equals(principal.getId()))
                                .toList();

                builder.vehicle(vehicleResponseOrNull(jobCard))
                        .repairTasks(ownTasks)
                        .assignments(ownAssignments);

                allowedActions.add("view");
                allowedActions.add("start_repair");
                allowedActions.add("complete_repair");
            }

            default -> {

                visibleSections.addAll(List.of(
                        "header", "customer", "vehicle", "complaints",
                        "estimate", "assignments", "repairTasks",
                        "qc", "invoice", "payment", "delivery",
                        "timeline"));

                Customer customer = jobCard.getCustomer();

                builder.customer(customer == null
                                ? null
                                : customerService.getCustomer(customer.getId()))
                        .vehicle(vehicleResponseOrNull(jobCard))
                        .complaints(complaintService.getComplaints(jobCard.getId()))
                        .estimate(estimate)
                        .estimateItems(estimate == null
                                ? List.of()
                                : estimateItemService.getItems(estimate.getId()))
                        .repairTasks(repairTaskService.getRepairTasks(jobCard.getId()))
                        .assignments(jobAssignmentService
                                .getAssignmentsByJobCard(jobCard.getId()))
                        .qualityCheck(qualityCheckResponseOrNull(jobCard.getId()))
                        .invoice(invoice)
                        .delivery(deliveryResponseOrNull(jobCard.getId()));

                allowedActions.addAll(resolveOperationalActions(jobCard.getStatus()));
            }
        }

        builder.visibleSections(visibleSections);
        builder.allowedActions(allowedActions);

        return builder.build();
    }

    private Viewer resolveViewer(
            JobCard jobCard,
            GarageUserPrincipal principal) {

        if (principal.getRoles().contains(RoleCode.CUSTOMER.name())) {

            Customer customer = customerRepository
                    .findByMobileNumber(principal.getMobile())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Job Card not found with id : "
                                            + jobCard.getId()));

            if (jobCard.getCustomer() == null
                    || !jobCard.getCustomer().getId().equals(customer.getId())) {
                throw new ResourceNotFoundException(
                        "Job Card not found with id : " + jobCard.getId());
            }

            return Viewer.CUSTOMER;
        }

        if (jobCard.getGarage() == null
                || principal.getGarageId() == null
                || !jobCard.getGarage().getId().equals(principal.getGarageId())) {

            throw new ResourceNotFoundException(
                    "Job Card not found with id : " + jobCard.getId());
        }

        boolean isTechnicianOnly =
                principal.getRoles().contains(RoleCode.TECHNICIAN.name())
                        && !principal.getRoles().contains(RoleCode.MANAGER.name())
                        && !principal.getRoles().contains(RoleCode.OWNER.name())
                        && !principal.getRoles().contains(RoleCode.SERVICE_ADVISOR.name());

        if (isTechnicianOnly) {

            boolean assigned = jobAssignmentRepository
                    .findByJobCardId(jobCard.getId())
                    .stream()
                    .anyMatch(a ->
                            a.getUser().getId().equals(principal.getId())
                                    && a.getStatus() != JobAssignmentStatus.CANCELLED);

            if (!assigned) {
                throw new ResourceNotFoundException(
                        "Job Card not found with id : " + jobCard.getId());
            }

            return Viewer.TECHNICIAN;
        }

        return Viewer.OPERATIONAL;
    }

    private boolean isOwnAssignment(Long jobAssignmentId, Long userId) {

        return jobAssignmentRepository.findById(jobAssignmentId)
                .map(a -> a.getUser().getId().equals(userId))
                .orElse(false);
    }

    private VehicleResponse vehicleResponseOrNull(JobCard jobCard) {

        return jobCard.getVehicle() == null
                ? null
                : vehicleService.getVehicle(jobCard.getVehicle().getId());
    }

    private QualityCheckResponse qualityCheckResponseOrNull(Long jobCardId) {

        return qualityCheckRepository.findByJobCardId(jobCardId)
                .map(qualityCheckMapper::toResponse)
                .orElse(null);
    }

    private DeliveryResponse deliveryResponseOrNull(Long jobCardId) {

        return deliveryRepository.findByJobCardId(jobCardId)
                .map(deliveryMapper::toResponse)
                .orElse(null);
    }

    private List<String> resolveOperationalActions(JobCardStatus status) {

        return switch (status) {

            case OPEN -> List.of("start_inspection");
            case INSPECTION_PENDING -> List.of("complete_inspection");
            case INSPECTION_COMPLETED -> List.of("prepare_estimate");
            case ESTIMATE_PENDING -> List.of("send_estimate_for_approval");
            case WAITING_FOR_APPROVAL -> List.of("assign_technician");
            case REPAIR_PENDING -> List.of("start_repair", "assign_technician", "reassign_technician");
            case REPAIR_IN_PROGRESS -> List.of("assign_technician", "reassign_technician");
            case REPAIR_COMPLETED -> List.of("pass_quality_check", "fail_quality_check");
            case READY_FOR_INVOICE -> List.of("generate_invoice");
            case INVOICE_GENERATED -> List.of("record_payment");
            case READY_FOR_DELIVERY -> List.of("complete_delivery");
            case DELIVERED -> List.of("close_job_card");
            default -> List.of();
        };
    }

    private int resolveProgress(JobCardStatus status) {

        return switch (status) {

            case OPEN, INSPECTION_PENDING -> 10;
            case INSPECTION_COMPLETED -> 20;
            case ESTIMATE_PENDING -> 30;
            case WAITING_FOR_APPROVAL -> 40;
            case REPAIR_PENDING -> 50;
            case REPAIR_IN_PROGRESS -> 60;
            case REPAIR_COMPLETED -> 70;
            case READY_FOR_INVOICE -> 80;
            case INVOICE_GENERATED -> 85;
            case READY_FOR_DELIVERY -> 90;
            case DELIVERED -> 95;
            case CLOSED -> 100;
            default -> 0;
        };
    }
}
