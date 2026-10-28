package com.garageos.modules.navigation.service;

import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.core.enums.navigation.TripMediaStage;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.entity.NavigationTripMedia;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripMediaRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import com.garageos.modules.navigation.storage.CloudAwareMediaStorageService;
import com.garageos.modules.navigation.storage.MediaStorageService;
import com.garageos.modules.navigation.storage.MediaStorageService.DeleteResult;
import com.garageos.modules.navigation.storage.TripEvidenceImages;
import com.garageos.modules.navigation.storage.TripEvidenceKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Moves trip evidence (pickup photos under the provisional {@code TRIP-<tripId>} folder, or under the legacy
 * {@code trip-evidence/} prefix) into the job card's folder once the job card exists and is linked to the trip's
 * request. It is idempotent and safe to re-run from any state, which is what makes the recovery touchpoints and the
 * opt-in operator runner possible without any new status or column.
 *
 * <h3>Per photo, in {@link Mode#EXECUTE} / {@link Mode#EXECUTE_KEEP_OLD}</h3>
 * <ol>
 *   <li>read the old object; an existing object at the target is reused only if its bytes are identical. A target that
 *       differs is <b>never</b> overwritten while another row references it ({@link Outcome#TARGET_COLLISION}); an
 *       unreferenced differing target can only be a stale or partial copy of this same photo left by an interrupted run
 *       (the target name is deterministic), and is replaced and verified;</li>
 *   <li>copy and verify the copy byte for byte;</li>
 *   <li>repoint the row with a <b>compare-and-set</b> update (only if the row still holds the old key): a concurrent or
 *       repeated run that loses the race changes nothing and never deletes anything;</li>
 *   <li>write the audit event for the switch <b>immediately</b>, stating how the copy was obtained and what will happen to
 *       the old object (delete pending / kept because of keep-old mode / kept because another row references it);</li>
 *   <li>only {@link Mode#EXECUTE}, only when no row references the old key any more and the new object is confirmed to
 *       exist, delete the old object through {@link MediaStorageService#deleteObject} and record the real result
 *       (deleted, already absent, or failed: a failed deletion is never reported as a deletion).</li>
 * </ol>
 * {@link Mode#DRY_RUN} only reads (existence checks): no object write or delete, no database change, and no audit event
 * even when a check fails.
 *
 * <h3>Canonical naming and thumbnails</h3>
 * An image is not just moved: it is written under the job-card naming convention
 * ({@code <jobCardNumber>_<STAGE>_<seq>_evidence.jpg}, sequence = 1 + the number of lower-id photos of that job card and
 * stage, so retries and concurrent runs always produce the same key) together with a genuine 320 px thumbnail
 * ({@code ..._thumbnail.jpg}). The evidence is the upright JPEG without any burned-in text. Evidence <b>and</b> thumbnail
 * are verified before the row is switched, the switch changes key, content type and size in one compare-and-set, and the
 * old object is deleted only after the database no longer references it and both new objects are confirmed. A video or an
 * image that cannot be decoded keeps the previous behaviour (moved as it is into the job-card folder).
 *
 * <h3>Two policies</h3>
 * An <b>operator run</b> ({@link #run(Long, Mode)}) retries each storage call a bounded number of times. A
 * <b>touchpoint</b> ({@link #adoptForJobCard(Long)}, called after a trip completes and at the start of report
 * generation) makes a single attempt per call and stops the whole run at the first failure, so a storage outage costs one
 * failed call, not one per photo; the next touchpoint tries again. A touchpoint only adopts <b>provisional</b>
 * ({@code TRIP-<id>}) photos; legacy and UUID-keyed photos are left alone ({@link Outcome#SKIPPED_OUT_OF_SCOPE}) and can
 * only be migrated by an operator run, so no ordinary upload or report request ever migrates old data in bulk. Identical failure audit events for one photo are
 * throttled (at most one per {@code failureAuditWindow} per process). This service never throws for a storage problem.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripEvidenceAdoptionService {

    public enum Mode { DRY_RUN, EXECUTE_KEEP_OLD, EXECUTE }

    public enum Outcome {
        ALREADY_ADOPTED, WOULD_ADOPT, WOULD_SWITCH_EXISTING_COPY, ADOPTED, ADOPTED_OLD_KEPT, ADOPTED_OLD_DELETE_FAILED,
        LOST_RACE, TARGET_COLLISION, OLD_MISSING, GARAGE_MISMATCH, SKIPPED_LOCAL_KEY, SKIPPED_OUT_OF_SCOPE, FAILED
    }

    /** One photo in a run (ids and keys only: no URLs, names or other personal data). */
    public record Item(Long mediaId, Long jobCardId, TripMediaStage stage, String fromKey, String toKey,
                       Outcome outcome, String detail) {
    }

    public record Report(Mode mode, Long jobCardId, List<Item> items) {

        public long count(Outcome... outcomes) {
            Set<Outcome> wanted = Set.of(outcomes);
            return items.stream().filter(i -> wanted.contains(i.outcome())).count();
        }

        /** Photos an execution would move (candidates). */
        public long candidates() {
            return count(Outcome.WOULD_ADOPT, Outcome.WOULD_SWITCH_EXISTING_COPY);
        }

        /** Photos that were switched (whatever happened to the old object). */
        public long switched() {
            return count(Outcome.ADOPTED, Outcome.ADOPTED_OLD_KEPT, Outcome.ADOPTED_OLD_DELETE_FAILED);
        }

        /** Photos that need a person to look at them. */
        public long needAttention() {
            return count(Outcome.FAILED, Outcome.OLD_MISSING, Outcome.TARGET_COLLISION, Outcome.ADOPTED_OLD_DELETE_FAILED);
        }
    }

    /** Compact outcome of {@link #adoptForJobCard(Long)}, for logs and the existing callers. */
    public record Result(int adopted, int alreadyAdopted, int failed) {
    }

    /** A copy that does not match its source; never retried. */
    static final class VerificationFailedException extends RuntimeException {
        VerificationFailedException() {
            super("copy verification failed");
        }
    }

    /** The target key belongs to another row and holds different bytes: nothing may be overwritten. */
    static final class TargetCollisionException extends RuntimeException {
        TargetCollisionException() {
            super("target is referenced by another row");
        }
    }

    private final NavigationRequestRepository requestRepository;
    private final NavigationTripRepository tripRepository;
    private final NavigationTripMediaRepository mediaRepository;
    private final JobCardRepository jobCardRepository;
    private final GarageRepository garageRepository;
    private final MediaStorageService storage;
    private final AuditService auditService;

    /** Pauses between the bounded retries of one storage call in an operator run (so at most {@code length + 1} attempts). */
    private long[] backoffMillis = {300L, 1500L};

    /** In-process single flight per job card; the compare-and-set covers other instances. */
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    /** Last time a failure audit event was written per photo (in this process), to bound repeated identical events. */
    private final Map<Long, Instant> lastFailureAudit = new ConcurrentHashMap<>();
    private Duration failureAuditWindow = Duration.ofHours(1);
    private Supplier<Instant> clock = Instant::now;

    void setBackoffMillisForTests(long... millis) {
        this.backoffMillis = millis;
    }

    void setFailureAuditWindowForTests(Duration window, Supplier<Instant> clock) {
        this.failureAuditWindow = window;
        this.clock = clock;
    }

    /** Re-triggerable entry point used by the touchpoints: {@link Mode#EXECUTE}, single attempt, stops at the first failure. */
    public Result adoptForJobCard(Long jobCardId) {

        Report report = run(jobCardId, Mode.EXECUTE, true);

        return new Result((int) report.switched(),
                (int) report.count(Outcome.ALREADY_ADOPTED, Outcome.LOST_RACE),
                (int) (report.needAttention() - report.count(Outcome.ADOPTED_OLD_DELETE_FAILED)));
    }

    /** Operator entry point: bounded retries per storage call, continues with the next photo after a failure. */
    public Report run(Long jobCardId, Mode mode) {
        return run(jobCardId, mode, false);
    }

    private Report run(Long jobCardId, Mode mode, boolean touchpoint) {

        List<Item> items = new ArrayList<>();

        if (!inFlight.add(jobCardId)) {
            log.info("[TRIP-EVIDENCE] Adoption already running for this job card; skipped. jobCardId={}", jobCardId);
            return new Report(mode, jobCardId, items);
        }

        try {
            collect(jobCardId, mode, touchpoint, items);
        } finally {
            inFlight.remove(jobCardId);
        }

        Report report = new Report(mode, jobCardId, items);

        log.info("[TRIP-EVIDENCE] Run finished. mode={}, touchpoint={}, jobCardId={}, candidates={}, switched={}, alreadyAdopted={}, lostRace={}, oldMissing={}, collisions={}, deleteFailed={}, failed={}, garageMismatch={}",
                mode, touchpoint, jobCardId, report.candidates(), report.switched(),
                report.count(Outcome.ALREADY_ADOPTED), report.count(Outcome.LOST_RACE), report.count(Outcome.OLD_MISSING),
                report.count(Outcome.TARGET_COLLISION), report.count(Outcome.ADOPTED_OLD_DELETE_FAILED),
                report.count(Outcome.FAILED), report.count(Outcome.GARAGE_MISMATCH));

        return report;
    }

    private void collect(Long jobCardId, Mode mode, boolean touchpoint, List<Item> items) {

        JobCard jobCard = jobCardRepository.findById(jobCardId).orElse(null);

        if (jobCard == null || jobCard.getJobCardNumber() == null) {
            return;
        }

        Long jobCardGarageId = jobCardRepository.findGarageIdById(jobCardId).orElse(null);

        outer:
        for (NavigationRequest request : requestRepository.findByJobCardId(jobCardId)) {

            String garageCode = request.getGarageId() == null ? null
                    : garageRepository.findById(request.getGarageId()).map(Garage::getGarageCode).orElse(null);

            // Authoritative relationships only: the request's garage must be the job card's garage, and the job-card
            // number embeds that garage's code.
            boolean sameGarage = garageCode != null
                    && request.getGarageId().equals(jobCardGarageId)
                    && jobCard.getJobCardNumber().startsWith(garageCode + "-");

            for (NavigationTrip trip : tripRepository.findByNavigationRequestIdIn(List.of(request.getId()))) {
                for (NavigationTripMedia media : mediaRepository.findByTripIdOrderByCapturedAtAsc(trip.getId())) {

                    String oldKey = media.getStorageKey();

                    if (!sameGarage) {
                        items.add(item(media, jobCardId, oldKey, null, Outcome.GARAGE_MISMATCH,
                                "request garage does not match the job card"));
                        continue;
                    }

                    String folder = TripEvidenceKeys.folder(garageCode, jobCard.getJobCardNumber(), media.getMediaStage());

                    if (folder == null || oldKey == null || !CloudAwareMediaStorageService.isCloud(oldKey)) {
                        items.add(item(media, jobCardId, oldKey, null, Outcome.SKIPPED_LOCAL_KEY, "not an R2 key"));
                        continue;
                    }

                    boolean image = media.getContentType() != null && media.getContentType().toLowerCase().startsWith("image/");

                    // deterministic: only persisted rows decide the sequence, so every run computes the same key
                    String canonicalKey = !image ? null : TripEvidenceKeys.canonicalEvidenceKey(
                            garageCode, jobCard.getJobCardNumber(), media.getMediaStage(),
                            (int) mediaRepository.countStageRowsBefore(jobCardId, media.getMediaStage(), media.getId()) + 1);

                    String legacyKey = folder + "/" + TripEvidenceKeys.fileName(oldKey);
                    String newKey = canonicalKey != null ? canonicalKey : legacyKey;

                    try {
                        if (canonicalKey != null && oldKey.equals(canonicalKey)) {
                            // already canonical; only an operator run looks for a missing thumbnail (an extra storage call)
                            items.add(touchpoint
                                    ? item(media, jobCardId, oldKey, oldKey, Outcome.ALREADY_ADOPTED, null)
                                    : checkThumbnail(media, jobCardId, request.getGarageId(), oldKey, mode));
                            continue;
                        }

                        if (canonicalKey == null && TripEvidenceKeys.isInFolder(oldKey, folder)) {
                            items.add(item(media, jobCardId, oldKey, oldKey, Outcome.ALREADY_ADOPTED, null));
                            continue;
                        }

                        if (touchpoint && !TripEvidenceKeys.isProvisional(oldKey, garageCode)) {
                            items.add(item(media, jobCardId, oldKey, null, Outcome.SKIPPED_OUT_OF_SCOPE,
                                    "legacy or UUID key: operator migration only"));
                            continue;
                        }

                        items.add(mode == Mode.DRY_RUN
                                ? plan(media, jobCardId, oldKey, newKey, touchpoint)
                                : adoptOne(media, jobCardId, request.getGarageId(), oldKey, canonicalKey, legacyKey, mode, touchpoint));
                    } catch (TargetCollisionException ex) {
                        items.add(item(media, jobCardId, oldKey, newKey, Outcome.TARGET_COLLISION,
                                "target key is referenced by another row; nothing overwritten"));
                        log.warn("[TRIP-EVIDENCE] Target collision; photo left untouched. mediaId={}, jobCardId={}", media.getId(), jobCardId);
                        auditFailure(mode, media, request.getGarageId(), jobCardId, oldKey, newKey, "TARGET_COLLISION", "COPY");
                    } catch (RuntimeException ex) {
                        items.add(item(media, jobCardId, oldKey, newKey, Outcome.FAILED, ex.getClass().getSimpleName()));
                        log.warn("[TRIP-EVIDENCE] Adoption failed; the photo stays where it is and a later run retries. mediaId={}, jobCardId={}, reason={}",
                                media.getId(), jobCardId, ex.getClass().getSimpleName());
                        auditFailure(mode, media, request.getGarageId(), jobCardId, oldKey, newKey, ex.getClass().getSimpleName(), "COPY_OR_SWITCH");
                        if (touchpoint) {
                            // a storage / database problem: do not spend time on the remaining photos; the next touchpoint retries
                            log.warn("[TRIP-EVIDENCE] Touchpoint stopped at the first failure; remaining photos are left for the next attempt. jobCardId={}", jobCardId);
                            break outer;
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ dry run: reads only

    private Item plan(NavigationTripMedia media, Long jobCardId, String oldKey, String newKey, boolean touchpoint) {

        boolean oldExists = retrying(touchpoint, () -> storage.exists(oldKey));
        boolean targetExists = retrying(touchpoint, () -> storage.exists(newKey));

        if (oldExists) {
            return item(media, jobCardId, oldKey, newKey,
                    targetExists ? Outcome.WOULD_SWITCH_EXISTING_COPY : Outcome.WOULD_ADOPT, null);
        }

        return targetExists
                ? item(media, jobCardId, oldKey, newKey, Outcome.WOULD_SWITCH_EXISTING_COPY, "old object missing; copy exists")
                : item(media, jobCardId, oldKey, newKey, Outcome.OLD_MISSING, "neither object exists");
    }

    /** A photo that already has its canonical key: its thumbnail must exist; an operator run regenerates a missing one. */
    private Item checkThumbnail(NavigationTripMedia media, Long jobCardId, Long garageId, String key, Mode mode) {

        String thumbnailKey = TripEvidenceKeys.thumbnailKeyOf(key);

        if (retrying(false, () -> storage.exists(thumbnailKey))) {
            return item(media, jobCardId, key, key, Outcome.ALREADY_ADOPTED, null);
        }

        if (mode == Mode.DRY_RUN) {
            return item(media, jobCardId, key, key, Outcome.WOULD_ADOPT, "thumbnail missing");
        }

        if (!retrying(false, () -> storage.exists(key))) {
            return item(media, jobCardId, key, key, Outcome.OLD_MISSING, "evidence object missing");
        }

        byte[] evidence = retrying(false, () -> storage.readBytes(key));
        TripEvidenceImages.Rendition rendition = renderOrNull(evidence);

        if (rendition == null) {
            throw new VerificationFailedException();
        }

        ensureThumbnail(thumbnailKey, rendition, false);

        audit(AuditEventType.TRIP_EVIDENCE_ADOPTED, media, garageId,
                metadata(jobCardId, key, key, mode, "THUMBNAIL_REGENERATED", "thumbnail", thumbnailKey));

        return item(media, jobCardId, key, key, Outcome.ADOPTED, "thumbnail regenerated");
    }

    // ------------------------------------------------------------------ execution

    private Item adoptOne(NavigationTripMedia media, Long jobCardId, Long garageId, String oldKey,
                          String canonicalKey, String legacyKey, Mode mode, boolean touchpoint) {

        boolean oldExists = retrying(touchpoint, () -> storage.exists(oldKey));

        byte[] original = oldExists ? retrying(touchpoint, () -> storage.readBytes(oldKey)) : null;

        // an image is canonicalised (upright evidence + thumbnail); a video or an undecodable image keeps the old behaviour
        TripEvidenceImages.Rendition rendition = oldExists && canonicalKey != null ? renderOrNull(original) : null;

        // With the old object gone, an earlier run may have left its copy at the canonical key (this version) or at the
        // previous-version folder key: take whichever exists, the canonical one first.
        final String target;
        if (canonicalKey == null) {
            target = legacyKey;
        } else if (oldExists) {
            target = rendition != null ? canonicalKey : legacyKey;
        } else {
            target = retrying(touchpoint, () -> storage.exists(canonicalKey)) ? canonicalKey : legacyKey;
        }

        if (target.equals(oldKey)) {
            // an undecodable image that already sits in the job-card folder: nothing to canonicalise, nothing to move
            return item(media, jobCardId, oldKey, oldKey, Outcome.ALREADY_ADOPTED, "not a decodable image; kept as it is");
        }

        final boolean canonical = target.equals(canonicalKey);
        final String thumbnailKey = canonical ? TripEvidenceKeys.thumbnailKeyOf(target) : null;

        boolean targetExists = retrying(touchpoint, () -> storage.exists(target));

        String copy; // how the target object was obtained: WRITTEN, REUSED, REPLACED_UNREFERENCED_DIFFERENT, OLD_GONE_COPY_USED
        long evidenceLength;

        if (oldExists) {

            byte[] payload = rendition != null ? rendition.evidence() : original;
            evidenceLength = payload.length;

            boolean needCopy = true;
            copy = "WRITTEN";

            if (targetExists) {
                byte[] existing = retrying(touchpoint, () -> storage.readBytes(target));
                if (Arrays.equals(existing, payload)) {
                    needCopy = false;
                    copy = "REUSED";
                } else if (mediaRepository.countByStorageKey(target) > 0) {
                    // somebody else's object: never overwrite it
                    throw new TargetCollisionException();
                } else {
                    copy = "REPLACED_UNREFERENCED_DIFFERENT";
                }
            }

            if (needCopy) {
                retrying(touchpoint, () -> {
                    storage.putObject(target, payload, canonical ? "image/jpeg" : media.getContentType());
                    return Boolean.TRUE;
                });
                byte[] copied = retrying(touchpoint, () -> storage.readBytes(target));
                if (!Arrays.equals(payload, copied)) {
                    throw new VerificationFailedException();
                }
            }

            if (canonical) {
                ensureThumbnail(thumbnailKey, rendition, touchpoint);
            }

        } else if (targetExists) {
            // old object gone, an earlier run left a complete copy: it must at least be readable (and have its thumbnail)
            byte[] existing = retrying(touchpoint, () -> storage.readBytes(target));
            evidenceLength = existing.length;
            copy = "OLD_GONE_COPY_USED";

            if (canonical) {
                TripEvidenceImages.Rendition fromCopy = renderOrNull(existing);
                if (fromCopy == null) {
                    throw new VerificationFailedException();
                }
                ensureThumbnail(thumbnailKey, fromCopy, touchpoint);
            }
        } else {
            auditFailure(mode, media, garageId, jobCardId, oldKey, target, "OLD_AND_COPY_MISSING", "COPY");
            return item(media, jobCardId, oldKey, target, Outcome.OLD_MISSING, "neither object exists");
        }

        // compare-and-set: only the run that still sees the old key may switch it (and later delete the old object)
        final long length = evidenceLength;
        int updated = canonical
                ? retrying(touchpoint, () -> mediaRepository.updateEvidenceIfUnchanged(media.getId(), oldKey, target, "image/jpeg", length))
                : retrying(touchpoint, () -> mediaRepository.updateStorageKeyIfUnchanged(media.getId(), oldKey, target));

        if (updated == 0) {
            log.info("[TRIP-EVIDENCE] Another run already switched this photo; nothing deleted. mediaId={}", media.getId());
            return item(media, jobCardId, oldKey, target, Outcome.LOST_RACE, "key changed by another run");
        }

        media.setStorageKey(target);
        if (canonical) {
            media.setContentType("image/jpeg");
            media.setFileSize(length);
        }
        lastFailureAudit.remove(media.getId());

        // What will happen to the old object (decided before deleting, and recorded immediately after the switch).
        String disposition;
        if (!oldExists) {
            disposition = "ABSENT";
        } else if (mode == Mode.EXECUTE_KEEP_OLD) {
            disposition = "RETAINED_KEEP_OLD_MODE";
        } else if (mediaRepository.countByStorageKey(oldKey) > 0) {
            disposition = "RETAINED_REFERENCED_BY_OTHER_ROW";
        } else {
            disposition = "DELETE_PENDING";
        }

        String thumbnailInfo = thumbnailKey == null ? "NONE" : thumbnailKey;

        audit(AuditEventType.TRIP_EVIDENCE_ADOPTED, media, garageId, metadata(jobCardId, oldKey, target, mode,
                "SWITCHED", "copy", copy, "oldObject", disposition, "thumbnail", thumbnailInfo));

        log.info("[TRIP-EVIDENCE] Photo switched. mediaId={}, jobCardId={}, from={}, to={}, copy={}, oldObject={}",
                media.getId(), jobCardId, oldKey, target, copy, disposition);

        if (!"DELETE_PENDING".equals(disposition)) {
            return item(media, jobCardId, oldKey, target,
                    "ABSENT".equals(disposition) ? Outcome.ADOPTED : Outcome.ADOPTED_OLD_KEPT, disposition);
        }

        // never delete the old object unless the new evidence (and its thumbnail) are confirmed to be there
        DeleteResult deletion;
        try {
            boolean confirmed = retrying(touchpoint, () -> storage.exists(target))
                    && (thumbnailKey == null || retrying(touchpoint, () -> storage.exists(thumbnailKey)));
            deletion = confirmed ? storage.deleteObject(oldKey) : DeleteResult.FAILED;
        } catch (RuntimeException ex) {
            deletion = DeleteResult.FAILED;
        }

        switch (deletion) {
            case DELETED -> {
                audit(AuditEventType.TRIP_EVIDENCE_ADOPTED, media, garageId,
                        metadata(jobCardId, oldKey, target, mode, "OLD_DELETED", "oldObject", "DELETED"));
                return item(media, jobCardId, oldKey, target, Outcome.ADOPTED, null);
            }
            case ALREADY_ABSENT -> {
                audit(AuditEventType.TRIP_EVIDENCE_ADOPTED, media, garageId,
                        metadata(jobCardId, oldKey, target, mode, "OLD_ABSENT", "oldObject", "ABSENT"));
                return item(media, jobCardId, oldKey, target, Outcome.ADOPTED, "old object was already absent");
            }
            default -> {
                log.warn("[TRIP-EVIDENCE] Old object could not be deleted; it is left in place and recorded. mediaId={}, jobCardId={}", media.getId(), jobCardId);
                audit(AuditEventType.TRIP_EVIDENCE_ADOPTION_FAILED, media, garageId,
                        metadata(jobCardId, oldKey, target, mode, "OLD_DELETE_FAILED", "reason", "DELETE_FAILED", "oldObject", "RETAINED_DELETE_FAILED"));
                return item(media, jobCardId, oldKey, target, Outcome.ADOPTED_OLD_DELETE_FAILED, "old object retained: delete failed");
            }
        }
    }

    /**
     * Makes sure the thumbnail exists and is a genuine image of the expected size: an existing one is reused only if it
     * decodes to the expected dimensions; anything else (missing, partial, wrong) is rewritten and read back byte for byte.
     */
    private void ensureThumbnail(String thumbnailKey, TripEvidenceImages.Rendition rendition, boolean touchpoint) {

        if (retrying(touchpoint, () -> storage.exists(thumbnailKey))) {
            int[] dimensions = TripEvidenceImages.dimensions(retrying(touchpoint, () -> storage.readBytes(thumbnailKey)));
            if (dimensions != null && dimensions[0] == rendition.thumbnailWidth() && dimensions[1] == rendition.thumbnailHeight()) {
                return;
            }
        }

        retrying(touchpoint, () -> {
            storage.putObject(thumbnailKey, rendition.thumbnail(), "image/jpeg");
            return Boolean.TRUE;
        });

        byte[] written = retrying(touchpoint, () -> storage.readBytes(thumbnailKey));

        if (!Arrays.equals(rendition.thumbnail(), written)) {
            throw new VerificationFailedException();
        }
    }

    private static TripEvidenceImages.Rendition renderOrNull(byte[] bytes) {
        try {
            return TripEvidenceImages.render(bytes);
        } catch (java.io.IOException | RuntimeException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Map<String, Object> metadata(Long jobCardId, String from, String to, Mode mode, String phase, String... more) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jobCardId", jobCardId);
        m.put("from", from);
        m.put("to", to);
        m.put("mode", mode.name());
        m.put("phase", phase);
        for (int i = 0; i + 1 < more.length; i += 2) {
            m.put(more[i], more[i + 1]);
        }
        return m;
    }

    /** A failure audit event, never in DRY_RUN, and at most one per photo per window in this process. */
    private void auditFailure(Mode mode, NavigationTripMedia media, Long garageId, Long jobCardId,
                              String from, String to, String reason, String phase) {

        if (mode == Mode.DRY_RUN) {
            return;
        }

        Instant now = clock.get();
        Instant last = lastFailureAudit.get(media.getId());

        if (last != null && Duration.between(last, now).compareTo(failureAuditWindow) < 0) {
            return;
        }

        lastFailureAudit.put(media.getId(), now);
        audit(AuditEventType.TRIP_EVIDENCE_ADOPTION_FAILED, media, garageId,
                metadata(jobCardId, from, to, mode, phase, "reason", reason));
    }

    private void audit(AuditEventType type, NavigationTripMedia media, Long garageId, Map<String, Object> metadata) {
        try {
            auditService.record(type, "NavigationTripMedia", media.getId(), garageId, metadata);
        } catch (RuntimeException ex) {
            // observability must never undo or block the adoption
            log.warn("[TRIP-EVIDENCE] Audit record could not be written. mediaId={}, reason={}", media.getId(), ex.getClass().getSimpleName());
        }
    }

    private static Item item(NavigationTripMedia media, Long jobCardId, String from, String to, Outcome outcome, String detail) {
        return new Item(media.getId(), jobCardId, media.getMediaStage(), from, to, outcome, detail);
    }

    /**
     * Bounded retry for storage / database calls: a touchpoint makes one attempt; an operator run retries with the
     * configured pauses. A failed byte verification is never retried.
     */
    private <T> T retrying(boolean touchpoint, Supplier<T> operation) {

        int pauses = touchpoint ? 0 : backoffMillis.length;
        RuntimeException last = null;

        for (int attempt = 0; attempt <= pauses; attempt++) {
            try {
                return operation.get();
            } catch (VerificationFailedException | TargetCollisionException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt < pauses && backoffMillis[attempt] > 0) {
                    try {
                        Thread.sleep(backoffMillis[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                }
            }
        }

        throw last;
    }
}
