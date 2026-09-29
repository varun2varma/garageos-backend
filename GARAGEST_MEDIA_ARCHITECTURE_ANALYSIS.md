# GarageST Media Architecture Analysis

*Prepared as a repository-evidence-based architecture review. No source code was modified to produce this report.*

---

## 1. Executive Summary

GarageST's current media system uploads job-card photos/videos from the Flutter app, through the Spring Boot backend, into a single shared Google Drive account. Playback works the same way in reverse: the backend downloads the full file from Drive into JVM memory and re-serves it to the mobile client as one buffered HTTP response.

**Biggest problems, in order of production impact:**

1. **Media bytes fully transit and buffer through Spring Boot in both directions**, with no HTTP Range/streaming support anywhere in the Drive layer (`GoogleDriveFileService.downloadFile` javadoc explicitly documents this as a known limitation). A ~19MB video causes the client to abort mid-download; the resulting `ClientAbortException` falls through to the application's generic exception handler, which then tries to write a JSON `ApiResponse` body onto a response whose `Content-Type` is already committed as `video/mp4` — producing the exact `No converter for [class ApiResponse] with preset Content-Type 'video/mp4'` error observed in production. This is a design defect, not a memory/timeout tuning problem.
2. **The "only 1 of 3 photos shows up" bug is a Flutter-side selection-state bug, not a backend or data-model defect.** The committed (HEAD) version of `job_step_screen.dart` stores picked media in a single nullable `XFile? _selectedMedia` field that gets overwritten on every pick — so only the last-picked photo is ever uploaded. **A correct fix (list-based selection + `pickMultiImage` + per-file upload loop) already exists in the working tree but is uncommitted.** Every other link in the chain (backend upload endpoint, DB schema, listing query, Flutter response model, gallery rendering) was independently traced and found structurally correct — they all already support many-items-per-stage.
3. **There is no persistent upload queue.** Upload is synchronous, sequential, and scoped entirely to `JobStepScreen`'s widget state. Backgrounding the app, navigating away, or an OS kill during upload has no defined recovery path.
4. **The media schema is Drive-specific**, not storage-provider-neutral (`drive_file_id`, `drive_web_view_link` are baked into `JobCardMedia`), which would require either overloading those columns or an additive migration to introduce a second storage backend.
5. **No media processing pipeline exists** — no thumbnails, no transcoding, no compression before upload. Camera-original files (a 1-second 4K video from a modern phone can be ~19MB) are uploaded and stored as-is.

**Target architecture, at a glance:** move Spring Boot out of the media byte-pipe entirely. Flutter compresses/normalizes media locally, uploads directly to object storage (Cloudflare R2 evaluated as the concrete candidate) via short-lived signed URLs issued by Spring Boot, and plays media back through a Cloudflare-fronted, access-controlled URL — never proxied through the JVM. Spring Boot's role becomes authorization, metadata, and lifecycle state, exactly as this task's brief proposes. A persistent local upload queue (survivable across navigation/backgrounding/restart) replaces the current widget-scoped, synchronous upload call.

**Most important recommendations:**
- Fix and commit the already-drafted Flutter multi-select fix immediately — it is low-risk, already written, and resolves a real user-facing bug today, independent of any storage migration.
- Introduce a `MediaStorageService` abstraction in the backend before touching storage providers — the existing navigation module already demonstrates this exact pattern (`storageKey`-based, provider-agnostic) and the Drive call sites needing to move behind it are confined to 3 files.
- Do not attempt to fix the streaming bug by increasing Tomcat/Render memory or timeouts — replace the proxy-through-Spring pattern with direct client↔storage transfer.
- Introduce a persistent (disk-backed) upload queue in Flutter before or alongside the storage migration — this is an independent, high-value fix that does not require R2/Drive changes to deliver value.
- Treat R2 migration as an additive, dual-provider period (`storageProvider = GOOGLE_DRIVE | R2`), not a big-bang cutover, given GarageST already has real production data in Drive.

---

## 2. Repository Evidence

| Repository | Technology | Relevant modules |
|---|---|---|
| `D:\garageos-backend` | Spring Boot 4.0.7, Java 17, PostgreSQL, Flyway, hosted on Render | `modules/media` (JobCardMedia, GoogleDrive*), `modules/navigation` (NavigationTripMedia, its own `MediaStorageService`), `modules/jobcard`, `core/security` (SecurityConfig, JWT), `core/exception` (GlobalExceptionHandler) |
| `D:\garagest_flutter` | Flutter, feature-first (`lib/features/<name>/{models,services,screens,state,widgets}`) | `features/media` (MediaPickerSheet, MediaService, MediaGallery, MediaFullScreenViewer), `features/workflow/screens/job_step_screen.dart` (upload wiring), `features/navigation` (DriverLocationTracker — only existing background-execution precedent) |

All findings below are sourced from direct repository inspection (three parallel read-only investigations covering backend media/Drive/DB, Flutter media/upload architecture, and an end-to-end trace of the multiple-media bug). No file was modified to produce this report.

---

## 3. Current Media Architecture

```
Flutter (image_picker)
    ↓  XFile (local temp path, OS-managed)
JobStepScreen (widget-local state, in-memory)
    ↓  ApiClient.postMultipart()  — one file per HTTP request
Spring Boot: MediaController.uploadMedia()
    ↓  MediaServiceImpl.uploadMedia()
    ├─→ local disk buffer (MediaStorageService, PENDING_UPLOAD_FOLDER)  [durability buffer]
    └─→ JobCardMedia row saved (status=PENDING) — INSERT, always
    ↓  MediaUploadRetryScheduler (@Scheduled, 15s) drives the actual attempt
GoogleDriveFileServiceImpl.uploadFile() / uploadBytes()
    ↓
Google Drive (single shared service-account-owned Drive, folder-per-garage → per-job-card → per-stage)

Playback:
Flutter → GET /api/v1/job-cards/{id}/media/{mediaId}/content
    ↓
MediaController.getMediaContent() → MediaServiceImpl → GoogleDriveFileService.downloadFile()
    ↓  buffers ENTIRE file into a ByteArrayOutputStream (no Range, no streaming)
Spring returns ResponseEntity<byte[]> with Content-Type set from Drive's stored contentType
    ↓
Flutter Image.network() / VideoPlayerController.networkUrl() — re-downloaded on every view, no disk cache
```

**Stage-by-stage:**
- **Selection**: `image_picker`'s `pickImage`/`pickVideo`/`pickMedia`/`pickMultiImage`, wrapped by `MediaPickerSheet` (selection-only, by design).
- **Local staging**: picked files referenced by `XFile.path` — `image_picker`'s own OS-managed temp file, never copied into an app-managed directory, never explicitly deleted after upload.
- **Upload trigger**: only after the job-card save itself succeeds (`job_step_screen.dart`'s save-then-upload sequencing, matching this repo's documented convention), and only while the screen is mounted and awaiting.
- **Backend acceptance**: durable-first design — the `JobCardMedia` row is saved with `status=PENDING` *before* the Drive upload is attempted, and a dedicated `@Scheduled` retry loop (`MediaUploadRetryScheduler`, every 15s) drives the actual Drive upload asynchronously from the request. This is a genuinely good pattern already in place — the request path does not block on Drive.
- **Storage**: one shared Google Drive account (OAuth credential stored in Postgres, keyed `"garagest-drive"`), folder hierarchy `garage → job card → stage`.
- **Playback**: full-file buffer-then-return, proxied through Spring Boot — the architectural root of the crash described in §12.

---

## 4. Current Media Data Model

**`job_card_media`** (`JobCardMedia` entity), migrations `V31`→`V33`→`V56`:

| Field | Notes |
|---|---|
| `id` | PK, IDENTITY |
| `job_card_id` | plain `Long` FK column, no JPA relation |
| `repair_task_id` | nullable, optional narrowing FK |
| `uploaded_by` | plain `Long`, no relation |
| `media_type` | enum: IMAGE / VIDEO |
| `media_stage` | enum: e.g. BEFORE_SERVICE / DURING_REPAIR / AFTER_REPAIR |
| `visibility` | enum: INTERNAL / CUSTOMER_VISIBLE |
| `drive_file_id`, `drive_web_view_link` | **Drive-specific**, not generic |
| `local_storage_path` | transient pending-upload buffer only |
| `upload_status`, `retry_count`, `next_retry_at`, `last_error` | added V56 — durable-upload/retry state |
| `content_type`, `file_size` | |
| `created_at` | no `updated_at` |

No `garage_id` column (resolved indirectly via the job card at authorization time), no duration, no GPS/lat-long, no capture-timestamp.

