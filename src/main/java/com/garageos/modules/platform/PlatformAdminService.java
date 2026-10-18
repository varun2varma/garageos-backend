package com.garageos.modules.platform;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.modules.garage.entity.Garage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only platform administration data for SUPER_ADMIN. Aggregates and a minimal garage directory only: no
 * customer, vehicle, owner-contact or financial-line detail leaves a tenant through here (data minimisation).
 * Only metrics the database actually holds are returned; everything else is listed in {@code unavailable}.
 */
@Service
@RequiredArgsConstructor
public class PlatformAdminService {

    static final List<JobCardStatus> FINISHED =
            List.of(JobCardStatus.CLOSED, JobCardStatus.DELIVERED, JobCardStatus.CANCELLED);

    private final EntityManager em;

    /** Optional so existing constructor-built tests keep their shape; always present at runtime. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AuditService auditService;

    /** Everything a platform admin may see about ONE garage: profile facts and counts, never people or money lines. */
    public record GarageDetail(
            Long id, String garageCode, String garageName, String workshopType, Integer numberOfBays,
            String address, String city, String state, String status, LocalDateTime createdAt,
            long activeStaff, Map<String, Long> jobCardsByStatus, long openJobCards,
            Map<String, Long> invoicesByPaymentStatus, long invoices,
            boolean hasAuthorizedSignatureAndStamp, boolean hasGstNumber) {
    }

    public record Overview(
            Map<String, Long> garagesByStatus,
            long totalGarages,
            long totalJobCards,
            long openJobCards,
            Map<String, Long> jobCardsByStatus,
            long totalInvoices,
            Map<String, Long> invoicesByPaymentStatus,
            List<String> unavailable) {
    }

    public record GarageRow(
            Long id, String garageCode, String garageName, String city, String state, String status,
            LocalDateTime createdAt, long openJobCards, long invoices) {
    }

    @Transactional(readOnly = true)
    public Overview overview() {

        Map<String, Long> garages = new HashMap<>();
        for (GarageStatus s : GarageStatus.values()) {
            garages.put(s.name(), 0L);
        }
        for (Object[] row : em.createQuery("select g.status, count(g) from Garage g group by g.status", Object[].class)
                .getResultList()) {
            garages.put(((GarageStatus) row[0]).name(), (Long) row[1]);
        }

        Map<String, Long> jobs = new HashMap<>();
        long total = 0;
        long open = 0;
        for (Object[] row : em.createQuery("select j.status, count(j) from JobCard j group by j.status", Object[].class)
                .getResultList()) {
            JobCardStatus s = (JobCardStatus) row[0];
            long n = (Long) row[1];
            jobs.put(s.name(), n);
            total += n;
            if (!FINISHED.contains(s)) {
                open += n;
            }
        }

        Map<String, Long> invoices = new HashMap<>();
        long invoiceTotal = 0;
        for (Object[] row : em.createQuery("select i.paymentStatus, count(i) from Invoice i group by i.paymentStatus",
                Object[].class).getResultList()) {
            String key = row[0] == null ? "UNKNOWN" : ((PaymentStatus) row[0]).name();
            invoices.put(key, (Long) row[1]);
            invoiceTotal += (Long) row[1];
        }

        return new Overview(garages, garages.values().stream().mapToLong(Long::longValue).sum(), total, open, jobs,
                invoiceTotal, invoices,
                List.of("Platform fees", "Settlement and payouts to garages", "Payment-provider transactions",
                        "Reconciliation status"));
    }

    @Transactional(readOnly = true)
    public Page<GarageRow> garages(String query, GarageStatus status, int page, int size) {

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        String where = " where (:status is null or g.status = :status)"
                + " and (:q is null or lower(g.garageName) like :q or lower(g.city) like :q or lower(g.garageCode) like :q)";
        String like = query == null || query.isBlank() ? null : "%" + query.trim().toLowerCase() + "%";

        TypedQuery<Garage> list = em.createQuery("select g from Garage g" + where + " order by g.garageName, g.id", Garage.class);
        TypedQuery<Long> count = em.createQuery("select count(g) from Garage g" + where, Long.class);
        for (TypedQuery<?> q : List.of(list, count)) {
            q.setParameter("status", status);
            q.setParameter("q", like);
        }
        list.setFirstResult(safePage * safeSize);
        list.setMaxResults(safeSize);

        List<Garage> garages = list.getResultList();
        Map<Long, Long> openByGarage = counts(garages,
                "select j.garage.id, count(j) from JobCard j where j.garage.id in :ids and j.status not in :done group by j.garage.id", true);
        Map<Long, Long> invoicesByGarage = counts(garages,
                "select i.estimate.jobCard.garage.id, count(i) from Invoice i where i.estimate.jobCard.garage.id in :ids group by i.estimate.jobCard.garage.id", false);

        List<GarageRow> rows = garages.stream()
                .map(g -> new GarageRow(g.getId(), g.getGarageCode(), g.getGarageName(), g.getCity(), g.getState(),
                        g.getStatus() == null ? null : g.getStatus().name(), g.getCreatedAt(),
                        openByGarage.getOrDefault(g.getId(), 0L), invoicesByGarage.getOrDefault(g.getId(), 0L)))
                .toList();

        return new PageImpl<>(rows, PageRequest.of(safePage, safeSize), count.getSingleResult());
    }

