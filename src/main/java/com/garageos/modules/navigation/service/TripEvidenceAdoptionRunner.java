package com.garageos.modules.navigation.service;

import com.garageos.modules.media.storage.R2MediaStorageProvider;
import com.garageos.modules.navigation.repository.NavigationTripMediaRepository;
import com.garageos.modules.navigation.service.TripEvidenceAdoptionService.Mode;
import com.garageos.modules.navigation.service.TripEvidenceAdoptionService.Outcome;
import com.garageos.modules.navigation.service.TripEvidenceAdoptionService.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Opt-in operator path that moves existing trip evidence (legacy {@code trip-evidence/} keys and provisional
 * {@code TRIP-<id>} folders) into the job-card folders. <b>Disabled unless</b>
 * {@code garagest.trip-evidence.adoption.run} is set to {@code dry-run}, {@code execute-keep-old} or {@code execute}.
 *
 * <h3>Safety guards</h3>
 * <ul>
 *   <li>All configuration is validated when a run starts, never when the bean is built: a bad value refuses the run
 *       with a clear message and cannot stop the application from starting.</li>
 *   <li>It always starts with a read-only pass over the candidates ({@link Mode#DRY_RUN}: no object write or delete, no
 *       database change, no audit event) and logs the manifest (job card, photo id, stage, old key, new key, outcome; no
 *       URLs, credentials or personal data) plus the connected database name, the bucket actually configured on the R2
 *       provider and the candidate count.</li>
 *   <li>{@code dry-run} stops there.</li>
 *   <li>An execute mode refuses to run unless <b>all three</b> hold: {@code ...adoption.confirm-database} equals the
 *       database actually connected to; {@code ...adoption.confirm-bucket} equals the bucket of the configured R2
 *       provider (if the bucket identity cannot be established the run is refused); and
 *       {@code ...adoption.confirm-candidates} equals the candidate count just computed.</li>
 *   <li>Bounded: at most {@code ...adoption.max-job-cards} job cards per start (1..1000, default 200), or the single
 *       {@code ...adoption.job-card-id} (a positive whole number). It runs once on a background thread and never blocks
 *       startup.</li>
 *   <li>The storage paths are always built from persisted garage and job-card records, never from configuration.</li>
 * </ul>
 * {@code execute-keep-old} copies, verifies and repoints but keeps every old object (the audit events record each old
 * and new key); {@code execute} also deletes an old object, only after the new object and the database reference are
 * verified and no other row references the old key.
 */
@Slf4j
@Component
@ConditionalOnExpression("'${garagest.trip-evidence.adoption.run:}' != ''")
public class TripEvidenceAdoptionRunner implements ApplicationRunner {

    static final int DEFAULT_MAX_JOB_CARDS = 200;
    static final int MAX_JOB_CARDS_LIMIT = 1000;

    /** What a start did, for logs and tests. */
    public record Summary(Mode mode, String database, String bucket, long candidates, boolean refused, String refusal,
                          long switched, long failed, long needAttention) {
    }

    private final TripEvidenceAdoptionService adoption;
    private final NavigationTripMediaRepository mediaRepository;
    private final JdbcTemplate jdbc;
    private final R2MediaStorageProvider r2;
    private final String runMode;
    private final String jobCardId;
    private final String maxJobCards;
    private final String confirmDatabase;
    private final String confirmBucket;
    private final String confirmCandidates;

    @Autowired
    public TripEvidenceAdoptionRunner(
            TripEvidenceAdoptionService adoption,
            NavigationTripMediaRepository mediaRepository,
            JdbcTemplate jdbc,
            R2MediaStorageProvider r2,
            @Value("${garagest.trip-evidence.adoption.run:}") String runMode,
            @Value("${garagest.trip-evidence.adoption.job-card-id:}") String jobCardId,
            @Value("${garagest.trip-evidence.adoption.max-job-cards:}") String maxJobCards,
            @Value("${garagest.trip-evidence.adoption.confirm-database:}") String confirmDatabase,
            @Value("${garagest.trip-evidence.adoption.confirm-bucket:}") String confirmBucket,
            @Value("${garagest.trip-evidence.adoption.confirm-candidates:}") String confirmCandidates) {
        this.adoption = adoption;
        this.mediaRepository = mediaRepository;
        this.jdbc = jdbc;
        this.r2 = r2;
        // raw strings only: nothing is parsed (and nothing can throw) while the application starts
        this.runMode = runMode;
        this.jobCardId = jobCardId;
        this.maxJobCards = maxJobCards;
        this.confirmDatabase = trim(confirmDatabase);
        this.confirmBucket = trim(confirmBucket);
        this.confirmCandidates = trim(confirmCandidates);
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread worker = new Thread(() -> {
            try {
                execute();
            } catch (RuntimeException e) {
                log.error("[TRIP-EVIDENCE][RUNNER] Run failed unexpectedly.", e);
            }
        }, "trip-evidence-adoption-runner");
        worker.setDaemon(true);
        worker.start();
    }

    /** Synchronous body of one start (the thread above only calls this). */
    public Summary execute() {

        // 0. configuration (never throws; a bad value refuses the run)
        Mode mode = parseMode(runMode);

        if (mode == null) {
            return refuse(null, null, null, 0, "unknown mode (use dry-run, execute-keep-old or execute)");
        }

        Long onlyJobCard = null;
        if (!trim(jobCardId).isEmpty()) {
            onlyJobCard = parsePositiveLong(jobCardId);
            if (onlyJobCard == null) {
                return refuse(mode, null, null, 0, "job-card-id must be a positive whole number");
            }
        }

        int cap = DEFAULT_MAX_JOB_CARDS;
        if (!trim(maxJobCards).isEmpty()) {
            Long parsed = parsePositiveLong(maxJobCards);
            if (parsed == null || parsed > MAX_JOB_CARDS_LIMIT) {
                return refuse(mode, null, null, 0, "max-job-cards must be a whole number from 1 to " + MAX_JOB_CARDS_LIMIT);
            }
            cap = parsed.intValue();
        }

        String database = jdbc.queryForObject("select current_database()", String.class);
        String bucket = bucketOrNull();

        // 1. read-only pass: manifest + candidate count (DRY_RUN is the only mode ever used here)
        List<Long> jobCardIds = onlyJobCard != null ? List.of(onlyJobCard) : candidateJobCards(cap);
        List<Report> plan = new ArrayList<>();
        long candidates = 0;

        for (Long id : jobCardIds) {
            Report report = adoption.run(id, Mode.DRY_RUN);
            plan.add(report);
            candidates += report.candidates();
            report.items().stream()
                    .filter(i -> i.outcome() != Outcome.ALREADY_ADOPTED)
                    .forEach(i -> log.info("[TRIP-EVIDENCE][RUNNER][MANIFEST] jobCardId={}, mediaId={}, stage={}, from={}, to={}, outcome={}",
                            i.jobCardId(), i.mediaId(), i.stage(), i.fromKey(), i.toKey(), i.outcome()));
        }

        long oldMissing = plan.stream().mapToLong(r -> r.count(Outcome.OLD_MISSING)).sum();
        long mismatched = plan.stream().mapToLong(r -> r.count(Outcome.GARAGE_MISMATCH)).sum();

        log.info("[TRIP-EVIDENCE][RUNNER] Plan. mode={}, database={}, bucket={}, jobCards={}, candidates={}, oldObjectMissing={}, garageMismatch={}",
                mode, database, bucket, jobCardIds.size(), candidates, oldMissing, mismatched);

        if (mode == Mode.DRY_RUN) {
            log.info("[TRIP-EVIDENCE][RUNNER] Dry run finished: no object, database or audit change was made. To execute, set confirm-database={}, confirm-bucket={} and confirm-candidates={}.",
                    database, bucket, candidates);
            return new Summary(mode, database, bucket, candidates, false, null, 0, 0, 0);
        }

        // 2. execution guards
        if (database == null || !database.equals(confirmDatabase)) {
            return refuse(mode, database, bucket, candidates, "confirm-database does not match the connected database");
        }
        if (bucket == null || bucket.isBlank()) {
            return refuse(mode, database, bucket, candidates, "the object-storage bucket cannot be established from the provider configuration");
        }
        if (!bucket.equals(confirmBucket)) {
            return refuse(mode, database, bucket, candidates, "confirm-bucket does not match the configured bucket");
        }
        if (!String.valueOf(candidates).equals(confirmCandidates)) {
            return refuse(mode, database, bucket, candidates, "confirm-candidates does not match the current candidate count");
        }

        // 3. execution
        long switched = 0;
        long failed = 0;
        long attention = 0;

        for (Long id : jobCardIds) {
            Report report = adoption.run(id, mode);
            switched += report.switched();
            failed += report.count(Outcome.FAILED, Outcome.OLD_MISSING, Outcome.TARGET_COLLISION);
            attention += report.needAttention();
        }

        log.info("[TRIP-EVIDENCE][RUNNER] Execution finished. mode={}, database={}, bucket={}, switched={}, failed={}, needAttention={}",
                mode, database, bucket, switched, failed, attention);

        return new Summary(mode, database, bucket, candidates, false, null, switched, failed, attention);
    }

    private Summary refuse(Mode mode, String database, String bucket, long candidates, String reason) {
        log.error("[TRIP-EVIDENCE][RUNNER] Run REFUSED: {}. database={}, bucket={}, candidates={}", reason, database, bucket, candidates);
        return new Summary(mode, database, bucket, candidates, true, reason, 0, 0, 0);
    }

    private String bucketOrNull() {
        try {
            return r2 == null ? null : r2.configuredBucketName();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private List<Long> candidateJobCards(int cap) {

        List<Long> ids = new ArrayList<>();
        long after = 0L;

        while (ids.size() < cap) {
            List<Long> page = mediaRepository.findJobCardIdsWithUnadoptedEvidence(after, PageRequest.of(0, Math.min(50, cap - ids.size())));
            if (page.isEmpty()) {
                break;
            }
            ids.addAll(page);
            after = page.get(page.size() - 1);
        }

        return ids;
    }

    private static Mode parseMode(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toLowerCase()) {
            case "dry-run" -> Mode.DRY_RUN;
            case "execute-keep-old" -> Mode.EXECUTE_KEEP_OLD;
            case "execute" -> Mode.EXECUTE;
            default -> null;
        };
    }

    /** A strictly positive whole number, or null (letters, signs, decimals, zero and values beyond a long are all null). */
    private static Long parsePositiveLong(String value) {
        String v = trim(value);
        if (!v.matches("[0-9]{1,18}")) {
            return null;
        }
        long parsed = Long.parseLong(v);
        return parsed > 0 ? parsed : null;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
