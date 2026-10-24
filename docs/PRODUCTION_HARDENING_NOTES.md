# Production hardening notes (backend)

Technical notes for the hardening, Emergency Assist, branding, Super Admin and bank-account work. Written for the
engineer who deploys or extends it. Everything below was verified against the code; items that could not be verified in
this environment are marked **NOT VERIFIED**.

## 1. Database migrations (all additive, none edits an applied migration)

| Migration | Adds | Compatibility |
|---|---|---|
| `V84__rate_limit_bucket.sql` | `rate_limit_bucket` (hashed key, window, count) | New table. Self-pruning (scheduled). |
| `V85__emergency_assist.sql` | `emergency_assist_request`; partial unique index = one active request per mobile; unique `(contact_mobile, idempotency_key)` | New table. FKs to `garage`, `customer`. |
| `V86__garage_brand_colors.sql` | 4 nullable columns on `garage` + `#RRGGBB` CHECK constraints | Existing garages unchanged (NULL = default theme). |
| `V87__garage_bank_account.sql` | `garage_bank_account`; partial unique index = one current account per garage | New table. Account number stored only encrypted. |
| `V88__user_email_verification.sql` | `users.email_verified_at`, `users.email_verified_address` | Nullable. Existing users are simply unverified. |

Rollback: each migration only creates objects or adds nullable columns, so rolling the application back leaves them
unused; to remove them, drop the new tables / columns (no data in existing tables depends on them).

No data migration, backfill or renaming of stored objects is performed anywhere.

## 2. New configuration

| Property / env | Default | Purpose |
|---|---|---|
| `GARAGEST_FIELD_ENCRYPTION_KEY` (`garagest.field-encryption.key`) | empty | Base64 of 32 random bytes. **Required before bank-account registration works.** With no key registration is refused (`BANK_NOT_CONFIGURED`); there is never a plaintext fallback. Generate with e.g. `openssl rand -base64 32`. Keep it in the secret store; losing it makes stored account numbers unreadable. |
| `SWAGGER_ENABLED` | `false` | Swagger UI / OpenAPI are off and not whitelisted unless `true` (local or authorised staging only). |
| `JPA_SHOW_SQL` | `false` | SQL logging (was `true`). |
| `GOOGLE_DRIVE_OAUTH_OPERATOR_TOKEN` | empty | `/api/v1/media/google/authorize?token=...` works only with this token; empty disables the flow. The callback additionally requires a signed, 10-minute `state`. |
| `GARAGEST_RATE_LIMIT_ENABLED` | `true` | Master switch for the shared limiter. |
| `garagest.rate-limit.policies.<name>.limit` / `.window-seconds` | see `RateLimitProperties` | Per-policy thresholds. |
| `emergency.coverage-radius-km` | `40` | Distance within which a garage is considered able to reach a location. |
| `emergency.request-ttl-minutes` | `30` | Unanswered requests expire after this. |
| `emergency.accepted-auto-close-hours` | `24` | Accepted but never completed requests close after this (frees the number). |
| `garagest.gstin.enforce-checksum` | `true` | GSTIN structural validation on garage create/update. Emergency escape hatch only. |
| `GARAGEST_CORS_ALLOWED_ORIGINS` | empty | Unchanged: empty means no cross-origin access; wildcards are never honoured. `Retry-After` is now exposed. |

## 3. Behaviour changes worth knowing

* **Login** resolves username, email (case-insensitive) or mobile (spaces, dashes, `+91`/`91`/`0` prefixes) in
  `LoginIdentifierResolver`. An identifier that matches more than one account is rejected like an unknown one (logged
  with counts only). Failures now return **401 "Invalid username or password."** (previously the generic catch-all
  returned 500 for bad credentials). Disabled/locked accounts return the same 401.
* **Rate limiting** is PostgreSQL-backed (no Redis in the project), atomic per key, committed in its own transaction,
  stores only SHA-256 digests, fails open if the counter store is down, and prunes expired rows. Applied to: login (IP
  and per-identifier), registration, OTP send, place search, nearby discovery, booking creation, media uploads and all
  public Emergency Assist routes. The existing OTP cooldown/hourly/attempt caps and the password-recovery throttle are
  unchanged. Trade-off: one small write per limited request; swap the implementation behind `RateLimitService.require`
  if traffic outgrows it. The password-recovery throttle remains in-process memory (pre-existing).
* **Estimates**: create/update/delete need `OWNER`/`MANAGER`/`SERVICE_ADVISOR` (controller and service); edits lock the
  row first; an `APPROVED` or invoiced estimate cannot be updated or deleted; an estimate cannot be moved to another job
  card. Line add/update/delete use the same role set. **Not implemented:** a reopen/revision workflow for an approved
  estimate (see §6).
* **Audit**: `GET /api/v1/audit/events` is staff-only and garage-scoped; platform-only and banking events are never
  returned to garages; actions by a platform admin on a garage's own records are shown without actor or reason. Platform
  admins read everything through `GET /api/v1/platform/audit` (that read is itself audited).
* **Platform status change** needs a reason **and the admin's own password** (step-up; there is no second factor for
  staff). Wrong password: refused, rate limited (5/15 min) and audited. This is the strongest re-authentication the
  current identity model supports.
* **Inactive garage**: no new customer bookings, Emergency Assist, staff-created bookings or new job cards; existing
  work continues; staff can still sign in; history stays readable; My Garages reports `unavailableReason`.
