# CLAUDE.md — GarageST Backend

This file guides Claude Code (and any other agent) working in this repository.
Read it before making changes. When in doubt, prefer asking over acting.

## 1. Purpose and Architecture

GarageST backend is a Spring Boot 4.0.7 / Java 17 modular monolith that serves
as the single API for **two application clients**:

1. A legacy static multi-page JS frontend (`src/main/resources/static/`) — auth,
   garage onboarding, owner portal, employee portal, customer portal. **Still
   live and still served by this app.**
2. The new **GarageST Flutter mobile app** (separate repo, `garagest_flutter`),
   which is a systematic, in-progress port of the same JS frontend — same API
   contract, same backend behavior, same roles.

Swagger/OpenAPI (springdoc-openapi) is documentation, testing, and integration
**tooling** — not a third application client. It does not carry independent
behavioral requirements; it describes the contract the two real clients above
depend on.

This backend is not a greenfield API — it is the **source of truth** that both
frontends were/are built against. Its current behavior, however inconsistent
it may look, is a contract two live clients already depend on.

## 2. Modular Monolith Conventions

- One Spring Boot application, package-by-feature under
  `com.garageos.modules.<feature>`, plus cross-cutting concerns under
  `com.garageos.core`.
- Each module is internally layered the same way (see §3). Preserve this
  layering for any new module or endpoint — don't introduce a different
  pattern (e.g. a fat controller with inline business logic) even locally.
- Modules are allowed to depend on each other's `service`/`entity`/`repository`
  layers (e.g. `serviceworkflow` orchestrates across `jobcard`, `estimate`,
  `repairtask`, etc.) — this is the existing style, not a violation to "fix"
  into stricter module boundaries.

## 3. Package / Module Structure

Every module follows:
```
modules/<feature>/
  controller/   — @RestController, thin, delegates to service
  dto/request/  — inbound request bodies (Bean Validation annotations)
  dto/response/ — outbound response bodies
  entity/       — JPA entities (extend core.audit.BaseEntity)
  event/        — domain events, where used
  mapper/       — MapStruct entity <-> DTO mappers
  repository/   — Spring Data JPA repositories
  service/      — interface
  service/impl/ — implementation
  validator/    — module-specific validation, where used
```
New code in an existing module must follow this layout. A new module must
create the same structure, not a subset or a variant.

## 4. Spring Boot / Java Conventions

- Java 17 language level. Spring Boot 4.0.7 starter naming (`spring-boot-starter-webmvc`,
  not `-web`; `spring-boot-starter-flyway`, not `-data-flyway`) — match this
  project's actual starter names, not older Boot 3 tutorials/examples.
- Lombok for boilerplate (`@Getter`/`@Setter`/`@RequiredArgsConstructor`/
  `@Builder`), `@FieldDefaults(level = AccessLevel.PRIVATE)` is used on some
  entities/DTOs — match the surrounding file's existing style rather than
  imposing a different Lombok convention.
- Controllers stay thin: `@Valid @RequestBody` in, delegate to one service
  method, wrap the result via `ApiResponseUtil`. Business logic belongs in
  `service/impl`, not the controller.
- Constructor injection via `@RequiredArgsConstructor` + `final` fields is the
  established DI style — do not switch to field injection (`@Autowired` on
  fields) even in new code.

## 5. JPA / Hibernate Conventions

- Every entity extends `com.garageos.core.audit.BaseEntity` (`id`, `createdAt`,
  `updatedAt` via `@CreatedDate`/`@LastModifiedDate`). Do not hand-roll audit
  columns on a new entity.
- `spring.jpa.hibernate.ddl-auto=validate` is load-bearing: Hibernate must
  never be allowed to auto-generate or alter schema. Every schema change goes
  through a new Flyway migration (§6), never through entity annotations alone.