    @Transactional(readOnly = true)
    public GarageDetail garage(Long id) {

        Garage g = em.find(Garage.class, id);

        if (g == null) {
            throw new ResourceNotFoundException("Garage not found.");
        }

        Map<String, Long> jobs = new HashMap<>();
        long open = 0;
        for (Object[] row : em.createQuery(
                "select j.status, count(j) from JobCard j where j.garage.id = :id group by j.status", Object[].class)
                .setParameter("id", id).getResultList()) {
            JobCardStatus s = (JobCardStatus) row[0];
            jobs.put(s.name(), (Long) row[1]);
            if (!FINISHED.contains(s)) {
                open += (Long) row[1];
            }
        }

        Map<String, Long> invoices = new HashMap<>();
        long invoiceTotal = 0;
        for (Object[] row : em.createQuery(
                "select i.paymentStatus, count(i) from Invoice i where i.estimate.jobCard.garage.id = :id group by i.paymentStatus",
                Object[].class).setParameter("id", id).getResultList()) {
            invoices.put(row[0] == null ? "UNKNOWN" : ((PaymentStatus) row[0]).name(), (Long) row[1]);
            invoiceTotal += (Long) row[1];
        }

        long staff = em.createQuery(
                "select count(m) from GarageMembership m where m.garage.id = :id and m.status = :st", Long.class)
                .setParameter("id", id)
                .setParameter("st", com.garageos.core.enums.garagemembership.GarageMembershipStatus.ACTIVE)
                .getSingleResult();

        return new GarageDetail(g.getId(), g.getGarageCode(), g.getGarageName(),
                g.getWorkshopType() == null ? null : g.getWorkshopType().name(), g.getNumberOfBays(),
                g.getAddress(), g.getCity(), g.getState(), g.getStatus() == null ? null : g.getStatus().name(),
                g.getCreatedAt(), staff, jobs, open, invoices, invoiceTotal,
                g.getSignatureStorageKey() != null && g.getStampStorageKey() != null,
                g.getGstNumber() != null && !g.getGstNumber().isBlank());
    }

    /**
     * Activates or deactivates a garage. A reason is mandatory and the change is written to the audit trail with the
     * previous and new status. Only the status flag changes; no data is deleted.
     */
    @Transactional
    public GarageDetail changeStatus(Long id, GarageStatus newStatus, String reason, Long actorId) {

        if (newStatus == null) {
            throw new BusinessException("A status is required.");
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A reason is required to change a garage's status.");
        }

        Garage g = em.find(Garage.class, id);

        if (g == null) {
            throw new ResourceNotFoundException("Garage not found.");
        }

        GarageStatus previous = g.getStatus();

        if (previous != newStatus) {
            g.setStatus(newStatus);
            em.flush();

            if (auditService != null) {
                auditService.record(AuditEventType.GARAGE_STATUS_CHANGED, "GARAGE", g.getId(), g.getId(),
                        Map.of("from", String.valueOf(previous), "to", newStatus.name(), "reason", reason.trim(),
                                "actorUserId", String.valueOf(actorId)));
            }
        }

        return garage(id);
    }

    private Map<Long, Long> counts(List<Garage> garages, String jpql, boolean withDone) {

        Map<Long, Long> out = new HashMap<>();
        if (garages.isEmpty()) {
            return out;
        }
        TypedQuery<Object[]> q = em.createQuery(jpql, Object[].class)
                .setParameter("ids", garages.stream().map(Garage::getId).toList());
        if (withDone) {
            q.setParameter("done", FINISHED);
        }
        for (Object[] row : q.getResultList()) {
            out.put((Long) row[0], (Long) row[1]);
        }
        return out;
    }
}