* **Reports**: the customer report holds only `CUSTOMER_VISIBLE` photos (query-level, server side). The owner report
  holds all photos and marks internal ones "INTERNAL - not shown to the customer". `DURING_REPAIR` still defaults to
  `INTERNAL` deliberately (see §6).
* **Customers**: only `OWNER`/`MANAGER`/`SERVICE_ADVISOR` create or edit customers (`OWNER`/`MANAGER` delete). Reusing
  another garage's customer by mobile returns only what the caller typed, not that garage's stored details.
* **GSTIN**: normalised and validated (shape, state code, check character, PAN match) on garage create and on a
  *changed* value at update. "Format valid" is **not** "verified with the GST portal"; the admin view says so.
* **RC verification**: `VERIFIED` requires a document reference; every change is audited; moving back to
  pending/not-submitted clears the earlier reviewer. This records a staff decision; there is no OCR or registry lookup.

## 4. Object-storage key convention (R2 / local fallback)

* Job-card media (unchanged): `GarageST/{garageCode}/{jobCardNumber}/{STAGE}/{generated file}` where `STAGE` is
  `BEFORE_SERVICE`, `DURING_REPAIR`, `AFTER_SERVICE`, etc. Derived objects (`_evidence.jpg`, thumbnail) sit beside it.
* Pickup/delivery trip evidence (new for new uploads): `trip-evidence/garage-{garageId}/navigation/{tripId}/{stage}/{uuid}.ext`.
  Objects written earlier keep their stored key (`trip-evidence/navigation/{tripId}/...`); keys are always read from the
  database row, never rebuilt, so nothing is renamed or lost. If the garage cannot be resolved the old layout is used.
* The folder name is organisation only. Access is decided by the database row and the authenticated caller; no key or
  URL is publicly readable. A failed R2 write when R2 is configured fails the upload (nothing recorded as complete); the
  local-disk fallback applies only when R2 is not configured at all, and is logged as not durable.
* Retry safety: `clientUploadId` (trip media) and the upload-intent sequence (job-card media) make retries return the
  existing row instead of creating a duplicate.

## 5. API summary (new)

* `POST /api/v1/emergency/verify/send|confirm`, `POST /emergency/garages/search`, `POST /emergency/requests`
  (`Idempotency-Key` header), `GET /emergency/requests/{code}` and `POST .../cancel` (`X-Emergency-Token` header) are
  public; `GET /emergency/garage/requests`, `POST /emergency/garage/requests/{id}/accept|reject|complete` are
  `OWNER/MANAGER/SERVICE_ADVISOR` and garage-scoped.
* `PUT /api/v1/garages/{id}/branding` (`OWNER`): `{primaryColor, secondaryColor}` as `#RRGGBB` or null.
* `GET|PUT /api/v1/garages/{id}/bank-account`, `POST .../retire` (`OWNER`, password step-up, masked responses).
* `GET /api/v1/platform/audit`, `GET /api/v1/platform/garages/{id}/bank-account` (`SUPER_ADMIN`, masked, audited).
* `POST /api/v1/account/email-verification/send|verify` (authenticated; address always read from the account).

## 6. Decisions and things deliberately not done

| Topic | Decision |
|---|---|
| Estimate revision after approval | Not implemented. A safe reopen needs the job-card state machine (`REPAIR_PENDING` cannot return to `WAITING_FOR_APPROVAL`) and repair-task creation to become reversible. Needs a product decision (reopen vs. separate additional-work estimate). Approved estimates are protected from in-place change instead. |
| Invoice amendment / credit & debit notes | Not implemented. `invoice.estimate_id` is `UNIQUE`, so cancel-and-reissue needs a schema redesign, and GST credit/debit-note treatment needs qualified tax advice. Existing safeguards (no edit route, cancel restricted to Manager/Owner and refused after payment) are covered by a regression test. |
| `DURING_REPAIR` photos | Remain internal by default; the garage chooses what a customer may see. Not made customer-visible to "fix" reports. |
| Emergency without a default garage + `findAnotherGarage` | Emergency Assist has no alternative-garage flag at all. Normal bookings keep the existing, explicit "find another garage" action (ACTIVE garages only). |
| Emergency place search before login | `/places/**` stays authenticated (provider quota). Anonymous users use GPS or drag a pin; address search needs an account. |
| Emergency notifications to the customer | The customer follows status in the app (token-based). There is no SMS/push to an anonymous caller (no provider integrated). |
| Bank verification / payouts | Registration, encryption, masking, audit and a provider interface only. No provider is integrated: every account stays `PENDING_VERIFICATION`. Payouts are not implemented. |
| Legacy static pages | No longer deployed (2026-10-24). `pom.xml` excludes `static/**` except `privacy-policy.html`, `delete-account.html`, `delete-account-requested.html` from the packaged resources; the source files stay in the repository. `SecurityConfig` no longer whitelists the legacy paths and a missing resource is a 404 (`GlobalExceptionHandler`). `/` answers 200 `GarageST API` for health checks. Verified in `ExposedEndpointsIT` and against the packaged jar. A separately hosted copy (for example a Render static site) is not covered by this change. |
| Web domain | `EnvConfig` still holds `GarageST.example.com` placeholders; the production domain is not stated anywhere in the repositories, so it is not guessed. |
| GSTIN uniqueness | Not enforced (a business may legitimately register several garages under one GSTIN). Needs a business rule. |

## 7. Verification status

See the final implementation report for command-level results. Anything involving real GPS, camera, FCM, R2, SMS/e-mail
delivery or a real bank provider is **NOT VERIFIED** here and is covered by the physical-device checklist.
