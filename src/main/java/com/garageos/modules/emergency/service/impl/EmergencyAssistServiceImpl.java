package com.garageos.modules.emergency.service.impl;

import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.core.enums.customer.CustomerGarageSource;
import com.garageos.core.enums.emergency.EmergencyRequestStatus;
import com.garageos.core.enums.notification.NotificationEventType;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ConflictException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.ratelimit.RateLimitService;
import com.garageos.core.security.GarageScope;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.modules.customer.service.CustomerGarageService;
import com.garageos.modules.emergency.dto.request.EmergencyRequests;
import com.garageos.modules.emergency.dto.response.EmergencyResponses;
import com.garageos.modules.emergency.dto.response.EmergencyResponses.GarageOption;
import com.garageos.modules.emergency.dto.response.EmergencyResponses.GarageOptions;
import com.garageos.modules.emergency.dto.response.EmergencyResponses.GarageRequestView;
import com.garageos.modules.emergency.dto.response.EmergencyResponses.RequestView;
import com.garageos.modules.emergency.entity.EmergencyAssistRequest;
import com.garageos.modules.emergency.repository.EmergencyAssistRequestRepository;
import com.garageos.modules.emergency.service.EmergencyAssistService;
import com.garageos.modules.emergency.service.EmergencyEligibilityService;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.notification.event.NotificationEvent;
import com.garageos.modules.notification.event.NotificationEventPublisher;
import com.garageos.modules.notification.event.NotificationEvents;
import com.garageos.modules.otp.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EmergencyAssistServiceImpl implements EmergencyAssistService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyAssistServiceImpl.class);

    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("^[A-Za-z0-9._:-]{8,80}$");
    private static final EnumSet<EmergencyRequestStatus> ACTIVE =
            EnumSet.of(EmergencyRequestStatus.REQUESTED, EmergencyRequestStatus.ACCEPTED);
    private static final String ENTITY = "EmergencyAssistRequest";

    private final EmergencyAssistRequestRepository repository;
    private final EmergencyTokens tokens;
    private final EmergencyEligibilityService eligibility;
    private final EmergencyCustomerResolver customerResolver;
    private final OtpService otpService;
    private final com.garageos.modules.otp.config.OtpProperties otpProperties;
    private final RateLimitService rateLimit;
    private final AuditService auditService;
    private final GarageRepository garageRepository;
    private final CustomerGarageService customerGarageService;
    private final GarageScope garageScope;
    private final long requestTtlMinutes;
    private final long acceptedAutoCloseHours;
    private final TransactionTemplate transactionTemplate;

    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;

    public EmergencyAssistServiceImpl(
            EmergencyAssistRequestRepository repository,
            EmergencyTokens tokens,
            EmergencyEligibilityService eligibility,
            EmergencyCustomerResolver customerResolver,
            OtpService otpService,
            com.garageos.modules.otp.config.OtpProperties otpProperties,
            RateLimitService rateLimit,
            AuditService auditService,
            GarageRepository garageRepository,
            CustomerGarageService customerGarageService,
            GarageScope garageScope,
            PlatformTransactionManager transactionManager,
            @Value("${emergency.request-ttl-minutes:30}") long requestTtlMinutes,
            @Value("${emergency.accepted-auto-close-hours:24}") long acceptedAutoCloseHours) {
        this.repository = repository;
        this.tokens = tokens;
        this.eligibility = eligibility;
        this.customerResolver = customerResolver;
        this.otpService = otpService;
        this.otpProperties = otpProperties;
        this.rateLimit = rateLimit;
        this.auditService = auditService;
        this.garageRepository = garageRepository;
        this.customerGarageService = customerGarageService;
        this.garageScope = garageScope;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.requestTtlMinutes = requestTtlMinutes;
        this.acceptedAutoCloseHours = acceptedAutoCloseHours;
    }

    // ------------------------------------------------------------------ contact verification

    @Override
    public EmergencyResponses.VerificationSent sendVerification(String mobile) {

        rateLimit.require("emergency-phone", mobile);

        // The OTP service owns generation, expiry, attempts, cooldown and hourly caps. The response is identical for
        // a number that is or is not already a customer.
        otpService.send(com.garageos.core.enums.otp.OtpPurpose.EMERGENCY_ASSIST, mobile, null);

        return new EmergencyResponses.VerificationSent(
                "XXXXXX" + mobile.substring(6),
                otpProperties.getExpiry().toSeconds(),
                otpProperties.getResendCooldown().toSeconds());
    }

    @Override
    public EmergencyResponses.VerificationConfirmed confirmVerification(String mobile, String otp) {

        otpService.verifyAndConsume(com.garageos.core.enums.otp.OtpPurpose.EMERGENCY_ASSIST, mobile, otp);

        return new EmergencyResponses.VerificationConfirmed(
                tokens.issueVerification(mobile), EmergencyTokens.VERIFICATION_TTL_SECONDS);
    }

    private String requireVerifiedMobile(String token) {
        return tokens.verifiedMobile(token).orElseThrow(() ->
                new AccessDeniedException("Contact verification is missing or has expired. Verify your number again."));
    }

    // ------------------------------------------------------------------ eligibility

    @Override
    public GarageOptions eligibleGarages(EmergencyRequests.SearchGarages request) {

        String mobile = requireVerifiedMobile(request.getVerificationToken());

        return eligibility.eligibleFor(mobile, request.getLatitude(), request.getLongitude());
    }

    // ------------------------------------------------------------------ create

    /** What one create attempt decided; a refusal is returned (not thrown) so its audit row survives. */
    private record CreateOutcome(RequestView view, GarageOption refusedGarage) {
    }

    @Override
    public RequestView create(String idempotencyKey, EmergencyRequests.Create req) {

        CreateOutcome outcome = transactionTemplate.execute(status -> doCreate(idempotencyKey, req));

        if (outcome.refusedGarage() != null) {
            GarageOption refused = outcome.refusedGarage();
            // Recorded after the (empty) transaction ended: a thrown exception would have rolled this row back.
            auditService.record(AuditEventType.EMERGENCY_REQUEST_REFUSED, ENTITY, refused.garageId(), refused.garageId(),
                    Map.of("reason", String.valueOf(refused.unavailableReason())));
            // The default garage cannot take this request. Say so; never route the customer elsewhere on their behalf.
            throw new ConflictException(
                    "Your garage cannot take this request right now (" + refused.unavailableReason() + ").",
                    "EMERGENCY_GARAGE_UNAVAILABLE");
        }

        return outcome.view();
    }

    private CreateOutcome doCreate(String idempotencyKey, EmergencyRequests.Create req) {

        if (idempotencyKey == null || !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            throw new BusinessException("A valid Idempotency-Key header (8 to 80 letters, digits or . _ : -) is required.");
        }

        String mobile = requireVerifiedMobile(req.getVerificationToken());

        // A retry of the same submission is the same request: return it, never create a second.
        var replay = repository.findByContactMobileAndIdempotencyKey(mobile, idempotencyKey);
        if (replay.isPresent()) {
            return new CreateOutcome(view(expireIfDue(replay.get()), true), null);
        }

        rateLimit.require("emergency-create-phone", mobile);

        var active = repository.findFirstByContactMobileAndStatusIn(mobile, ACTIVE);
        if (active.isPresent() && !isExpiredNow(active.get())) {
            throw new ConflictException(
                    "You already have an active emergency request (" + active.get().getRequestCode() + ").",
                    "EMERGENCY_ACTIVE_REQUEST");
        }

        GarageOptions options = eligibility.eligibleFor(mobile, req.getLatitude(), req.getLongitude());
        GarageOption chosen = choose(options, req.getGarageId());

        if (!chosen.available()) {
            return new CreateOutcome(null, chosen);
        }

        Long customerId = resolveCustomer(mobile, req.getContactName().trim(), blankToNull(req.getContactEmail()));

        LocalDateTime now = LocalDateTime.now();

        EmergencyAssistRequest r = new EmergencyAssistRequest();
        r.setRequestCode(tokens.newRequestCode());
        r.setIdempotencyKey(idempotencyKey);
        r.setStatus(EmergencyRequestStatus.REQUESTED);
        r.setAssistanceType(req.getAssistanceType());
        r.setGarageId(chosen.garageId());
        r.setCustomerId(customerId);
        r.setContactName(req.getContactName().trim());
        r.setContactMobile(mobile);
        r.setContactEmail(blankToNull(req.getContactEmail()));
        r.setVehicleRegistration(blankToNull(req.getVehicleRegistration()));
        r.setVehicleDescription(blankToNull(req.getVehicleDescription()));
        r.setLatitude(req.getLatitude());
        r.setLongitude(req.getLongitude());
        r.setAddress(blankToNull(req.getAddress()));
        r.setNotes(blankToNull(req.getNotes()));
        r.setExpiresAt(now.plus(Duration.ofMinutes(requestTtlMinutes)));

        try {
            r = repository.saveAndFlush(r);
        } catch (DataIntegrityViolationException e) {
            // Lost a race on (number, key) or on the one-active-request-per-number index.
            throw new ConflictException(
                    "An emergency request for this number is already in progress.", "EMERGENCY_ACTIVE_REQUEST");
        }

        auditService.record(AuditEventType.EMERGENCY_REQUEST_CREATED, ENTITY, r.getId(), r.getGarageId(),
                Map.of("assistanceType", r.getAssistanceType().name(), "defaultGarage", chosen.defaultGarage()));

        NotificationEvents.publish(notificationEventPublisher,
                NotificationEvent.of(NotificationEventType.EMERGENCY_REQUESTED, r.getGarageId(), r.getId(), null, "1"));

        log.info("[EMERGENCY] Created. requestId={}, garageId={}, type={}", r.getId(), r.getGarageId(), r.getAssistanceType());

        return new CreateOutcome(view(r, true), null);
    }

    private GarageOption choose(GarageOptions options, Long requestedGarageId) {

        if (options.defaultGarageMode()) {
            GarageOption only = options.garages().get(0);
            if (requestedGarageId != null && !requestedGarageId.equals(only.garageId())) {
                throw new AccessDeniedException("That garage is not available for this request.");
            }
            return only;
        }

        if (requestedGarageId == null) {
            throw new BusinessException("Choose one of the garages offered for your location.");
        }

        return options.garages().stream()
                .filter(g -> g.garageId().equals(requestedGarageId))
                .findFirst()
                // Not in the server-computed set: a forged or stale id never gets through.
                .orElseThrow(() -> new AccessDeniedException("That garage is not available for this request."));
    }

    private Long resolveCustomer(String mobile, String name, String email) {
        try {
            return customerResolver.findOrCreate(mobile, name, email);
        } catch (DataIntegrityViolationException e) {
            return customerResolver.findOrCreate(mobile, name, email);
        }
    }

    // ------------------------------------------------------------------ customer status / cancel

    @Override
    @Transactional
    public RequestView status(String requestCode, String statusToken) {
        return view(expireIfDue(requireByToken(requestCode, statusToken, false)), false);
    }

    @Override
    @Transactional
    public RequestView cancel(String requestCode, String statusToken) {

        EmergencyAssistRequest r = expireIfDue(requireByToken(requestCode, statusToken, true));

        if (!r.getStatus().isActive()) {
            throw new ConflictException("This request can no longer be cancelled.", "EMERGENCY_NOT_CANCELLABLE");
        }

        r.setStatus(EmergencyRequestStatus.CANCELLED);
        r = repository.save(r);

        auditService.record(AuditEventType.EMERGENCY_REQUEST_CANCELLED, ENTITY, r.getId(), r.getGarageId(), Map.of());

        return view(r, false);
    }

    /** Unknown code and wrong token are indistinguishable (same not-found), so codes cannot be probed. */
    private EmergencyAssistRequest requireByToken(String code, String token, boolean forUpdate) {

        var found = forUpdate ? repository.findByRequestCodeForUpdate(code) : repository.findByRequestCode(code);

        EmergencyAssistRequest r = found.orElseThrow(() -> new ResourceNotFoundException("Request not found."));

        if (!tokens.statusTokenMatches(r.getRequestCode(), r.getContactMobile(), token)) {
            throw new ResourceNotFoundException("Request not found.");
        }

        return r;
    }

    // ------------------------------------------------------------------ garage side

    @Override
    @Transactional
    public List<GarageRequestView> garageRequests(GarageUserPrincipal principal) {

        if (principal.getGarageId() == null) {
            return List.of();
        }

        return repository.findByGarageIdOrderByCreatedAtDesc(principal.getGarageId()).stream()
                .map(this::expireIfDue)
                .map(this::garageView)
                .toList();
    }

    @Override
    @Transactional
    public GarageRequestView accept(Long id, String remarks, GarageUserPrincipal principal) {

        EmergencyAssistRequest r = lockedForGarage(id, principal);

        requireOpen(r);

        r.setStatus(EmergencyRequestStatus.ACCEPTED);
        stamp(r, remarks, principal);
        r = repository.save(r);

        // The garage took the job: it is now part of this customer's garages (same source booking confirmation uses).
        customerGarageService.link(r.getCustomerId(), r.getGarageId(), CustomerGarageSource.BOOKING_CONFIRMED);

        auditService.record(AuditEventType.EMERGENCY_REQUEST_ACCEPTED, ENTITY, r.getId(), r.getGarageId(),
                Map.of("actorUserId", String.valueOf(principal.getId())));

        return garageView(r);
    }

    @Override
    @Transactional
    public GarageRequestView reject(Long id, String remarks, GarageUserPrincipal principal) {

        EmergencyAssistRequest r = lockedForGarage(id, principal);

        requireOpen(r);

        r.setStatus(EmergencyRequestStatus.REJECTED);
        stamp(r, remarks, principal);
        r = repository.save(r);

        auditService.record(AuditEventType.EMERGENCY_REQUEST_REJECTED, ENTITY, r.getId(), r.getGarageId(),
                Map.of("actorUserId", String.valueOf(principal.getId())));

        return garageView(r);
    }

    @Override
    @Transactional
    public GarageRequestView complete(Long id, String remarks, GarageUserPrincipal principal) {

        EmergencyAssistRequest r = lockedForGarage(id, principal);

        if (r.getStatus() != EmergencyRequestStatus.ACCEPTED) {
            throw new ConflictException("Only an accepted request can be completed.", "EMERGENCY_BAD_STATE");
        }

        r.setStatus(EmergencyRequestStatus.COMPLETED);
        stamp(r, remarks, principal);
        r = repository.save(r);

        auditService.record(AuditEventType.EMERGENCY_REQUEST_COMPLETED, ENTITY, r.getId(), r.getGarageId(),
                Map.of("actorUserId", String.valueOf(principal.getId())));

        return garageView(r);
    }

    /** Lock first, then check garage scope on the locked row. Another garage's id is just "not found". */
    private EmergencyAssistRequest lockedForGarage(Long id, GarageUserPrincipal principal) {

        EmergencyAssistRequest r = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found."));

        if (!garageScope.canAccessGarage(principal, r.getGarageId())) {
            throw new ResourceNotFoundException("Request not found.");
        }

        return expireIfDue(r);
    }

    private void requireOpen(EmergencyAssistRequest r) {
        if (r.getStatus() != EmergencyRequestStatus.REQUESTED) {
            throw new ConflictException(
                    "This request is " + r.getStatus().name().toLowerCase() + " and can no longer be answered.",
                    "EMERGENCY_BAD_STATE");
        }
    }

    private static void stamp(EmergencyAssistRequest r, String remarks, GarageUserPrincipal principal) {
        r.setGarageRemarks(blankToNull(remarks));
        r.setDecidedByUserId(principal.getId());
        r.setDecidedAt(LocalDateTime.now());
    }

    // ------------------------------------------------------------------ expiry

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${emergency.expiry-sweep-ms:60000}",
            initialDelayString = "${emergency.expiry-sweep-initial-ms:60000}")
    public void expireStale() {
        LocalDateTime now = LocalDateTime.now();
        int unanswered = repository.expireRequested(now);
        int closed = repository.expireStaleAccepted(now.minusHours(acceptedAutoCloseHours));
        if (unanswered + closed > 0) {
            log.info("[EMERGENCY] Expired. unanswered={}, staleAccepted={}", unanswered, closed);
        }
    }

    private boolean isExpiredNow(EmergencyAssistRequest r) {
        return r.getStatus() == EmergencyRequestStatus.REQUESTED && r.getExpiresAt().isBefore(LocalDateTime.now());
    }

    /** Applies expiry lazily so a read never shows a lapsed request as still open. */
    private EmergencyAssistRequest expireIfDue(EmergencyAssistRequest r) {
        if (isExpiredNow(r)) {
            r.setStatus(EmergencyRequestStatus.EXPIRED);
            return repository.save(r);
        }
        return r;
    }

    // ------------------------------------------------------------------ mapping

    private RequestView view(EmergencyAssistRequest r, boolean withToken) {
        String garageName = garageRepository.findById(r.getGarageId())
                .map(g -> g.getGarageName()).orElse(null);
        return new RequestView(r.getRequestCode(), r.getStatus().name(), r.getAssistanceType().name(), garageName,
                r.getGarageRemarks(), r.getCreatedAt(), r.getExpiresAt(),
                withToken ? tokens.statusToken(r.getRequestCode(), r.getContactMobile()) : null);
    }

    private GarageRequestView garageView(EmergencyAssistRequest r) {
        return new GarageRequestView(r.getId(), r.getRequestCode(), r.getStatus().name(), r.getAssistanceType().name(),
                r.getContactName(), r.getContactMobile(), r.getVehicleRegistration(), r.getVehicleDescription(),
                r.getLatitude(), r.getLongitude(), r.getAddress(), r.getNotes(), r.getGarageRemarks(),
                r.getCreatedAt(), r.getExpiresAt(), r.getDecidedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