**`navigation_trip_media`** (`NavigationTripMedia`, separate table) — architecturally more mature: `storage_key` (generic, not provider-named), `trip_id`, `media_stage`, `shot_type`, `content_type`, `file_size`, `captured_by`, `captured_at`, `latitude`, `longitude`. Uses its own `com.garageos.modules.navigation.storage.MediaStorageService` interface (`upload`/`getUrl`/`readBytes`) — **the only existing storage-abstraction interface in the codebase**, and notably already provider-neutral.

**Verdict: (A) storage-provider dependent for job-card media, (B) storage-provider neutral for navigation media.** The navigation module is the template to generalize, not a green-field design.

---

## 5. Current Upload Flow

```
User                MediaPickerSheet         JobStepScreen              MediaService            Backend
 │  tap "Add media"        │                       │                        │                    │
 ├─────────────────────────▶ pickMultiImage()       │                        │                    │
 │                          │──────List<XFile>─────▶│ _selectedMedia.addAll │                    │
 │  tap "Upload/Save"       │                       │                        │                    │
 ├──────────────────────────┼───────────────────────▶ while(!empty):         │                    │
 │                          │                       │   uploadMedia(file[0]) │                    │
 │                          │                       ├───────────────────────▶ postMultipart()     │
 │                          │                       │                        ├────────────────────▶ INSERT JobCardMedia
 │                          │                       │                        │                    │  (status=PENDING)
 │                          │                       │◀───────── 201 ─────────┤◀───────────────────┤
 │                          │                       │   removeAt(0); repeat  │                    │
 │                          │                       │  (await'ed, sequential)│                    │
 │                          │                       │                        │                    │
 │                          │                       │                        │      [async, 15s later]
 │                          │                       │                        │        MediaUploadRetryScheduler
 │                          │                       │                        │          → Drive upload
```

Confirmed properties:
- **Synchronous relative to UI**: yes — the screen `await`s the full loop before proceeding.
- **Queue**: none — `_selectedMedia`/`_isUploadingMedia` are plain widget `State` fields, in-memory only.
- **Retry (client-side)**: not implemented. A server-signaled "queued for retry" response is recognized and treated as non-error, but this is not a client retry mechanism.
- **Backoff, progress %, cancellation**: not implemented.
- **Idempotency**: each upload call is a fresh INSERT; no idempotency key. A retried client request after a lost response would create a duplicate row.
- **Local cleanup**: picked temp files are never explicitly deleted after upload.

---

## 6. Current Playback Flow

```
Flutter (MediaFullScreenViewer)              Spring Boot                          Google Drive
 │  Image.network(url) / VideoPlayerController │                                       │
 ├──────────────GET .../media/{id}/content─────▶ MediaController.getMediaContent()     │
 │                                              │  → authorizeEmployeeAccess() (tenant  │
 │                                              │     + job-card-ownership check)       │
 │                                              │  → MediaServiceImpl.downloadContent() │
 │                                              ├───────────────────────────────────────▶ files.get(id).executeMediaAsInputStream()
 │                                              │◀───────── full byte stream ───────────┤
 │                                              │  transferTo(ByteArrayOutputStream)    │
 │                                              │  (ENTIRE file now in JVM heap)         │
 │◀─────── ResponseEntity<byte[]>, Content-Type set from stored contentType ────────────┤
 │  (no Range support either direction; large file write can be aborted mid-stream)     │
```

If the client aborts mid-write (slow connection, user backs out, video too large): Tomcat throws `ClientAbortException`, which is **not** caught by any specific `@ExceptionHandler` in `GlobalExceptionHandler` and falls to the generic `Exception` handler, which tries to write a JSON `ApiResponse<Void>` onto a response whose `Content-Type` is already committed as `video/mp4` — Spring's converter negotiation fails, producing the exact "No converter for [class ApiResponse] with preset Content-Type 'video/mp4'" seen in production logs.

No thumbnail endpoint exists — the Flutter gallery grid shows a static play-icon placeholder for videos specifically because fetching a real thumbnail would mean downloading the whole file. No disk-level caching exists on the Flutter side (`cached_network_image`/`flutter_cache_manager` not present in `pubspec.yaml`) — every view re-downloads.

---

## 7. Current Google Drive Integration