- Favor `@ManyToOne(fetch = FetchType.LAZY)` for relations, as the existing
  entities do — don't switch existing relations to EAGER without a specific,
  approved reason (N+1 fixes should be done via fetch joins/`@EntityGraph` at
  the query site, not by changing the mapping's fetch type globally).
- When adding a query that must be tenant/ownership-scoped, follow the
  existing repository pattern of a derived query method scoped by
  `garageId`/`customer` (see `UserRepository.findByGarageIdAndRoleAndStatus`,
  `JobCardRepository.findByCustomer`) rather than filtering in memory after
  `findAll()`. See §10 — this is mandatory for new code, not just a style
  preference.

## 6. Flyway Migration Rules

- Migrations live in `src/main/resources/db/migration/`, named
  `V<N>__description.sql`, strictly sequential from the current highest `V`.
- **Never edit an already-applied migration file.** If a past migration was
  wrong or is now understood to be a mistake, the fix is a **new** migration
  that corrects it — never a retroactive edit, even for a typo.
- Do not consolidate, rename, renumber, or delete existing migrations, even
  ones that look duplicated (e.g. `V27`/`V29` are both titled
  `create_navigation_tables.sql` — investigate before touching, do not merge
  them without approval; see §17).
- Do not modify migration files, database data, or apply/rollback migrations
  as a side effect of an unrelated task.

## 7. REST API Contract Rules

- Base path is `/api/v1/...`. New endpoints should live under the correct
  existing module's base path.
- **Do not "fix" a path to be consistent with a sibling module** (e.g. do not
  rename `/api/v1/jobcards` to `/api/v1/job-cards`, or vice versa, to match
  another controller) without explicit approval — a live client may depend on
  the exact current path. See §17 for the general rule on overlapping APIs.
- Response envelope: most endpoints return `ApiResponse<T>` via
  `ApiResponseUtil.success/created(...)`. A minority of endpoints
  (`NavigationTripController`, several `JobCardController` transition
  endpoints) return the bare DTO instead. **Match whichever convention the
  controller you're editing already uses** — do not unilaterally add or
  remove the envelope on existing endpoints; that is a breaking response-shape
  change for any client already parsing it.
- New endpoints should default to the `ApiResponse<T>` envelope unless there's
  a specific reason to match an un-enveloped sibling group.
- Error responses currently have two shapes (`ApiError` for validation/
  not-found/business exceptions, `ApiResponse<Void>` for
  already-exists/generic exceptions) — do not unify these as a drive-by change.

## 8. DTO / MapStruct Conventions

- Every module keeps `dto/request` and `dto/response` separate; entities are
  never returned directly from a controller.
- MapStruct mappers (interfaces in `mapper/`) do entity↔DTO conversion.
  Annotation processor chain is `lombok` → `lombok-mapstruct-binding` →
  `mapstruct-processor`, in that order (see `pom.xml`) — don't reorder if you
  touch the compiler-plugin config.
- Request DTOs use `jakarta.validation` annotations (`@NotNull`, etc.) for
  input validation; keep validation on the DTO, not re-implemented in the
  service layer, unless the check is cross-field/business-rule level (those
  belong in the service as a `BusinessException`).

## 9. Authentication and Authorization Rules

- Auth is stateless JWT (access + refresh), issued by `AuthController`,
  validated per-request by `JwtAuthenticationFilter`. Roles come from
  `RoleCode` (the **live** enum — `com.garageos.core.enums.identity.RoleCode`).
  Do not use `com.garageos.core.enums.UserRole`, which is dead/unused; do not
  reintroduce references to it.
- **Current reality**: almost no endpoint enforces role via `@PreAuthorize` —
  only `MediaController.uploadMedia` does. This is a known, previously
  identified gap, not something to silently "complete" by adding
  `@PreAuthorize` broadly across controllers. **Do not add or change
  authorization checks on existing endpoints without explicit approval** —
  some current client behavior may implicitly depend on the current lack of
  server-side role enforcement, and a blanket RBAC pass could break a live
  client in ways that aren't visible from the backend alone.
- If asked to add authorization to a **specific, named** endpoint, follow the
  existing working pattern: `@PreAuthorize("hasAnyRole('MANAGER', ...)")`
  relies on `ROLE_<RoleCode>` granted authorities already set up in
  `GarageUserDetailsService` — reuse that, don't invent a new mechanism.
- The fine-grained `Permission`/`RolePermission` system exists in the schema
  and entities but is intentionally not wired into authorities (commented out
  in `GarageUserDetailsService`). Do not "finish" this wiring speculatively —
  treat it as a deliberate future project, not an oversight to patch.
- `/auth/refresh` **exists and works** — do not treat it as missing, and do
  not duplicate it.
- See §14 for the security-specific rules that apply on top of this section
  (never weaken existing auth checks, always report gaps found in passing).

## 10. Multi-Tenant / Garage-Scoping Rules

- Most data (customers, vehicles, job cards, estimates, etc.) is scoped to a
  `garage_id`. Any new query that lists or searches such data **must** be
  scoped to the caller's garage (via `@AuthenticationPrincipal
  GarageUserPrincipal user` → `user.getGarageId()`), following the pattern in
  `UserController.drivers()` / `UserServiceImpl.getDrivers(garageId)`.
- **All new garage-scoped functionality must enforce tenant scope. This is
  mandatory, not best-effort.** Never introduce a new query, endpoint, or
  service method that reads or writes garage-scoped data (customers, vehicles,
  job cards, estimates, invoices, employees/users, media, etc.) without a
  `garage_id` (or equivalent ownership) filter applied at the query level. A
  `findAll()` or unscoped `findBy...` call over tenant-owned data is not
  acceptable in new code under any circumstances.
- Known, already-identified scoping gaps exist in **existing** code (e.g.
  `UserServiceImpl.getTechnicians()`/`getRepairEmployees()` currently query
  across all garages; `CustomerPortalServiceImpl.getEstimateDetails()` doesn't
  verify the estimate belongs to the calling customer). **Do not silently fix
  these as a side effect of unrelated work.** If you are asked to work on
  either of these methods, or if a task requires touching them, flag the
  scoping gap explicitly and get confirmation before changing the query —
  fixing it is a behavior change with real product implications (e.g. a
  cross-garage technician list might currently be relied upon by some flow),
  not a pure bug fix to apply on sight. This "don't silently fix" rule applies
  only to **existing** unscoped queries — it never excuses writing a **new**
  unscoped one; see the bullet above.

## 11. JobCard Workflow Rules

- There are **two parallel controllers** driving job-card status transitions:
  `ServiceWorkflowController` (`/api/v1/workflow/...`) and `JobCardController`
  (`/api/v1/jobcards/...`, several transition sub-paths). Both are currently
  live. **Do not merge, deprecate, or redirect one into the other** without
  explicit approval — determine which the currently-active client call sites
  use before changing either.
- Some transitions intentionally do not refresh dependent state afterward
  (e.g. `closeJob`/`assignTechnician`-equivalent calls) where a sibling
  transition does. This asymmetry has already been identified as **preserved,
  intentional legacy behavior** ported from the original JS frontend, not a
  bug — do not "fix" it to be consistent with sibling methods without
  approval (see §16).
- A `PUT /jobcards/{jobCardNumber}/complete` endpoint is deliberately
  commented out in `JobCardController`. Do not re-enable it as a convenience
  without confirming it's actually wanted — its being disabled may itself be
  intentional.

## 12. Technician Assignment Rules

- Two parallel models currently coexist: `JobAssignmentController`
  (`/api/v1/job-assignments`, proper foreign-keyed `JobAssignment` entity —
  assign/accept/start/complete/reassign/my/my-driver) and
  `RepairTaskController` (`/api/v1/repair-tasks`, simpler free-text
  `technicianName` field on `RepairTask`). **Do not consolidate these into one
  model** without explicit approval — both are currently exercised by live
  client code paths.
- **For any NEW technician/assignment functionality, default to
  `JobAssignment`** (the proper foreign-keyed model) **unless the task
  explicitly requires `RepairTask`** (e.g. the task specifically says to
  extend the free-text repair-task assignment flow, or must interoperate with
  existing `RepairTask` call sites). Do not choose `RepairTask` for new work
  by default or for convenience.
- **Never introduce a third assignment mechanism.** If neither existing model
  fits the request, stop and ask rather than designing a new one.

## 13. Integration Rules with the Flutter Client

- The Flutter app (`garagest_flutter` repo) is being actively ported from the
  JS frontend against **this backend's current contract**. Treat any endpoint
  path, request/response shape, status-transition side effect, or role
  behavior currently in use as a contract, not a first draft.
- Before changing any endpoint's path, request shape, response shape, status
  code, or side effects, check whether it's called by:
  - the static JS frontend (`src/main/resources/static/**/*.js`), and/or
  - the Flutter app's `lib/features/*/services/*.dart` files (in the sibling
    repo).
  If either depends on current behavior, changing it is a breaking change
  requiring explicit approval and coordinated update of the caller.
- A known, already-identified client-side bug exists: the Flutter
  `JobCardService.updateJob()` calls `PUT /job-cards/{id}` (hyphenated), but
  the real backend route is `PUT /jobcards/{id}` (no hyphen). **The fix
  belongs in the Flutter repo, not here** — do not add a `/job-cards/{id}`
  alias route on the backend to paper over it unless explicitly asked to; the
  two repos should be reconciled deliberately, not patched around.

## 14. Security and Secret-Handling Rules

- **Production secrets must never be committed to source control.** This
  includes database URLs/usernames/passwords, JWT signing secrets, OAuth
  client IDs/secrets, and API keys. `application.properties` currently
  contains live production values checked into git — this is a known,
  already-flagged incident, not a pattern to continue or extend.
- Do not add new secrets, credentials, or tokens directly into
  `application.properties`, any other tracked file, or a migration. New
  secrets must be externalized (environment variables, a secrets manager, or
  an untracked/gitignored profile file) — ask if the project's actual secret
  management approach for a new credential is unclear rather than defaulting
  to a checked-in properties file.
- **Never weaken existing security controls.** Do not remove, loosen, or
  bypass an existing `@PreAuthorize` check, a `SecurityConfig` matcher, a
  validation rule, an ownership check, or any other access-control mechanism
  — even if it seems to be getting in the way of the current task, even
  temporarily "to test something." If a security control is blocking
  legitimate work, report it and ask, rather than disabling it.
- **Report security vulnerabilities discovered during unrelated work.** If
  you encounter a security issue while doing something else (an unscoped
  query, a missing auth check, an exposed secret, an unauthenticated
  endpoint with real side effects, etc.), surface it clearly in your
  response — do not stay silent because it's outside the current task.
- **Do not silently fix security issues outside the requested scope.**
  Reporting a vulnerability is required; fixing it without being asked is
  not allowed. Flag it, describe the risk, and wait for direction — an
  unrequested security fix is still an unrequested behavior/architecture
  change (see §18) and may have its own product implications.
- **Do not propagate known vulnerabilities into new code.** Even though
  existing instances of a vulnerability class (e.g. unscoped tenant queries,
  missing `@PreAuthorize`, endpoints trusting client-supplied IDs without
  ownership checks) must not be silently fixed when out of scope, they must
  also never be copied into new code. "The existing code does it this way"
  is not a justification for a new endpoint or query to repeat a known-bad
  pattern — new code follows the secure pattern (§9, §10), not the
  vulnerable precedent.
- Do not print JWTs, passwords, or other credential material to logs.
  (`JwtAuthenticationFilter` currently has debug `System.out.println` calls
  that touch the resolved principal — don't extend this pattern to new code;
  flag it rather than silently removing it unless asked to clean it up.)
- Do not rotate, change, or invalidate the currently-committed secrets as a
  side effect of another task — secret rotation is an operational action with
  real consequences (invalidates active sessions, breaks the live DB
  connection, requires re-authorizing Google Drive) and needs to be a
  deliberate, explicitly-requested, coordinated action.
- Test/debug endpoints that perform real external side effects (e.g.
  `GoogleDriveOAuthController`'s `/test/*` routes) must not be left reachable
  without authentication in code you add or touch — but do not remove or
  re-gate the existing ones without approval, since that's a behavior change
  in its own right (see §17).

## 15. Testing Requirements

- This project uses `spring-boot-starter-*-test` modules (actuator, data-jpa,
  flyway, security-oauth2-resource-server, security, validation, webmvc) —
  use the appropriate slice-test starter for what you're testing rather than
  always booting the full context.
- Any new service-layer business logic (status transitions, scoping checks,
  validation rules) should have a corresponding unit or slice test.
- When fixing a confirmed bug (per §16), add a regression test that would have
  caught it, scoped to that specific bug — not a broader test-coverage
  expansion unless asked.
- Do not delete or weaken an existing test to make a change pass. If a change
  legitimately requires updating a test's expectations, explain why in the
  change description.
- See §16 for how "tested" and "verified" must be reported — a change is not
  done just because it compiles.

## 16. Source-of-Truth Hierarchy

When determining what the correct/intended behavior of the system is, or
whether a discrepancy is a bug, consult sources in this priority order:

1. **Approved product requirement** — an explicit instruction or decision from
   the user/product owner in the current conversation (or another
   clearly-approved source), specific to the point in question.
2. **Actual backend implementation** — what this codebase currently does
   (entity constraints, service logic, controller behavior, migration
   history) — the ground truth for what the system *actually* does today.
3. **Legacy JS frontend behavior** (`src/main/resources/static/`) — the
   original client this backend was built to serve; useful for understanding
   *why* the backend behaves as it does.
4. **Flutter implementation** (sibling repo) — the in-progress port; useful
   context, but it is the client adapting to this backend, not the other way
   around, and it may itself contain bugs (e.g. the `/job-cards` vs
   `/jobcards` mismatch).
5. **Documentation/comments** — code comments, this file, README-style notes.
   Valuable for stated intent, but comments can go stale and may not reflect
   current code.
6. **Agent inference** — your own reasoning about what "should" be correct,
   absent any of the above. This is the **lowest**-priority source and must
   never override 1–5.

**If sources disagree with each other, report the discrepancy explicitly and
do not silently pick a winner.** For example, if a code comment says one
thing but the actual implementation does another, or the Flutter client
assumes an endpoint behaves one way but the backend implementation behaves
differently, state both, say which sources conflict, and ask how to proceed
rather than resolving it unilaterally.

## 17. Distinguishing Bugs from Intentional Legacy Behavior

This is one of the most important judgment calls in this codebase. Before
"fixing" anything that looks wrong:

1. **Check whether it's already documented as intentional.** Comments across
   both this backend and the Flutter app explicitly mark several behaviors as
   deliberately-preserved parity with the original JS frontend (e.g.
   `WorkflowState.reset()` on the Flutter side, several skip-refresh
   transitions here). If a comment says "preserved as-is" or similar, treat
   that as authoritative (per §16) — do not change it because it looks like a
   bug.
2. **Check whether the same pattern is applied correctly nearby.** Several
   real bugs in this codebase were identified precisely because a sibling
   method in the same class does the right thing (e.g. `getDrivers(garageId)`
   correctly scopes by garage right next to `getTechnicians()`, which doesn't;
   `trackRepair()` correctly checks estimate/job-card ownership right above
   `getEstimateDetails()`, which doesn't). That inconsistency-with-a-sibling
   pattern is a strong signal of a genuine bug, not intentional design — but
   still **do not fix it silently**. Flag it and ask.
3. **When genuinely unsure, ask rather than assume.** Default to *not*
   changing behavior. State clearly: "this looks inconsistent with X, is it
   intentional?" and wait for direction before touching it.
4. **Never fix a "bug" as a side effect of an unrelated task.** If you notice
   one while working on something else, mention it, optionally propose it as
   a separate follow-up, and leave it alone in the current change.

## 18. Rules for Dealing with Duplicate/Overlapping APIs

Several deliberate-or-not overlaps exist in this codebase (job-card transition
controllers, technician-assignment models, navigation-table migrations named
identically). For all of them:

- Do not merge, delete, deprecate, or redirect one side into the other.
- Do not pick a "canonical" one and quietly stop maintaining the other.
- Do not add a compatibility shim/alias route to make two conventions look
  unified.
- If a task requires understanding which of two overlapping APIs is actually
  live, investigate call sites (both clients) and report findings — don't
  guess and don't consolidate based on a guess.
- Any consolidation, deprecation, or removal of one side of a duplicate API is
  an **architectural decision** requiring explicit approval, not an
  engineering cleanup to perform proactively.

## 19. Rules Preventing Unrelated Refactoring

- Make the smallest change that accomplishes the requested task. Do not:
  - rename methods/classes/packages for consistency,
  - reformat files you're not otherwise editing,
  - reorganize imports or package structure,
  - "clean up" commented-out code you encounter (some of it is intentionally
    preserved — see §17),
  - normalize inconsistent conventions (paths, response envelopes, error
    shapes, fetch types, etc.) as a drive-by improvement,
  - remove the dead `core.enums.UserRole` enum or similar identified dead code
    unless specifically asked to.
- If you notice something worth fixing that's out of scope for the current
  task, mention it in your summary as a suggestion rather than doing it.
- Any change with a blast radius beyond the specific file(s)/endpoint(s) named
  in the task — a new migration, a security-config change, a dependency
  bump, a change to `application.properties` — requires explicit confirmation
  before proceeding, even if it seems like an obvious improvement.

## 20. Git Safety Rules

- Run `git status` before making changes, to understand the current state of
  the working tree and avoid mistaking someone else's in-progress work for a
  clean baseline.
- Review `git diff` (and `git status` again) before declaring a task
  complete, to confirm the actual change matches what was intended and
  nothing unrelated was touched.
- **Never force-push.**
- **Never rewrite history** (no `commit --amend` on already-shared commits, no
  interactive rebase of shared history) unless explicitly requested.
- **Never `reset`, `rebase`, `checkout --`, `restore`, or `clean`** in a way
  that discards changes, unless explicitly requested for that specific
  operation.
- **Never commit automatically.** Only create a commit when the user
  explicitly asks for one in that turn — do not commit "to be safe" or
  because a logical unit of work is finished.
- **Never modify, stage, discard, or commit unrelated uncommitted work**
  found in the working tree. If unrelated changes are present, leave them
  exactly as found and mention them if relevant.

## 21. Completion Claims and Verification

**Never claim work is complete unless the relevant implementation and
verification actually occurred.** Distinguish explicitly between:

- **Implemented** — code was written/changed.
- **Tested** — an automated test was written and actually run, and it passed.
- **Manually verified** — the behavior was actually exercised (e.g. the
  endpoint was actually called, the query actually run against real/test
  data) and the result observed, not merely reasoned about.
- **Not verified** — the change was made but not run, tested, or checked in
  any way (e.g. because the environment doesn't allow it).

Do not describe a change as "done," "working," or "fixed" if it falls into
the "not verified" category — say so plainly instead (e.g. "implemented, not
yet run — I don't have a way to start the Postgres instance in this
environment"). Do not infer success from code merely looking correct.

## 22. Standard Completion Report

At the end of any non-trivial task, provide a completion report with these
sections (omit a section only if genuinely not applicable, and say so):

```
Changed
  — files/modules touched and a one-line description of each change.

API Changes
  — new/modified/removed endpoints, request/response shape changes, or
    "none".

Database Changes
  — new/modified migrations, schema impact, or "none".

Flutter Impact
  — whether this change affects the Flutter client's contract (endpoint
    path/shape/behavior), and if so, what the Flutter side needs to do about
    it, or "none".

Tests Run
  — which tests were run and their result, or "none run" (with why).

Verification
  — implemented / tested / manually verified / not verified, per §21, for
    each meaningful piece of the change.

Known Issues
  — anything left incomplete, uncertain, or deliberately deferred.

Out-of-Scope Findings
  — bugs, security issues, or inconsistencies noticed but not fixed, per
    §14, §16, §17, §19 — flagged for the user's attention, not acted on.
```

## 23. When to Ask Before Acting

Prefer asking over acting whenever a decision involves any of the following —
these are not judgment calls to make unilaterally, even when an answer seems
obvious:

- **Product behavior** — anything that changes what the system does from a
  user's perspective (a workflow transition, a validation rule, a role's
  capabilities, a status side effect).
- **Architecture** — consolidating duplicate APIs, changing module
  boundaries, introducing a new cross-cutting mechanism, changing the
  response envelope or error shape conventions.
- **Security boundaries** — anything touching authentication, authorization,
  `@PreAuthorize`, `SecurityConfig`, secret handling, or tenant scoping (see
  §10, §14).
- **Data ownership** — anything affecting which user/role/garage can read or
  write which data.
- **Destructive operations** — deleting data, dropping/altering schema outside
  a new additive migration, force-pushing, rewriting git history, discarding
  uncommitted work.
- **Breaking API changes** — any change to an existing endpoint's path,
  method, request shape, response shape, or status code that an existing
  client (§13) currently relies on.

For anything outside this list — a clearly-scoped, additive, low-risk change
that was explicitly requested — proceed directly rather than over-asking.

---

**This backend is a shared contract between a live legacy frontend and an
actively-migrating Flutter client — an "improvement" that isn't coordinated
with both is a regression.** When in doubt, stop and ask.
