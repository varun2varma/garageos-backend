package com.garageos.modules.bankaccount.service.impl;

import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.core.enums.bankaccount.BankAccountVerificationStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ConflictException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.ratelimit.RateLimitService;
import com.garageos.core.security.FieldEncryptor;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.modules.bankaccount.dto.request.RegisterBankAccountRequest;
import com.garageos.modules.bankaccount.dto.request.RetireBankAccountRequest;
import com.garageos.modules.bankaccount.dto.response.BankAccountView;
import com.garageos.modules.bankaccount.entity.GarageBankAccount;
import com.garageos.modules.bankaccount.provider.BankVerificationProvider;
import com.garageos.modules.bankaccount.repository.GarageBankAccountRepository;
import com.garageos.modules.bankaccount.service.BankAccountService;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BankAccountServiceImpl implements BankAccountService {

    private static final Logger log = LoggerFactory.getLogger(BankAccountServiceImpl.class);
    private static final String ENTITY = "GarageBankAccount";

    private final GarageBankAccountRepository repository;
    private final GarageRepository garageRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FieldEncryptor encryptor;
    private final AuditService auditService;
    private final RateLimitService rateLimit;
    private final List<BankVerificationProvider> providers;
    private final TransactionTemplate tx;

    public BankAccountServiceImpl(
            GarageBankAccountRepository repository,
            GarageRepository garageRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            FieldEncryptor encryptor,
            AuditService auditService,
            RateLimitService rateLimit,
            List<BankVerificationProvider> providers,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.garageRepository = garageRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptor = encryptor;
        this.auditService = auditService;
        this.rateLimit = rateLimit;
        this.providers = providers;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // ------------------------------------------------------------------ owner: register

    @Override
    public BankAccountView register(GarageUserPrincipal principal, Long garageId, RegisterBankAccountRequest req) {

        if (!encryptor.isConfigured()) {
            // Never a plaintext fallback: without the key, registration is simply unavailable.
            throw new BusinessException("Bank account registration is not available yet.", "BANK_NOT_CONFIGURED");
        }

        Garage garage = requireOwnedGarage(principal, garageId);

        requireStepUp(principal, garage.getId(), req.getPassword());

        GarageBankAccount saved;

        try {
            saved = tx.execute(status -> persistNew(principal, garage.getId(), req));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(
                    "The bank account was changed at the same time. Reload and try again.", "BANK_CONCURRENT_CHANGE");
        }

        maybeVerify(saved, req.getAccountNumber());

        return view(repository.findById(saved.getId()).orElse(saved));
    }

    private GarageBankAccount persistNew(GarageUserPrincipal principal, Long garageId, RegisterBankAccountRequest req) {

        LocalDateTime now = LocalDateTime.now();

        Optional<GarageBankAccount> previous = repository.findByGarageIdAndCurrentTrue(garageId);

        previous.ifPresent(p -> {
            p.setCurrent(false);
            p.setSupersededAt(now);
            p.setRetiredByUserId(principal.getId());
            repository.saveAndFlush(p);
        });

        String number = req.getAccountNumber();

        GarageBankAccount a = new GarageBankAccount();
        a.setGarageId(garageId);
        a.setAccountHolderName(req.getAccountHolderName().trim());
        a.setBankName(blankToNull(req.getBankName()));
        a.setIfsc(req.getIfsc().trim().toUpperCase());
        a.setAccountType(req.getAccountType());
        a.setAccountNumberEnc(encryptor.encrypt(number, aad(garageId)));
        a.setAccountNumberLast4(number.substring(number.length() - 4));
        a.setVerificationStatus(BankAccountVerificationStatus.PENDING_VERIFICATION);
        a.setConsentAt(now);
        a.setCurrent(true);
        a.setCreatedByUserId(principal.getId());

        a = repository.saveAndFlush(a);

        // The audit row carries who/what/when and the last four digits only; never the number or the ciphertext.
        auditService.record(AuditEventType.BANK_ACCOUNT_REGISTERED, ENTITY, a.getId(), garageId,
                Map.of("actorUserId", String.valueOf(principal.getId()), "last4", a.getAccountNumberLast4(),
                        "replacedPrevious", previous.isPresent()));

        log.info("[BANK] Account registered. garageId={}, accountId={}, replacedPrevious={}",
                garageId, a.getId(), previous.isPresent());

        return a;
    }

    /** Calls the configured provider, if any, OUTSIDE the registration transaction. An outage leaves it pending. */
    private void maybeVerify(GarageBankAccount account, String plainNumber) {

        Optional<BankVerificationProvider> provider = providers.stream().filter(BankVerificationProvider::isConfigured).findFirst();

        if (provider.isEmpty()) {
            return;
        }

        try {
            BankVerificationProvider.Outcome outcome = provider.get()
                    .verify(account.getAccountHolderName(), plainNumber, account.getIfsc());
            applyVerificationOutcome(account.getId(), provider.get().name(), outcome);
        } catch (RuntimeException e) {
            // A provider outage is not a failed verification: the account stays PENDING_VERIFICATION.
            log.warn("[BANK] Verification provider unavailable; account stays pending. accountId={}, reason={}",
                    account.getId(), e.getClass().getSimpleName());
        }
    }

    // ------------------------------------------------------------------ owner: read / retire

    @Override
    @Transactional(readOnly = true)
    public Optional<BankAccountView> currentForOwner(GarageUserPrincipal principal, Long garageId) {

        Garage garage = requireOwnedGarage(principal, garageId);

        return repository.findByGarageIdAndCurrentTrue(garage.getId()).map(this::view);
    }

    @Override
    public void retire(GarageUserPrincipal principal, Long garageId, RetireBankAccountRequest request) {

        Garage garage = requireOwnedGarage(principal, garageId);

        requireStepUp(principal, garage.getId(), request.getPassword());

        tx.executeWithoutResult(status -> {
            GarageBankAccount a = repository.findByGarageIdAndCurrentTrue(garage.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("No bank account is registered."));

            a.setCurrent(false);
            a.setSupersededAt(LocalDateTime.now());
            a.setRetiredByUserId(principal.getId());
            repository.save(a);

            auditService.record(AuditEventType.BANK_ACCOUNT_RETIRED, ENTITY, a.getId(), garage.getId(),
                    Map.of("actorUserId", String.valueOf(principal.getId()), "last4", a.getAccountNumberLast4()));
        });
    }

    // ------------------------------------------------------------------ platform administrator

    @Override
    @Transactional
    public Optional<BankAccountView> currentForPlatform(Long garageId, Long adminUserId) {

        if (!garageRepository.existsById(garageId)) {
            throw new ResourceNotFoundException("Garage not found.");
        }

        // Sensitive read, so it leaves a trace even though the view is masked.
        auditService.record(AuditEventType.PLATFORM_BANK_ACCOUNT_VIEWED, ENTITY, garageId, null,
                Map.of("actorUserId", String.valueOf(adminUserId)));

        return repository.findByGarageIdAndCurrentTrue(garageId).map(this::view);
    }

    @Override
    @Transactional(readOnly = true)
    public String statusSummary(Long garageId) {
        return repository.findByGarageIdAndCurrentTrue(garageId)
                .map(a -> a.getVerificationStatus().name())
                .orElse("NOT_REGISTERED");
    }

    // ------------------------------------------------------------------ provider outcome

    @Override
    @Transactional
    public void applyVerificationOutcome(Long accountId, String providerName, BankVerificationProvider.Outcome outcome) {

        GarageBankAccount a = repository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found."));

        if (outcome == null || outcome.status() == null
                || outcome.status() == BankAccountVerificationStatus.PENDING_VERIFICATION) {
            return;
        }

        a.setVerificationStatus(outcome.status());
        a.setVerificationProvider(providerName);
        a.setVerificationReference(outcome.reference());
        a.setVerifiedAt(outcome.status() == BankAccountVerificationStatus.VERIFIED ? LocalDateTime.now() : null);
        a.setFailureReason(outcome.status() == BankAccountVerificationStatus.VERIFIED ? null : outcome.failureReason());
        repository.save(a);

        auditService.record(AuditEventType.BANK_ACCOUNT_VERIFICATION_RESULT, ENTITY, a.getId(), a.getGarageId(),
                Map.of("status", outcome.status().name(), "provider", String.valueOf(providerName)));
    }

    // ------------------------------------------------------------------ helpers

    /** The caller must be the OWNER of THIS garage (never trusted from the request). Others get "not found". */
    private Garage requireOwnedGarage(GarageUserPrincipal principal, Long garageId) {

        Garage garage = garageRepository.findById(garageId)
                .orElseThrow(() -> new ResourceNotFoundException("Garage not found."));

        boolean owner = principal.getRoles() != null && principal.getRoles().contains(RoleCode.OWNER.name());

        boolean ownsThis = (principal.getGarageId() != null && principal.getGarageId().equals(garage.getId()))
                || (principal.getId() != null && principal.getId().equals(garage.getOwnerUserId()));

        if (!owner || !ownsThis) {
            throw new ResourceNotFoundException("Garage not found.");
        }

        return garage;
    }

    /**
     * Step-up: the owner re-enters their own password. Attempts are rate limited and every refusal is audited
     * (outside any transaction, so the refusal row survives the error). This is the strongest re-authentication the
     * current identity model supports; SMS/e-mail OTP can be layered on later but would only prove contact control.
     */
    private void requireStepUp(GarageUserPrincipal principal, Long garageId, String password) {

        rateLimit.require("bank-stepup-user", "user:" + principal.getId());

        User user = principal.getId() == null ? null : userRepository.findById(principal.getId()).orElse(null);

        boolean ok = user != null && password != null && !password.isBlank()
                && passwordEncoder.matches(password, user.getPasswordHash());

        if (!ok) {
            auditService.record(AuditEventType.BANK_ACCOUNT_STEPUP_REFUSED, ENTITY, garageId, garageId,
                    Map.of("actorUserId", String.valueOf(principal.getId())));
            throw new AccessDeniedException("Re-authentication failed. Enter your current password to continue.");
        }
    }

    private static String aad(Long garageId) {
        return "bank:" + garageId;
    }

    private BankAccountView view(GarageBankAccount a) {
        return new BankAccountView(a.getId(), a.getAccountHolderName(), a.getBankName(), a.getIfsc(),
                a.getAccountType().name(), "XXXXXX" + a.getAccountNumberLast4(), a.getVerificationStatus().name(),
                a.getVerificationProvider(), a.getVerificationReference(), a.getVerifiedAt(), a.getFailureReason(),
                a.getCreatedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