| Aspect | Finding |
|---|---|
| Config/credentials | `GoogleDriveCredential` entity, Postgres-backed via `GoogleDriveCredentialDataStore`, single row keyed `"garagest-drive"` (replaced a prior local-disk store that didn't survive Render restarts) |
| Client construction | `GoogleDriveClientService` — builds `Drive` client from stored OAuth `Credential`; `executeWithAuthRetry()` does exactly one refresh+retry on 401/403 |
| Folder structure | `GoogleDriveFolderService.getOrCreateGarageFolder → getOrCreateJobCardFolder → getOrCreateStageFolder` — confirmed folder-per-garage → per-job-card → per-stage, get-or-create (folder-level dedup only) |
| Upload | `GoogleDriveFileServiceImpl.uploadFile()` streams via `InputStreamContent` (not fully buffered); `uploadBytes()` (retry path) is a full `byte[]` buffered upload |
| Download | `downloadFile()` **fully buffers into memory** — own javadoc states it "does not support HTTP Range requests, so large videos are not partially streamed either to this server or on to the client" — a documented, known limitation |
| Endpoints | `/api/v1/media/google/{authorize,callback,test,test/create-folder,test/create-job-card-structure}` — OAuth plumbing only, publicly reachable per `SecurityConfig`'s `permitAll` (scoped correctly to just this OAuth path, not the actual media upload/download endpoints) |
| Deletion | **No file-deletion capability exists anywhere** — searched exhaustively; the only delete method found removes the stored OAuth *credential* row, not a Drive file |
| Duplicate handling | Folder-existence dedup only; no content/checksum dedup |
| Video/image support | Both — `mediaType` enum covers IMAGE/VIDEO uniformly, same Drive path |
| Navigation media | **Does not use Drive** — separate `storageKey`-based `MediaStorageService` (§4) |
| Should Drive remain? | Viable as an archive/backup target; not viable as the live playback path (see §12) |

---

## 8. Current Flutter Media Architecture

| Component | Responsibility |
|---|---|
| `MediaPickerSheet` (widget) | Selection only — camera photo/video, single gallery pick, and (`allowMultiple: true`) `pickMultiImage()` |
| `MediaService` (service) | Upload (`postMultipart`, one file per call) + fetch (`getJobCardMedia`, `contentUrl`, `authHeaders`, `updateVisibility`) — no selection logic |
| `JobCardMedia` (model) | Flat, one-row-per-media-item shape (`id, jobCardId, mediaStage, fileName, uploadStatus, ...`) — **not** a fixed-field-per-stage shape; structurally correct for many-per-stage |
| `MediaGallery` (widget) | Groups a `List<JobCardMedia>` into `Map<String, List<JobCardMedia>>` by stage for grid display — append-based, not overwrite |
| `MediaFullScreenViewer` (widget) | `Image.network` for photos, `video_player`'s `VideoPlayerController.networkUrl` for video — minimal player, no scrubber beyond built-in progress bar |
| `job_step_screen.dart` | The one integration point wiring picker → local list → upload loop (see §10 for its bug) |

The selection/upload separation described in this repo's own architecture is real and intact.

---

## 9. Current Android/iOS Background Behavior

**Android**: only one background-execution precedent exists — `DriverLocationTracker`, using `geolocator`'s built-in Android foreground-service support (`AndroidSettings.foregroundNotificationConfig`), with `FOREGROUND_SERVICE`/`FOREGROUND_SERVICE_LOCATION`/`POST_NOTIFICATIONS` permissions declared. Its own doc comment states it is screen-scoped and explicitly **not** "a fully independent background service that survives the app being swiped away." No `<service>` is declared directly in the app's own `AndroidManifest.xml` — the plugin merges its own. No `workmanager`/`flutter_background_service`/`background_fetch` dependency exists anywhere.

**iOS**: `Info.plist` declares only `NSLocationWhenInUseUsageDescription`, with an explicit comment "the app never asks for background location." **`UIBackgroundModes` is absent** — no background execution capability is configured on iOS at all. `NSCameraUsageDescription`/`NSPhotoLibraryUsageDescription` are also absent despite `image_picker` being used — flagged as a likely separate, pre-existing gap (an iOS device build would need these before camera/gallery pickers work), not something this report's scope covers fixing.

**Conclusion**: no existing background-upload plumbing to reuse on either platform. A persistent upload queue's *execution* model would need to be built from scratch; the location tracker demonstrates the Android foreground-service pattern is viable in this codebase, but is not itself reusable machinery.

---

## 10. Multiple-Media Investigation

**Root cause, confirmed with evidence, single link:** `lib/features/workflow/screens/job_step_screen.dart`.

The **committed (HEAD)** version of this file declares:
```dart
XFile? _selectedMedia;               // single nullable field, not a list
...
_selectedMedia = file;               // _addMedia() overwrites on every pick
```
Each new pick for the same stage silently discards the previous one. `_uploadSelectedMedia()` then uploads only that single surviving file. A user picking 3 "before_repair" photos has only the 3rd ever reach the backend — this matches the reported symptom exactly, and requires no backend or model explanation.

**Every other link in the chain was independently traced and found correct:**
- Flutter `image_picker` multi-select (`pickMultiImage`) exists and works.
- Backend upload endpoint does a plain INSERT per call — no upsert-by-stage, no unique constraint on `(job_card_id, media_stage)` that would cause a silent overwrite.
- Backend listing query (`findByJobCardIdOrderByCreatedAtAsc`) returns `List<JobCardMedia>` with no grouping/collapse.
- Flutter's `JobCardMedia` model is a flat per-item shape, not a `{beforeRepair, afterRepair}` singleton container.
- `MediaGallery` groups into `Map<String, List<JobCardMedia>>` (append), renders every item in a stage via `GridView.builder`.

**Important, actionable finding**: the working tree (uncommitted) already contains a fix — `_selectedMedia` rewritten to `final List<XFile> _selectedMedia = []`, appended via `addAll`/`.add()`, uploaded via a `while` loop, wired to `MediaPickerSheet(allowMultiple: true)`. This fix is **correct** per the independent cross-repo trace (which read the working-tree files and found the full chain clean end-to-end), but it is **not committed** — whatever is currently deployed almost certainly still has the single-overwrite bug. This is pre-existing uncommitted work in the repository; per this project's own git-safety conventions it was not touched, staged, or committed as part of this analysis — it is flagged here for your attention and should be reviewed and committed deliberately.

Nothing about this bug required a display-side, deserialization-side, or backend fix — it lived entirely in one screen's local selection state.

---

## 11. Current Problems

### Critical
- **Media playback proxies fully through Spring Boot with no streaming/Range support**, causing production crashes on files in the tens-of-MB range (§6, §7, §12). This blocks reliable video playback today and will get worse as video usage grows.
- **No persistent upload queue** — an upload in progress does not survive navigation, backgrounding, or an app/process kill, with no defined recovery (§5, §9).

### High
- **Uncommitted multi-select fix** — real user-facing bug (§10), fix already exists, just not shipped. Highest-value/lowest-risk item to close immediately.
- **No media deletion capability anywhere** (§7) — files accumulate in Drive indefinitely with no cleanup path, including for content that should be removable (e.g. mis-uploaded media).
- **No compression/optimization before upload** — camera-original files (a 1-second 4K clip can be ~19MB) are uploaded and stored as-is, driving both the playback crash and unnecessary storage/bandwidth cost.

### Medium
- **Drive-coupled schema** (`drive_file_id`, `drive_web_view_link` baked into `JobCardMedia`) — makes introducing a second storage provider more invasive than it needs to be; the navigation module's `storageKey` pattern is the better template.
- **No idempotency on upload** — a retried client request after a lost response creates a duplicate row (no dedup key).
- **No thumbnails** — video grid tiles show a static placeholder; this is a direct consequence of the proxy-through-server download cost, not a separate defect.

### Low
- **Picked local temp files are never explicitly cleaned up** after upload — a real but low-impact leak of the OS's own temp storage.
- **No per-file upload progress** (only a boolean spinner) — UX polish, not correctness.

Severity basis: Critical = actively causing production failures or data-loss-adjacent gaps today; High = confirmed defects with a real user impact and a known/available fix; Medium = architectural debt that increases the cost of the next change but isn't failing today; Low = cosmetic/UX gaps.

---

## 12. Root Cause Analysis

- **Backend media streaming**: the root cause is architectural, not a resource-limit problem — `downloadFile()` was written as a full in-memory buffer with an explicit, self-documented "known limitation" comment acknowledging no Range/streaming support. Increasing heap or Tomcat timeouts would only raise the failure threshold, not remove it, and does nothing for the exception-handling mismatch (a generic handler trying to write JSON onto an already-committed `video/mp4` response).
- **Large video size**: modern phone cameras (e.g. S24 Ultra) can produce ~19MB for a 1-second clip at high bitrate/resolution — this is a capture-side reality GarageST must design around (local optimization before upload), not something the backend can absorb by scaling up.
- **Screen-bound upload**: `_selectedMedia`/`_isUploadingMedia` living in `JobStepScreen`'s `State` is a direct architectural consequence of there being no persistent queue/store anywhere in the Flutter app for media (no `sqflite`/`hive`, no `path_provider`-based staging area) — the upload has nowhere else to live.
- **Missing persistent queue**: same root cause as above — no local persistence layer exists in the app at all for this purpose; one must be built from scratch (§20).
- **Multiple-media handling**: single-field vs. list, confined entirely to one screen's local state (§10) — already fixed in the uncommitted working tree.
- **Storage coupling**: `JobCardMedia`'s Drive-named columns were designed for a single-provider world; the navigation module's parallel, already-neutral design shows this was a solvable problem the job-card media path simply didn't adopt.

---

## 13. Proposed Target Architecture

```
Flutter Camera/Gallery
    ↓
Local optimizer (compress/transcode to target spec)
    ↓
Persistent local upload queue (disk-backed, survives navigation/background/restart)
    ↓
Spring Boot: authorization + signed-upload-URL issuance (metadata row created, status=PENDING)
    ↓
Direct upload (Flutter → object storage, bytes never touch Spring Boot)
    ↓
Cloudflare R2 (canonical storage)
    ↓
Async processing worker (thumbnail generation, playback-asset normalization)
    ↓
Cloudflare edge cache / CDN, access-gated (signed/tokenized, not public)
    ↓
Flutter (playback via controlled URL)
    ↓
Local Flutter disk cache (avoid re-download on repeat views)
```

Spring Boot's role narrows to: authentication, authorization (tenant/job-card/role checks — already implemented well today, see §7), media metadata, upload-authorization/signed-URL issuance, status/lifecycle tracking, listing, deletion authorization, and audit logging. It stops being the pipe media bytes flow through.

---

## 14. Proposed Upload Flow

1. Flutter captures/selects media → written to a local optimization pipeline (compress photo/video to target spec, §21).
2. Optimized file is enqueued in a **persistent local upload queue** (state machine, §16) — enqueue happens before any network call, so the item survives immediately.
3. Queue worker (independent of any screen) calls Spring Boot: `POST /media/upload-intent` with job-card id, stage, content-type, size → backend performs the same tenant/job-card/role authorization already implemented in `MediaServiceImpl.authorizeEmployeeAccess()`, creates a `JobCardMedia` row (`status=PENDING`, `storageProvider=R2`), and returns a short-lived signed upload URL/token for R2.
4. Queue worker uploads the optimized bytes **directly to R2** using that signed URL — Spring Boot is not in this data path.
5. On successful direct upload, the queue worker calls Spring Boot: `POST /media/{id}/upload-complete` (checksum/size confirmation) → backend flips `status=UPLOADED` and enqueues async processing (thumbnail/playback-asset generation).
6. Processing worker generates a thumbnail (and, for video, a normalized playback asset if the original doesn't already meet target spec) → writes derived objects to R2 → backend flips `status=READY`.
7. Queue item is marked complete and removed from the local queue only after step 5 succeeds — a crash/kill before that point leaves the item `QUEUED`/`UPLOADING` on disk, to be resumed on next app launch.

This preserves the existing "durable-first" philosophy already present in the current backend design (row created before the slow operation) while removing Spring Boot from the byte path.

---

## 15. Proposed Playback Flow

1. Flutter requests media metadata/list from Spring Boot as today (`GET /job-cards/{id}/media`) — unchanged, this is metadata only and already fast.
2. For a specific item, Flutter requests a **playback access token/URL** from Spring Boot (re-using the existing tenant/job-card authorization check) rather than requesting content directly.
3. Spring Boot returns a short-lived signed URL (or a token to present to a Cloudflare Worker gateway) pointing at the R2 object via Cloudflare's edge — never a Spring-Boot-proxied byte stream.
4. Flutter fetches the media directly from Cloudflare's edge (CDN-cached on repeat access across users/devices, subject to the access gate re-validating the signed token).
5. Flutter's local disk cache (new capability, currently absent) stores the fetched asset keyed by media id, avoiding re-fetch on repeat views within the token's/cache's validity window.
6. Thumbnails are requested as a **separate, smaller derived asset** (from step 6 of the upload flow), not the full original — this is what makes video grid tiles finally show a real preview without downloading full video.

---

## 16. Proposed Media State Machine

```
LOCAL_CREATED
    ↓
QUEUED
    ↓
OPTIMIZING
    ↓
READY_TO_UPLOAD
    ↓
UPLOADING ──────────────┐
    ↓                   │ (failure)
UPLOADED                ▼
    ↓                 UPLOAD_FAILED (retry w/ backoff → back to READY_TO_UPLOAD)
PROCESSING ─────────────┐
    ↓                   │ (failure)
READY                   ▼
                    PROCESSING_FAILED (retry, or fall back to serving original as "ready" without derived assets)

(OPTIMIZING can also fail → OPTIMIZATION_FAILED → retry or fall back to uploading un-optimized original)
```

**Where each state lives:**
- `LOCAL_CREATED` → `READY_TO_UPLOAD` (and `OPTIMIZATION_FAILED`): **Flutter-local only** — the backend has no row yet; this is purely device-side queue state (mirrors this proposal's persistent local queue, §20).
- `UPLOADING` → `READY` (and `UPLOAD_FAILED`/`PROCESSING_FAILED`): **backend-tracked** (`JobCardMedia.uploadStatus`, extending the pattern already shipped in V56 — `PENDING`/`UPLOADING`/`RETRY_WAIT`/`AUTH_REQUIRED`/`COMPLETED`/`FAILED` already exists for the Drive-retry mechanism and generalizes directly to this state machine).
- Additional fields needed (backend): `retryCount`, `lastError`, `nextRetryAt` (**already exist** on `JobCardMedia` from V56 — reusable as-is), plus new: `storageProvider` (enum, replacing the implicit "always Drive" assumption), `storageKey` (generic, replacing/complementing `driveFileId`), `checksum` (for idempotency/integrity), `uploadSessionId` (to correlate a client-side queue item with its backend row across retries).
- Additional fields needed (Flutter-local queue): `localQueueId`, `state`, `retryCount`, `retryAt`, `optimizedFilePath`, `backendMediaId` (once known), `checksum`.

---

## 17. Proposed Database Model

*Conceptual — not a migration to run, not a code change.*

`job_card_media` (additive columns, backward compatible):

| Field | Change |
|---|---|
| `storage_provider` | **new**, enum `GOOGLE_DRIVE \| R2`, default `GOOGLE_DRIVE` for existing rows |
| `storage_key` | **new**, generic object key (nullable during transition, required for new R2 rows) |
| `drive_file_id`, `drive_web_view_link` | **kept, unchanged** — becomes the Drive-specific case rather than the only case |
| `checksum` | **new**, for upload integrity/idempotency |
| `upload_session_id` | **new**, correlates retried client requests to the same logical upload |
| `thumbnail_key` | **new**, nullable, populated once processing completes |
| `duration_seconds` | **new**, nullable, video-only |
| `upload_status`, `retry_count`, `next_retry_at`, `last_error` | **kept, unchanged** — same lifecycle columns, now covering both providers |

No removal of any existing column — this preserves every existing Drive-backed row's readability unchanged.

---

## 18. Proposed Storage Abstraction

```
MediaStorageService (interface)
 ├── generateUploadAuthorization(garageId, jobCardId, mediaId, contentType, sizeHint): UploadAuthorization
 ├── confirmUpload(storageKey): UploadResult   // size/checksum verification
 ├── generatePlaybackAccess(storageKey): PlaybackAccess   // short-lived signed URL/token
 ├── delete(storageKey): void
 └── exists(storageKey): boolean

 ├── R2MediaStorageService implements MediaStorageService
 │      — issues R2 presigned PUT URLs / Cloudflare Worker upload tokens
 │      — issues signed GET access (direct R2 presign, or a Worker-mediated token)
 │
 └── GoogleDriveMediaStorageService implements MediaStorageService
        — wraps the EXISTING GoogleDriveFileService/GoogleDriveClientService/GoogleDriveFolderService
          (already isolated to 3 files — see §7/§8's existing-abstraction-potential finding)
        — generatePlaybackAccess() for this provider is the one case that still proxies
          through Spring Boot (Drive has no clean signed-URL-to-arbitrary-client model
          without additional public-sharing configuration this codebase doesn't use) —
          acceptable ONLY for legacy/archival Drive-backed rows, not for new uploads
```

This exactly mirrors the pattern the navigation module already uses (`storageKey`-based `MediaStorageService` with `upload`/`getUrl`/`readBytes`) — the job-card media path did not adopt this pattern originally, but the precedent already exists in this codebase and should be generalized rather than invented fresh. Call-site impact is small: only `MediaServiceImpl` and `MediaUploadRetryServiceImpl` reference Drive classes directly today.

---

## 19. R2 / CDN Architecture

**Bucket**: one R2 bucket for GarageST media, private by default (R2's default — no public bucket access).

**Direct upload**: R2 supports S3-compatible presigned PUT URLs — Spring Boot generates one per upload (scoped to a specific object key, short TTL, e.g. 5–15 minutes), Flutter PUTs directly to R2. No Worker needed for the upload path itself.

**Access control for playback (the harder half)**: R2 objects are private; the choice is between:
- **R2 presigned GET URLs** (simplest): Spring Boot issues a short-lived presigned URL per playback request. Pro: no extra infrastructure. Con: every playback request round-trips through Spring Boot first (for authorization + URL issuance) — acceptable, since that round-trip is tiny (a URL, not the media bytes) and is exactly the "control plane, not byte plane" split this task calls for.
- **Cloudflare Worker gateway**: a Worker sits in front of a custom media domain, validates a short-lived token issued by Spring Boot, and streams from R2 (private binding, no public bucket exposure) with Cloudflare's edge cache in front. Pro: enables edge caching of frequently-viewed media (e.g. a photo viewed by both a technician and a customer) without re-issuing R2 credentials per view; cleaner custom-domain URLs. Con: additional infrastructure component to build/operate/monitor.

**Recommendation**: start with plain R2 presigned GET URLs (no Worker) for the first migration phase — it fully satisfies "media bytes don't pass through Spring Boot" and "media isn't public" with zero new infrastructure. Introduce a Worker gateway later specifically if/when edge-cache hit-rate on frequently-viewed media becomes a measurable cost or latency concern — this is a deliberately deferred optimization, not a day-one requirement, consistent with GarageST's early-stage-product cost/complexity balance.

**Object naming**: `garage/{garageId}/jobcard/{jobCardId}/media/{mediaId}/original.{ext}` for the canonical upload, `.../thumbnail.jpg` and `.../playback.mp4` for derived assets, mirroring the brief's suggested shape. Considerations:
- **Enumeration**: `mediaId` should be a UUID (or the existing opaque numeric PK, already not guessable in sequence-sensitive ways today) so a garage/job-card prefix leak doesn't let someone iterate mediaIds; access is gated by signed URL/token regardless, so the key itself is not the security boundary, but avoiding trivially-sequential IDs in the *key* still reduces blind-probing value if the private-bucket boundary were ever misconfigured.
- **Tenant isolation**: prefixing by `garageId` makes bulk operations (e.g. "export/delete everything for garage X") a simple prefix-list, which is valuable for both the not-yet-built deletion feature (§7) and any future garage-offboarding flow.
- **Migration**: this naming has no dependency on `driveFileId`, so Drive-backed rows simply don't have an R2 key until/unless migrated (§27) — no naming collision risk.
- **Debugging/cache**: human-readable path segments (vs. a flat hash) make it easy to correlate a support ticket ("garage 42, job card 1103") directly to storage without a DB lookup.

**Multipart uploads / resumability**: R2 supports S3-compatible multipart upload for large objects; relevant primarily for video before local optimization caps typical size (§21) — worth wiring once video sizes are confirmed to occasionally exceed single-PUT comfort (typically 100MB+); not needed for typical optimized output.

**Lifecycle rules**: R2 supports TTL-based object expiry — useful later for a "processing scratch" prefix (e.g. pre-optimization originals kept only long enough to regenerate a derived asset on failure) but not needed for canonical media, which has no fixed retention policy today (§10 of the privacy policy already states this honestly).

---

## 20. Background Upload Architecture

**Android**: the existing `DriverLocationTracker` demonstrates the codebase already knows how to run a `geolocator`-style Android foreground service with a persistent notification. A background upload mechanism needs its own, separate foreground-service wiring (uploads and location tracking are different lifecycles and shouldn't share a service) — realistically this means either `WorkManager` (Android's own recommended durable-background-task API, survives app kill/reboot, handles network-constraint retries natively) for the actual upload execution, paired with a lightweight foreground service + notification only while an upload is actively in flight (to satisfy Android's background-execution restrictions on newer API levels for user-initiated data transfer). `WorkManager` is the right primitive here, not a bespoke long-running service — it already has retry/backoff/constraint semantics built in, which directly satisfies "retry" and "network loss" requirements without hand-rolling them.

**iOS**: `Info.plist` currently declares no `UIBackgroundModes` at all — there is no existing background-execution capability to build on for either uploads or anything else. `URLSession` background upload tasks (`URLSessionConfiguration.background(withIdentifier:)`) are the correct native mechanism — iOS itself manages the actual transfer even if the app is suspended/killed, and re-launches the app in the background to deliver completion callbacks. This requires: adding the appropriate background mode capability, and a native (Swift, via a platform channel or a Flutter plugin such as a background-upload-focused package) integration — **this does not exist in the repository today and must be built from scratch**; do not assume any iOS background capability is already present.

**Realistic sequencing**: build the persistent local queue's *data model and enqueue/dequeue logic* first (pure Dart, platform-agnostic) — that alone fixes "survives navigation" and "survives app restart with app still running/relaunched normally." Layer true "survives being force-killed by the OS mid-upload" on top per-platform (`WorkManager` / `URLSession` background tasks) as a second phase, since it requires native plugin work on both platforms and is the highest-effort, most platform-divergent piece of this whole architecture.

---

## 21. Media Optimization Strategy

**Current state (repository evidence)**: no compression library exists anywhere in `pubspec.yaml` (`flutter_image_compress`, `video_compress`, `ffmpeg_kit_flutter` — none found). `image_picker`'s own `pickMultiImage`/`pickImage` calls already pass `imageQuality: 85`/`maxWidth`/`maxHeight` constraints in the working-tree version (per the multi-select fix trace) for **photos** — so some client-side photo downsizing already exists via `image_picker`'s built-in resize/quality parameters, but **no equivalent exists for video** (`pickVideo` has no size/bitrate control — `image_picker` does not offer one), and no library re-encodes video after capture. This directly explains why a 1-second 4K clip reaches the backend at full camera bitrate/resolution.

**Photos**: current `imageQuality: 85` + `maxWidth/maxHeight: 1920` constraints are a reasonable starting point and already partially address the "don't store huge raw camera files" goal — validate actual resulting file sizes in practice before assuming this is sufficient; EXIF/GPS/orientation/capture-timestamp handling was not confirmed either way in this pass (whether `image_picker` strips or preserves EXIF orientation/GPS was not directly verified) — **open question**, worth confirming before finalizing a target, since GPS/timestamp EXIF data may itself be useful service-record metadata worth deliberately preserving (not stripping) rather than an oversight to fix.

**Video**: needs a real solution, since none exists today. A native (platform) H.264/AVC re-encode step is the standard approach — do not treat the brief's suggested 1080p/~30fps/controlled-bitrate numbers as final without validating them against actual sample footage from the devices GarageST staff use (the reported ~19MB/1-second S24 Ultra clip is likely a 4K/high-fps/high-bitrate capture profile; simply capping resolution and bitrate should reduce this by an order of magnitude, but the exact numbers should be tuned against real before/after inspection footage to confirm service-evidence quality is preserved, not assumed). A library such as `video_compress` or `ffmpeg_kit_flutter` would need to be added — this is a new dependency, which per this project's own conventions needs explicit approval before adding (flagged, not decided here).

**Evidence preservation**: since GarageST media serves as service/inspection evidence, any compression target must be validated against real sample photos/videos for legibility (e.g. can a technician's before/after damage photo still show the relevant detail at the chosen resolution/quality) before being locked in — this report deliberately does not prescribe final numbers.

---

## 22. Media Processing Strategy

**Where**: a **separate async worker** (option C from the brief), not on-device and not inline in Spring Boot request threads. Reasoning:
- **Not Flutter-only**: device-side compression (§21) handles the "don't upload huge originals" problem, but thumbnail generation and any server-side normalization (e.g. re-encoding an outlier oversized upload that slipped past client-side limits) should be centralized so every client/version produces consistent playback assets, not whatever that device's local encoder happened to produce.
- **Not inline in Spring Boot request threads**: synchronous transcoding inside an API request is explicitly called out as an anti-pattern (§31) — it would reintroduce the exact "large media blocking a web request thread" problem this whole redesign exists to remove.
- **Not a heavyweight dedicated microservice**: GarageST is early-stage; a full separate containerized service is more operational complexity than currently justified.

**Recommended approach**: reuse the pattern already proven in this codebase — `MediaUploadRetryScheduler`'s `@Scheduled` polling loop is the only existing async-job mechanism, and a `MediaProcessingScheduler` (or, if genuinely needed later, a Cloudflare Worker triggered by an R2 event) can follow the identical shape: poll for `status=UPLOADED` rows, generate thumbnail (and, if needed, a normalized playback asset) via a lightweight image/video library invoked from the same Spring Boot process (or a small separate worker process on Render if CPU/memory isolation from the API process becomes necessary), write results to R2, flip `status=READY`. This avoids introducing Cloudflare Queues, a new message broker, or a new deployable until there's evidence the polling-scheduler pattern (already proven at 15s intervals for Drive retries) doesn't scale for processing too.

---

## 23. Security Model

**Current state**: media upload/list/content endpoints are properly authenticated and tenant-checked today — `MediaServiceImpl.authorizeEmployeeAccess()` verifies garage-isolation and (for technicians specifically) an active `JobAssignment`, shared consistently across upload/list/content/visibility-update. This is a solid foundation to build the new architecture's authorization layer on top of — it does not need to be redesigned, only extended to also gate signed-URL issuance instead of gating a byte stream directly.

**For the target architecture**:
- Signed upload/playback URLs must be short-lived (minutes, not hours) and scoped to a single object key — never a bucket-wide credential handed to the client.
- MIME/extension validation should happen server-side at upload-authorization time (reject unexpected content-types before issuing a signed URL), not solely trusted from the client-declared `contentType`.
- Maximum media size/duration should be enforced both client-side (before upload starts) and server-side (reject an upload-authorization request exceeding policy, and ideally verify actual object size at `confirmUpload` time against what was declared).
- Content-type spoofing: validate the actual uploaded object's content-type/magic bytes at confirmation time where practical, not just trust the client's declared header.
- Rate limiting on upload-authorization issuance (prevent a compromised/malicious client from requesting unbounded signed URLs) — a lightweight in-process throttle (the same style already used for the public account-deletion-request endpoint in this codebase) is sufficient for now; no dedicated rate-limiting infrastructure exists or is needed yet.
- Malicious-file/antivirus scanning: **not required for MVP** — GarageST's media is internally-generated (staff/customer photos/videos of vehicles), not arbitrary public uploads; defer this unless the threat model changes (e.g. if customer-uploaded media from untrusted devices becomes a larger attack surface).
- Deletion authorization: piggyback on the same `authorizeEmployeeAccess()` pattern once a delete capability is built (§11 — currently doesn't exist at all).
- Audit/access logs: log who requested playback access and when (mediaId, jobCardId, garageId, userId, timestamp) — never log the signed URL/token itself.

**What to defer past MVP**: content-scanning/antivirus, a Worker gateway (until edge-cache economics justify it), fine-grained per-object ACLs beyond garage/job-card scoping, checksums beyond basic upload-integrity verification.

---

## 24. Failure & Recovery Matrix

| # | Scenario | Current behavior | Proposed behavior | Recovery strategy |
|---|---|---|---|---|
| 1 | Network lost after recording | Upload loop throws, remaining `_selectedMedia` items stay staged in memory only | Item stays `QUEUED`/`READY_TO_UPLOAD` in persistent local queue | Queue worker retries automatically on connectivity restore |
| 2 | User leaves screen during upload | In-flight `postMultipart` call is not explicitly cancelled/tracked; screen-scoped state lost on dispose | Upload is queue-worker-owned, not screen-owned | Queue continues independent of navigation |
| 3 | App killed during upload | Everything in `_selectedMedia`/in-flight request is lost | Item persisted to disk before any network call | Queue resumes on next launch from persisted state |
| 4 | Phone restarts | Same as #3 | Same as #3 | Same as #3 (WorkManager/URLSession also survive OS-level restart for truly background execution) |
| 5 | Upload succeeds, API response lost | Client doesn't know row was created; likely retries → duplicate row (no idempotency key today) | `uploadSessionId`/checksum lets backend recognize a retried confirm as the same logical upload | Backend dedups on `uploadSessionId` |
| 6 | DB row created, storage upload fails | Already handled well today — row saved `PENDING` before Drive attempt, retry scheduler drives completion | Same pattern, generalized to R2 | Existing retry-scheduler pattern (extend, don't replace) |
| 7 | Storage upload succeeds, DB update fails | Not applicable today (DB row created first) | With direct-to-R2 upload, this becomes possible (object exists, confirm-call fails) | Reconciliation job compares R2 objects to DB rows periodically; orphaned object without a `READY` row within a window gets cleaned up or retried |
| 8 | Same media uploaded twice | No dedup — two rows, two Drive uploads | Client generates a stable idempotency/checksum key per queue item | Backend rejects/merges duplicate `uploadSessionId` |
| 9 | Signed URL expires mid-upload | N/A (no signed URLs today) | Queue worker detects expiry (e.g. 403 from R2), requests a fresh signed URL from Spring Boot, resumes | Automatic re-authorization, transparent to user |
| 10 | R2 temporarily fails | N/A | Standard retry with backoff at the queue-worker level | Queue item returns to `READY_TO_UPLOAD`, retried per backoff schedule |
| 11 | Processing worker fails | N/A (no processing exists today) | Row stays `UPLOADED`/`PROCESSING_FAILED`; original is already durably stored and can be served as a fallback | Retry processing on schedule; media remains usable (without thumbnail) in the meantime |
| 12 | Thumbnail generation fails | N/A | Same as #11 — thumbnail absence doesn't block core media access | Retry; fall back to generic type icon in UI |
| 13 | User loses authorization mid-upload (e.g. deactivated) | Not specifically handled; likely a 401/403 on next request | Signed URL already issued remains valid for its short TTL (by design, unavoidable); subsequent authorization/listing calls correctly re-check current role/garage | Accept the short window as the acceptable exposure of any signed-URL model; keep TTL short |
| 14 | Job card deleted while media queued | Not directly evidenced; likely orphaned rows | Backend rejects upload-authorization for a non-existent/deleted job card at confirm time | Queue item surfaces a terminal failure to the user rather than silently retrying forever |
| 15 | Garage membership changes mid-upload | Not specifically handled today | Authorization is re-checked at each backend call (upload-authorization, confirm, playback-access) — a mid-flight membership change is caught at the next check, not just at initial pick | Fail the specific step cleanly; queue reports a terminal auth failure |
| 16 | User logs out with uploads pending | Not specifically handled; in-memory queue would simply vanish on logout/app restart today | Persistent queue survives logout (tied to device, not session) | Resume upload on next login using a fresh token, since the underlying job-card authorization is still what's checked, not the specific session |
| 17 | User changes garage (multi-garage staff) | Not specifically handled | Queue items are already tagged with their target `garageId`/`jobCardId` at enqueue time, independent of the user's "current" garage context | Uploads proceed against their original target regardless of the user's current UI context |
| 18 | R2 object exists, DB row missing | N/A today | Reconciliation job flags orphaned objects | Manual/automated cleanup after a grace period |
| 19 | DB row exists, R2 object missing | Roughly analogous to today's "Drive upload failed, row stuck PENDING" — already has a retry mechanism | Same retry pattern, generalized | Retry scheduler re-attempts upload from the still-available local optimized file (if still cached) or marks permanently failed after max retries |
| 20 | Media deleted while cached locally on device | N/A (no deletion feature exists at all today) | Local cache should honor a deletion signal (e.g. a 404/410 on next access) and evict | Cache invalidation on access-denied response |
| 21 | Multiple devices access same media | Works fine today (stateless GET); would work fine with signed URLs too, each device gets its own short-lived token | No change needed | N/A |
| 22 | 20+ photos uploaded simultaneously | Today: fully sequential, one at a time, awaited — slow but not broken | Queue worker can parallelize direct-to-R2 uploads (bytes don't burden Spring Boot) with a sensible concurrency cap | Concurrency limit tuned to device/network conditions |
| 23 | Multiple large videos uploaded | Today: each fully proxies through Spring Boot sequentially — exactly the scenario most likely to trigger the production crash | Each uploads directly to R2; Spring Boot only issues small authorization payloads | N/A — this is the core problem this redesign solves |
| 24 | Device has very little free storage | Not specifically handled; local optimization step could itself fail to write its output | Queue should surface a clear "insufficient storage" failure state rather than silently failing | Surface to user before attempting capture/optimization if feasible; otherwise fail the specific queue item with a clear reason |

---

## 25. Performance Model

*Conceptual — no production metrics were available to this analysis; figures below are formulas/ranges, not measured data.*

**Storage growth** (illustrative, using the brief's own example shape):
```
avg_optimized_video_size (target, post-compression) × videos_per_car_per_day × cars_per_day × days
e.g.: 3 MB × 4 videos/car × 20 cars/day × 30 days ≈ 7.2 GB/month (video only, post-optimization)
```
Compare to unoptimized: at the observed ~19MB/clip, the same volume would be ~45.6GB/month — roughly a 6x reduction from optimization alone, before any storage-tier considerations.

**Server bandwidth savings**: today, every playback view re-transits the full file through Spring Boot/Render (upload once, but every *view* also re-downloads from Drive through the server, since there's no cache). Moving playback to direct R2/CDN access removes essentially 100% of view-time bandwidth from Render's own network budget — the single largest infrastructure-cost lever in this redesign, independent of R2's own pricing.

**Memory usage**: today, each concurrent large-file download/upload holds a full buffered copy in JVM heap (§7) — this is a per-concurrent-request memory multiplier that disappears entirely once bytes no longer transit Spring Boot.

**Mobile CPU/battery**: video re-encoding is CPU-intensive; doing it on-device (§21) trades a one-time capture-time cost for a permanent reduction in upload time/data and server load — acceptable, but should be profiled on lower-end Android devices before finalizing target parameters, since GarageST's technician-facing users may not all have high-end hardware.

**Cache hit behavior**: CDN-level caching benefits most for media viewed by multiple parties (e.g. a customer-visible photo viewed by both the customer and staff) — internal-only media (technician-only inspection shots) will see lower cache-hit value but still benefit from not proxying through Spring Boot.

---

## 26. Cost Model

*Directional/conceptual comparison, not a quote. Pricing figures are marked as external research, not repository evidence.*

**Current**: Flutter → Spring Boot/Render → Google Drive.
- Storage: effectively "free" up to Google Drive's account quota (external/current research: standard consumer/workspace Drive tiers are quota-based, not pay-as-you-go per GB in the way object storage is — exact plan/quota not found in repository).
- Server bandwidth: **every playback view** consumes Render's outbound bandwidth (full file, every time, no cache) — this is the least visible but potentially fastest-growing cost driver as usage scales, since it's proportional to *views*, not just uploads.
- Compute: proxying/buffering large files consumes Render instance memory/CPU that would otherwise serve API requests — a shared-resource cost, not a separate line item, but a real one under load.

**Proposed**: Flutter → R2 → Cloudflare edge.
- Storage: R2 is priced per GB-month stored (external/current research — exact current rate not verified here, should be looked up at decision time).
- Egress/bandwidth: **R2's headline advantage is zero egress fees** (external/current research — this is Cloudflare's stated pricing model as of general public information, not confirmed against a live account in this review) — if accurate at decision time, this directly removes the "every view re-costs bandwidth" problem entirely, unlike a traditional S3-style egress-billed provider.
- Compute: Spring Boot's per-request cost drops to issuing small signed-URL payloads — negligible compared to today's buffering cost.
- CDN: Cloudflare's edge caching in front of R2 is typically bundled/low-incremental-cost when already using R2 (external/current research).
- Processing: a small additional compute cost for the async thumbnail/transcode worker — likely absorbable within existing Render capacity at GarageST's current scale, revisit if volume grows substantially.
- Operational complexity: net new — an R2 bucket, credentials, and (if added later) a Worker to operate — a real but modest increase over "just use a Drive account," proportionate to what a production media system requires.

**Bottom line**: the current architecture's costs are hidden inside Render's shared compute/bandwidth budget and Google's account-quota model; the proposed architecture makes storage and bandwidth explicit, likely cheaper at any meaningful scale (given R2's egress model), and shifts a small, deliberate amount of new operational surface onto GarageST in exchange.

---

## 27. Migration Strategy

**Guiding constraint**: GarageST has real production data already in Google Drive — this must be additive, not a rewrite, and must not require a synchronized "flag day" cutover of already-shipped Flutter clients.

Derived phase sequence (dependencies-driven, not blindly following the brief's suggested order):

1. **Commit the existing multi-select fix** (§10) — zero dependency on anything else in this plan, highest immediate user-facing value, already written.
2. **Backend storage abstraction** (§18) — introduce `MediaStorageService` interface, wrap existing Drive code behind `GoogleDriveMediaStorageService`, change no behavior yet. This must come before R2 integration so R2 is added as a second implementation, not a parallel bolt-on.
3. **Additive schema changes** (§17) — add `storage_provider` (default `GOOGLE_DRIVE`), `storage_key`, `checksum`, `upload_session_id`, `thumbnail_key`, `duration_seconds`. Zero impact on existing rows/clients.
4. **R2 integration (backend only)** — implement `R2MediaStorageService`, upload-authorization and playback-access endpoints, still gated behind a feature flag / not yet used by any shipping Flutter version.
5. **Flutter: direct upload to R2** — new app version uses the new upload-authorization flow for *new* uploads only; existing Drive-backed media continues to play back exactly as today (dual-path playback service: `if storageProvider == GOOGLE_DRIVE: legacy path; if R2: new path` — directly supported by the additive schema from phase 3).
6. **Flutter: persistent upload queue** — can be developed in parallel with phase 5 once the upload-authorization API shape is stable; this is independently valuable even before R2 ships (a queue that resumes a Drive-backed upload is still strictly better than today's screen-bound upload).
7. **Flutter: local media optimization** — compression before upload; independently valuable regardless of storage backend, reduces both Drive and R2 upload sizes.
8. **R2 playback via CDN/edge** — once new uploads are flowing to R2, confirm signed-URL playback end-to-end in production before broad rollout.
9. **Thumbnail/processing worker** — layer in once R2 upload+playback is stable; not a blocker for the core migration.
10. **Google Drive archive/backup decision** — once R2 is the default for new uploads, decide whether to (a) leave existing Drive media permanently in place (simplest, zero migration risk, playback dual-path stays forever for old rows) or (b) batch-migrate historical Drive objects to R2 over time (higher effort, only worth it if Drive's playback path becomes a maintenance burden or if consolidating onto one provider has a clear cost/ops win). **This report recommends (a) by default** — do not force a backfill migration merely for architectural tidiness; only revisit if Drive-side maintenance cost becomes a real burden.
11. **Remove old Drive live-playback assumptions from new-client code paths** only after confirming no meaningful volume of Drive-backed media remains actively viewed — likely never fully removed given decision in step 10, and that's an acceptable permanent dual-path state, not technical debt requiring closure.

For each phase: backend/Flutter/DB/infra changes are scoped as described per-phase above; risk is lowest at the start (phases 1–3 touch no live behavior for existing users) and highest at phases 5/8 (first real client-facing behavior change), where a feature flag and staged rollout (e.g. behind a specific app version or remote-config gate) is the recommended rollback strategy — disable the flag, fall back to the existing Drive upload path, no data loss since the old path is untouched.

---

## 28. Implementation Phases

| Phase | Backend | Flutter | DB | Infra | Risk | Rollback | Testing |
|---|---|---|---|---|---|---|---|
| 0. Commit multi-select fix | — | Review & commit existing working-tree change | — | — | Low | `git revert` | Manual multi-photo upload test |
| 1. Storage abstraction | New `MediaStorageService` interface, `GoogleDriveMediaStorageService` wraps existing code, no behavior change | — | — | — | Low | N/A (pure refactor, same behavior) | Existing upload/download tests must still pass unchanged |
| 2. Additive schema | — | — | New nullable/defaulted columns (§17) | — | Low | Drop columns if needed (no data depends on them yet) | Migration applies cleanly against a copy of prod schema |
| 3. R2 backend integration | `R2MediaStorageService`, upload-authorization + playback-access endpoints, feature-flagged | — | — | Provision R2 bucket + credentials | Medium | Feature flag off | Integration tests against a real (dev) R2 bucket |
| 4. Flutter direct upload | Confirm-upload endpoint stabilized | New upload path using signed URLs, dual-path playback service | — | — | Medium | App-side flag/remote-config to force legacy path | End-to-end upload+playback test on a real device |
| 5. Persistent local queue | — | Local disk-backed queue, state machine (§16) | — | — | Medium | Feature can ship independent of R2; regress to synchronous upload if needed | Kill-app-mid-upload manual test; airplane-mode test |
| 6. Local media optimization | — | Compression before upload/queue enqueue | — | Possible new dependency (compression lib) — needs explicit approval per project convention | Medium | Ship uncompressed if optimization step fails, don't block upload | Before/after quality comparison on real sample media |
| 7. Playback via CDN/edge | Playback-access endpoint returns R2/edge URL | Fetch from returned URL, add disk cache | — | Optional: Worker gateway | Medium | Fall back to presigned-URL-only (no Worker) | Load test signed-URL expiry/refresh path |
| 8. Processing worker | New scheduler (mirrors `MediaUploadRetryScheduler`) | Consume `thumbnail_key` once populated | Uses `thumbnail_key`/duration columns from phase 2 | — | Low | Serve without thumbnail on failure (already designed for) | Failure-injection test on transcode step |
| 9. Background execution (Android/iOS) | — | `WorkManager` (Android) / `URLSession` background tasks (iOS) integration | — | — | High (most platform-divergent, most native-code risk) | Ship queue without true OS-level background survival first; add this as a later hardening pass | Device-level test: force-kill app mid-upload, verify resume |
| 10. Drive archive decision | Document decision; no forced migration | — | — | — | Low | N/A | N/A |

---

## 29. Testing Strategy

- **Unit**: `MediaStorageService` implementations (mock R2/Drive SDK calls), media state-machine transitions, checksum/idempotency logic.
- **Integration**: upload-authorization → direct upload → confirm-upload round trip against a real dev R2 bucket; dual-path playback service resolving both `GOOGLE_DRIVE` and `R2` rows correctly.
- **API**: authorization boundary tests (garage isolation, job-card ownership, technician-assignment check) reused/extended for the new upload-authorization and playback-access endpoints — this repo already has this pattern for the existing media endpoints, extend rather than reinvent.
- **Flutter**: queue enqueue/dequeue/persistence unit tests (pure Dart, no device needed); widget tests confirming `MediaGallery` still renders N items per stage (regression guard for §10's bug class).
- **Device**: real multi-photo capture-and-upload on both a low-end and high-end Android device; iOS device test once background-upload capability is built.
- **Network interruption**: airplane-mode-mid-upload, verify queue item survives and resumes on reconnect.
- **Background upload**: force-kill the app mid-upload (Android and iOS separately, since mechanisms differ), verify resume behavior matches phase 9's chosen implementation.
- **Storage failure**: simulate R2 5xx/timeout during upload and during playback-URL generation; verify retry/backoff and clear failure surfacing.
- **Authorization**: verify a signed upload/playback URL cannot be used by a different garage's user, and that an expired signed URL is correctly rejected and refreshed.
- **Concurrent upload**: 20+ simultaneous queue items, verify the concurrency cap and that no items are silently dropped.
- **Large media**: a multi-hundred-MB video (before optimization is applied, to test the ceiling case) through the full pipeline, confirming Spring Boot never buffers it.
- **Multiple media**: explicit regression test for the exact bug in §10 — select 3+ photos for one stage, confirm all 3 appear in the gallery afterward. This should become a permanent automated test given it was a real, shipped bug.

---

## 30. Risks & Tradeoffs

- **Native background-upload work (Android `WorkManager` / iOS `URLSession` background tasks) is genuinely the highest-effort, most platform-divergent piece of this entire plan** — budget for it separately and don't let it block shipping the rest (queue + R2 + optimization all deliver value without true OS-kill-survival).
- **Dual-provider playback (Drive + R2, indefinitely)** is a permanent, not temporary, complexity cost if the "don't force-migrate old Drive media" recommendation (§27) is adopted — accepted here as the right tradeoff against migration risk, but it does mean the codebase carries two playback code paths forever.
- **R2 pricing/egress claims in this report are external, unverified-at-decision-time research**, not confirmed against a live GarageST Cloudflare account — re-verify actual current pricing before committing budget.
- **Local media optimization adds a new third-party dependency** (compression/transcode library) — per this project's own conventions this needs explicit approval, and could itself introduce its own bugs/platform quirks (video encoding libraries are a common source of device-specific crashes) — should be piloted carefully, not rolled out broadly on day one.
- **A Worker gateway, if added later, is a new operational surface** (another thing that can fail, needs monitoring, needs its own testing) — deliberately deferred (§19) until justified by actual cache-economics evidence, not built speculatively.

---

## 31. What We Should NOT Do

- Increase Render RAM/timeout limits as the primary fix for the streaming crash — treats a symptom, not the cause; the crash will recur at a larger file size.
- Continue passing large media bytes through Spring Boot indefinitely, even after adding R2 — the whole point of R2 is to route bytes around the JVM, not just relocate where they're eventually stored.
- Store raw, unoptimized camera videos by default — directly causes both the playback crash and unnecessary storage/bandwidth cost.
- Make private media publicly accessible (e.g. a public R2 bucket, or permanent unsigned URLs) merely to simplify playback — defeats the tenant-isolation model already correctly implemented in `MediaServiceImpl.authorizeEmployeeAccess()`.
- Tie uploads to a specific widget/screen's lifecycle — the root cause of "upload doesn't survive navigation" today; the persistent queue must be screen-independent by construction.
- Rely only on an in-memory cache for playback — provides no benefit across app restarts or between different screens revisiting the same media.
- Perform synchronous video transcoding inside an API request thread — reintroduces the exact large-payload-blocking-a-request-thread problem this redesign removes; processing must be async.
- Attempt a big-bang migration off Google Drive — GarageST has real production data in Drive today; any migration must be additive and dual-path-capable, not a forced cutover.

---

## 32. Recommended Final Architecture

```
Flutter (capture) → local optimizer → persistent disk-backed queue
        │
        ▼
Spring Boot (auth + authorization + metadata + signed URL issuance)
        │
        ▼
Cloudflare R2 (canonical storage, private)
        │
        ▼
Async processing worker (thumbnail/playback-asset generation)
        │
        ▼
Signed/short-lived access → Cloudflare edge → Flutter (+ local disk cache)

Google Drive: retained, unchanged, as the permanent home for pre-migration media
              (dual-path playback service, no forced backfill)
```

---

## 33. Decision Log

| Decision | Reasoning |
|---|---|
| Keep Google Drive for existing media, don't force-migrate | Real production data already there; migration risk outweighs the benefit of a single provider; dual-path playback is a small, permanent, acceptable complexity cost |
| R2 over continuing with Drive for new uploads | Drive has no clean signed-URL/streaming model for this use case (confirmed: no Range support, no clean direct-client-access pattern) and was never designed as an app's primary media backend |
| Presigned URLs before a Worker gateway | Fully solves "bytes don't transit Spring Boot" and "media isn't public" with zero new infrastructure; Worker is a deferred optimization, not a day-one requirement |
| WorkManager/URLSession over a bespoke background service | These are each platform's own recommended durable-background-task primitive, with retry/backoff already built in — matches this project's stated preference for not hand-rolling infrastructure that a platform API already provides |
| Reuse `@Scheduled`-polling pattern for processing, not a new queue/broker | Directly mirrors the already-proven `MediaUploadRetryScheduler` pattern; avoids introducing new infrastructure before there's evidence the simpler pattern doesn't scale |
| Generalize the navigation module's `storageKey` pattern rather than inventing a new abstraction | The codebase already solved "provider-neutral media storage" once, correctly, in a sibling module — reuse, don't reinvent |
| Additive-only schema changes | Zero risk to existing rows/clients; a hard requirement given GarageST already has real users |

---

## 34. Open Questions

- What are the actual current Google Drive account limits/quota, and how close is GarageST to them? (Not found in either repository — external account detail.)
- What is GarageST's actual current/expected media volume (uploads/day, average file size) — needed to validate the storage-growth and cost-model estimates in §25/§26 with real numbers rather than illustrative ones.
- Does `image_picker`'s current `imageQuality`/`maxWidth`/`maxHeight` usage strip or preserve EXIF GPS/orientation/capture-timestamp data? Not confirmed in this pass — matters for the "preserve service evidence" requirement.
- What are GarageST's actual target video-quality requirements from real sample inspection footage — the brief's suggested 1080p/30fps/controlled-bitrate should be validated against real before/after service photos/videos, not assumed.
- Is there a business reason media deletion has never been implemented (deliberate, e.g. "service records must never be deletable"), or is it simply not built yet? This materially affects whether a deletion capability should be added as part of this redesign or deliberately left out.
- Current/actual Cloudflare R2 pricing and egress terms at decision time (this report's cost figures are marked explicitly as external, unverified research).
- Should customer-portal-visible media eventually get its own, possibly more permissive, caching/access policy than internal-only technician media? Not decided here — flagged as a product question, not an engineering one.

---

## 35. Exact Next Steps

1. Review and commit the already-drafted Flutter multi-select fix in `job_step_screen.dart` (§10) — smallest possible task, real bug fix, ships independently of everything else in this report.
2. Introduce the `MediaStorageService` interface in the backend, with `GoogleDriveMediaStorageService` as its first (behavior-unchanged) implementation, generalizing the pattern already proven in the navigation module.
3. Add the additive `job_card_media` schema columns (§17) via a new Flyway migration — no behavior change, sets up everything downstream.
4. Provision a Cloudflare R2 bucket (dev/staging first) and confirm presigned PUT/GET URL generation works end-to-end in isolation, before wiring it into GarageST's upload/playback endpoints.
5. Build the Flutter persistent local upload queue's data model (pure Dart, no native work yet) — independently valuable even before R2 ships, and de-risks the hardest remaining pieces (native background execution) by not coupling them to the queue's initial delivery.

---

# Architecture Recommendation

1. **Should Google Drive remain the live media delivery mechanism?** No — it has no Range/streaming support (self-documented in the codebase) and is the direct cause of the production crash on larger files. It should remain only as the storage location for *already-uploaded* media, not as the target for new uploads or the live playback path going forward.

2. **Should Spring Boot continue streaming media bytes?** No — it should be reduced to issuing short-lived signed URLs/tokens (for both upload and playback) and stop being the pipe media bytes physically flow through.

3. **Should media be optimized on-device?** Yes, for both photos (partially already happening via `image_picker`'s quality/size parameters) and video (not happening at all today, and the direct cause of the ~19MB/clip figure). Validate target parameters against real sample footage before finalizing.

4. **Should GarageST introduce a persistent upload queue?** Yes — this is independently valuable regardless of the storage-provider decision, and directly fixes the "upload doesn't survive navigation/backgrounding" gap that exists today with zero queue at all.

5. **Should media be stored in object storage?** Yes, for new uploads going forward — object storage (vs. Drive) is what actually supports signed URLs, Range requests, and a CDN-friendly access model.

6. **Is Cloudflare R2 suitable?** Yes, on the evidence available — S3-compatible presigned URLs, private-by-default buckets, and (per external/unverified-at-decision-time research) a zero-egress pricing model that directly addresses today's largest hidden cost (every playback view re-costing server bandwidth). Re-verify current pricing before committing.

7. **Is a CDN/edge layer appropriate?** Eventually, yes — but defer a Worker gateway specifically until there's evidence of real cache-economics need; plain presigned URLs already solve the core architectural problem without it.

8. **Should private media use signed/controlled access?** Yes, unconditionally — GarageST's media is tenant-isolated business/personal data (per the recently-published Privacy Policy's own third-party-sharing disclosures), and the existing backend authorization model (garage isolation, job-card ownership, technician-assignment checks) is already well-built and should gate signed-URL issuance rather than being discarded.

9. **Should media processing be asynchronous?** Yes — synchronous transcoding inside an API request thread is the same class of problem as today's synchronous full-file proxying, just moved to a different stage.

10. **How should multiple media items be modeled?** As they already correctly are on both sides once the one Flutter bug (§10) is fixed — a flat, one-row-per-item model (`List<JobCardMedia>`), grouped by stage only for display purposes, never collapsed to a single item per stage anywhere in the data model.

11. **What should happen to existing Google Drive media?** Left in place, played back via a permanent dual-path playback service (`storageProvider` branch) — no forced backfill migration, given the migration-risk-vs-benefit tradeoff for a product with real existing users.

12. **What should we implement FIRST?** Commit the already-written multi-select bug fix (zero-risk, real user impact, done today) and the backend `MediaStorageService` abstraction (zero behavior change, unblocks everything else) — in that order, before any R2/infrastructure work begins.

13. **What should we deliberately NOT implement yet?** A Cloudflare Worker gateway, content/antivirus scanning, a full historical Drive-to-R2 backfill migration, and true OS-kill-surviving native background upload (WorkManager/URLSession) — each is either premature (no evidence of need yet) or high-effort-relative-to-current-value; ship the queue, optimization, and R2 direct-upload/playback first, and revisit these once real usage data justifies them.
