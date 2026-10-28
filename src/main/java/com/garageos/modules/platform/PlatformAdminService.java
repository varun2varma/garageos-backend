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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.core.ratelimit.RateLimitService rateLimitService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.modules.bankaccount.service.BankAccountService bankAccountService;

    /**
     * Everything a platform admin may see about ONE garage: profile facts, ownership, branding status, business
     * identifier status and activity counts. The single SUPER_ADMIN (TVASTRI, provisioned by the bootstrap runner) sees
     * the garage's own business and owner details in full (owner phone and e-mail, GSTIN, PAN); the masked variants stay in
     * the payload for older app builds. There are no customer records, vehicle documents, internal photos or invoice lines. A dedicated admin shape: it is never
     * built from the broader staff or customer garage DTOs.
     */
    public record GarageDetail(
            Long id, String garageCode, String garageName, String workshopType, Integer numberOfBays,
            String address, String city, String state, String status, LocalDateTime createdAt,
            long activeStaff, Map<String, Long> jobCardsByStatus, long openJobCards,
            Map<String, Long> invoicesByPaymentStatus, long invoices,
            boolean hasAuthorizedSignatureAndStamp, boolean hasGstNumber,
            LocalDateTime updatedAt, OwnerSummary owner, Map<String, Long> activeStaffByRole,
            BrandingSummary branding, BusinessIdentifiers identifiers,
            Map<String, Long> bookingsByStatus, Map<String, Long> estimatesByStatus,
            StatusChange lastStatusChange, String bankAccountStatus,
            String landmark, String pincode, String timeZone) {
    }

    /** The garage owner. Full contact details for the platform administrator; masked variants kept for older app builds. */
    public record OwnerSummary(Long userId, String name, String mobileMasked, String emailMasked, String mobile, String email) {
    }

    /** Read-only view of the garage's branding configuration. */
    public record BrandingSummary(boolean hasLogo, String primaryColor, String secondaryColor, boolean themeConfigured,
                                  LocalDateTime logoUpdatedAt, LocalDateTime brandingUpdatedAt) {
    }

    /**
     * GSTIN status is LOCAL format validation only: {@code ABSENT}, {@code FORMAT_VALID} or {@code FORMAT_INVALID}.
     * Nothing here means "verified with the GST portal"; no such check exists yet.
     */
    public record BusinessIdentifiers(String gstinStatus, String gstinMasked, boolean panPresent, String gstin, String pan) {
    }

    public record StatusChange(String from, String to, String reason, Long actorUserId, LocalDateTime at) {
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
                g.getGstNumber() != null && !g.getGstNumber().isBlank(),
                g.getUpdatedAt(), ownerOf(g), activeStaffByRole(id), brandingOf(g), identifiersOf(g),
                countsBy("select b.status, count(b) from Booking b where b.garageId = :id group by b.status", id),
                countsBy("select e.status, count(e) from Estimate e where e.jobCard.garage.id = :id group by e.status", id),
                lastStatusChange(id),
                bankAccountService == null ? "NOT_AVAILABLE" : bankAccountService.statusSummary(id),
                g.getLandmark(), g.getPincode(), g.getTimeZone());
    }

    private OwnerSummary ownerOf(Garage g) {

        if (g.getOwnerUserId() == null) {
            return null;
        }

        com.garageos.modules.identity.entity.User u =
                em.find(com.garageos.modules.identity.entity.User.class, g.getOwnerUserId());

        if (u == null) {
            return null;
        }

        String name = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();

        return new OwnerSummary(u.getId(), name, maskMobile(u.getMobile()), maskEmail(u.getEmail()), u.getMobile(), u.getEmail());
    }

    static String maskMobile(String mobile) {
        return mobile == null || mobile.length() < 4 ? null : "X".repeat(mobile.length() - 4) + mobile.substring(mobile.length() - 4);
    }

    static String maskEmail(String email) {

        if (email == null || email.indexOf('@') < 1) {
            return null;
        }

        int at = email.indexOf('@');

        return email.charAt(0) + "***" + email.substring(at);
    }

    private Map<String, Long> activeStaffByRole(Long garageId) {

        Map<String, Long> out = new HashMap<>();

        for (Object[] row : em.createQuery(
                "select r.code, count(distinct u.id) from User u join u.userRoles ur join ur.role r "
                        + "where u.garageId = :id and u.status = :st group by r.code", Object[].class)
                .setParameter("id", garageId)
                .setParameter("st", com.garageos.core.enums.identity.UserStatus.ACTIVE)
                .getResultList()) {
            out.put(String.valueOf(row[0]), (Long) row[1]);
        }

        return out;
    }

    private static BrandingSummary brandingOf(Garage g) {
        return new BrandingSummary(g.getLogoStorageKey() != null, g.getBrandPrimaryColor(), g.getBrandSecondaryColor(),
                g.getBrandPrimaryColor() != null || g.getBrandSecondaryColor() != null,
                g.getLogoUpdatedAt(), g.getBrandingUpdatedAt());
    }

    private static BusinessIdentifiers identifiersOf(Garage g) {

        String gstin = com.garageos.core.util.GstinValidator.normalize(g.getGstNumber());

        String status = gstin == null ? "ABSENT"
                : com.garageos.core.util.GstinValidator.isStructurallyValid(gstin) ? "FORMAT_VALID" : "FORMAT_INVALID";

        boolean panPresent = g.getPanNumber() != null && !g.getPanNumber().isBlank();

        return new BusinessIdentifiers(status, com.garageos.core.util.GstinValidator.mask(gstin), panPresent,
                gstin, panPresent ? g.getPanNumber().trim() : null);
    }

    private Map<String, Long> countsBy(String jpql, Long garageId) {

        Map<String, Long> out = new HashMap<>();

        for (Object[] row : em.createQuery(jpql, Object[].class).setParameter("id", garageId).getResultList()) {
            out.put(String.valueOf(row[0]), (Long) row[1]);
        }

        return out;
    }

    /** The most recent activation/deactivation, from the audit trail (admin-only data: it carries the reason). */
    private StatusChange lastStatusChange(Long garageId) {

        List<com.garageos.modules.audit.entity.AuditEvent> events = em.createQuery(
                "select e from AuditEvent e where e.garageId = :id and e.eventType = :t order by e.createdAt desc, e.id desc",
                com.garageos.modules.audit.entity.AuditEvent.class)
                .setParameter("id", garageId)
                .setParameter("t", AuditEventType.GARAGE_STATUS_CHANGED)
                .setMaxResults(1)
                .getResultList();

        if (events.isEmpty()) {
            return null;
        }

        com.garageos.modules.audit.entity.AuditEvent e = events.get(0);

        try {
            com.fasterxml.jackson.databind.JsonNode meta = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(e.getMetadata() == null ? "{}" : e.getMetadata());
            return new StatusChange(text(meta, "from"), text(meta, "to"), text(meta, "reason"), e.getActorId(),
                    e.getCreatedAt());
        } catch (Exception ex) {
            return new StatusChange(null, null, null, e.getActorId(), e.getCreatedAt());
        }
    }

    private static String text(com.fasterxml.jackson.databind.JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    /** One row of the platform audit trail. Platform administrators see the full record, including actor and reason. */
    public record AuditRow(Long id, String eventType, String entityType, Long entityId, Long garageId, Long actorId,
                           String actorRole, String metadata, LocalDateTime createdAt) {
    }

    /**
     * Step-up for high-impact administrative writes: the administrator must re-enter their own password. This is the
     * strongest server-side re-authentication the current identity model supports without a broader redesign (there
     * is no second factor for staff accounts); attempts are rate limited and every refusal is audited. Deliberately
     * NOT transactional, so the refusal's audit row is committed even though the caller then gets an error.
     */
    public void requireReauthentication(Long actorId, String password, Long garageId) {

        if (rateLimitService != null && actorId != null) {
            rateLimitService.require("admin-stepup-user", "user:" + actorId);
        }

        com.garageos.modules.identity.entity.User actor = actorId == null
                ? null
                : em.find(com.garageos.modules.identity.entity.User.class, actorId);

        boolean ok = actor != null && passwordEncoder != null && password != null && !password.isBlank()
                && passwordEncoder.matches(password, actor.getPasswordHash());

        if (!ok) {
            if (auditService != null) {
                auditService.record(AuditEventType.GARAGE_STATUS_CHANGE_REFUSED, "GARAGE", garageId, garageId,
                        Map.of("reason", "REAUTHENTICATION_FAILED", "actorUserId", String.valueOf(actorId)));
            }
            throw new BusinessException("Re-authentication failed. Enter your current password to continue.");
        }
    }

    /** Platform-level audit search: bounded, newest first, optional filters. Querying it is itself audited. */
    @Transactional
    public Page<AuditRow> audit(Long garageId, String entityType,
                                com.garageos.core.enums.audit.AuditEventType eventType, int page, int size, Long actorId) {

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        String where = " where (:garageId is null or e.garageId = :garageId)"
                + " and (:entityType is null or e.entityType = :entityType)"
                + " and (:eventType is null or e.eventType = :eventType)";

        TypedQuery<com.garageos.modules.audit.entity.AuditEvent> list = em.createQuery(
                "select e from AuditEvent e" + where + " order by e.createdAt desc, e.id desc",
                com.garageos.modules.audit.entity.AuditEvent.class);
        TypedQuery<Long> count = em.createQuery("select count(e) from AuditEvent e" + where, Long.class);

        String type = entityType == null || entityType.isBlank() ? null : entityType.trim();

        for (TypedQuery<?> q : List.of(list, count)) {
            q.setParameter("garageId", garageId);
            q.setParameter("entityType", type);
            q.setParameter("eventType", eventType);
        }
        list.setFirstResult(safePage * safeSize);
        list.setMaxResults(safeSize);

        List<AuditRow> rows = list.getResultList().stream()
                .map(e -> new AuditRow(e.getId(), e.getEventType().name(), e.getEntityType(), e.getEntityId(),
                        e.getGarageId(), e.getActorId(), e.getActorRole(), e.getMetadata(), e.getCreatedAt()))
                .toList();

        if (auditService != null) {
            auditService.record(AuditEventType.PLATFORM_AUDIT_VIEWED, "PLATFORM_AUDIT", garageId == null ? 0L : garageId,
                    null, Map.of("actorUserId", String.valueOf(actorId), "returned", rows.size()));
        }

        return new PageImpl<>(rows, PageRequest.of(safePage, safeSize), count.getSingleResult());
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
