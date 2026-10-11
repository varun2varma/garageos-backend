# GarageST Catalog Import: Contract Verification and Safe Test Import

Prepared 2026-10-10. The import contract below was read from the current code (`CatalogImportServiceImpl`, `CatalogServiceImpl.applyClassification`, `CatalogUnits`, `ImportCatalogRequest`, migrations V69/V70). No application code, schema or migration was changed. The import was run **only** in a throw-away schema of the local test database, which has been dropped.

## 0. Finalization update (second isolated run, current code)

The defect described in section 3 below (item 1) was **fixed** and re-verified. Sections 1 to 9 describe the first run against the unfixed code and are kept as evidence; the results that changed are here.

**Code change (backend, within the existing architecture, no endpoint or schema change):**
1. `CatalogServiceImpl.applyClassification` now rejects any code that does not match `^[0-9]{4,8}$` with the row error "HSN/SAC must be 4 to 8 digits." This is the rule already used by the request DTOs and by the database check `ck_catalog_item_code_format`, so no valid code is rejected and nothing is normalised. It applies to dry run and real run alike (and to create, update, classify and promote).
2. `CatalogImportServiceImpl.importCsv` is no longer one transaction. Each valid row is saved on its own; a row the database still refuses is reported as invalid ("Could not be saved; the row was skipped.") and does not roll back the other rows. Business-rule errors keep their existing per-row messages. Request-level errors stay HTTP 400. Dry run never calls save.

**New automated tests** (`CatalogImportServiceImplTest`, now 15 tests, all pass): malformed codes (space, letters, too short, too long, dot) rejected identically in dry and real run; 4-digit and 8-digit codes accepted as `PENDING_REVIEW`, never verified; HSN on a service and SAC on a part rejected; unknown tax rule is a row error; a database-refused row does not stop the others; exactly 500 rows accepted and 501 rejected; repeating an import creates nothing the second time; dry run never saves.

**Isolation (second run):** local server `127.0.0.1`, database `garageos_test`, throw-away schema `webcheck` (absent before, Flyway applied 70 migrations into it, absent after the drop); only the `public` schema existed beforehand and afterwards. Credentials were read from the local test configuration and never printed. Databases on the server: `garageos`, `garageos_test`, `postgres`; only `garageos_test` was touched.

| Step | Result |
|---|---|
| Starting state | 36 V70 starter rows (16 PART, 20 SERVICE), all `UNCLASSIFIED`, no code or rule, 0 tax rules |
| Dry run of 3 batches (703 rows) | 703 would be created, 0 duplicates, 0 invalid; **table dump byte-identical before and after** |
| Real import | 250 + 250 + 203 created, 0 duplicates, 0 invalid; database 739 rows (PART 511, SERVICE 228) |
| Field-by-field comparison | 703 of 703 identical (type, name, category, unit, description, `UNCLASSIFIED`, no code, no rule, active, created-by set); 36 starter rows unchanged; 0 unexpected rows |
| Re-import of batch 01 | 250 duplicates, 0 created |
| Malformed code `8421 31` | Dry run: row invalid. Real run: **HTTP 200**, valid row created, bad row reported; no HTTP 500 |
| Mixed batch (2 valid, 2 invalid) | created 2, invalid 2; well-formed codes saved as `PENDING_REVIEW` |
| Service with HSN-style code | Dry run: row invalid ("A service needs a SAC code (starting with 99).") |
| Authorization | Ordinary user (`USER` only): HTTP 403 on import, create, update, activate, classify, list admin items, list/modify tax rules, review queue and link; valid-body tax-rule create also 403 (a malformed body returns 400 first, no change); no token: HTTP 401 on all; `gst_tax_rule` stayed at 0 rows |
| Cleanup | Backend stopped; schema `webcheck` dropped (0 remaining); `public` unchanged |

**Backend tests run (current code):** 12 classes, 91 tests, 0 failures, 0 errors, 0 skipped: all `Catalog*` classes (including `CatalogSchemaIT` and `CatalogSeedIT` against the local test database), `EstimateItemServiceImplCatalogTest`, `PlatformAdmin*` tests and `CorsConfigTest`. The full backend suite was not run.

**Flutter:** no Flutter code changed. `flutter analyze lib/features/catalog`: no issues. `flutter test test/features/catalog`: 31 tests passed.

**Remaining contract notes:** the 500-row and 200,000-character limits are unchanged (HTTP 400). Row reasons are free text. A partial-failure import is not atomic by design: rows are committed one at a time. `GET /auth/me` without a token still returns HTTP 500 (unrelated, not changed).

## 1. Environment and isolation proof

| Evidence | Value |
|---|---|
| Server | `jdbc:postgresql://localhost:5432/garageos_test`; `inet_server_addr()` = `127.0.0.1`; PostgreSQL 16.14 |
| Databases on that server | `garageos`, `garageos_test`, `postgres`. Only `garageos_test` was touched. |
| Target schema | `webcheck`, created by Flyway on first start, absent before (schemas listed: `public` only), absent after |
| Backend | The packaged jar started locally on port 8080, profile `webcheck`, `SPRING_FLYWAY_SCHEMAS=webcheck`, JDBC `currentSchema=webcheck` (Flyway log: "Successfully applied 70 migrations to schema webcheck, now at version v70") |
| Credentials | Read from the committed local test configuration; never printed. The driver script refuses any base URL other than localhost:8080. |
| Not touched | `public` schema of `garageos_test`, database `garageos`, any staging or production system. `SUPER_ADMIN` grants in `public` after the run: 0. |

The one-time SUPER_ADMIN used for the call was created by the documented bootstrap (`PlatformAdminBootstrapRunner`) inside the isolated schema only.

## 2. Import contract (verified against the code)

| Aspect | Current behaviour |
|---|---|
| Endpoint | `POST /api/v1/catalog/import`, SUPER_ADMIN only (unauthenticated: HTTP 401) |
| Body | `{"csv": "<text>", "dryRun": true|false}`. `dryRun` defaults to **true** unless explicitly `false`. `csv` must be non-blank and at most 200,000 characters (else HTTP 400 `Validation Failed`) |
| Header | Case-insensitive; must contain `type`, `name`, `default_unit` (else HTTP 400). Optional: `category`, `description`, `hsn_sac_code`, `tax_rule_code`. Unknown columns are ignored |
| Rows | At most 500 data rows per request (else HTTP 400). Blank lines skipped. RFC 4180 quoting supported (verified) |
| `type` | `PART` or `SERVICE`, case-insensitive |
| `name` | Required, 150 characters at most; trimmed. Uniqueness is per type on the normalised name (trim, collapse whitespace, lower case) |
| `category` | Optional (blank is stored as null), 60 characters at most |
| `description` | Optional (blank becomes null), 500 characters at most |
| `default_unit` | Required; upper-cased; one of NOS, SET, PAIR, LTR, ML, KG, GM, MTR, KIT, BOX, PKT, ROLL, JOB, HR |
| Duplicates | Within the file (later row reported "Repeated earlier in this file") and against the catalog ("Already in the catalog; not changed"). Never updates an existing row |
| Result for new rows | `active = true`, `created_by_user_id` set, classification `UNCLASSIFIED` when no code is supplied. A supplied code makes the item `PENDING_REVIEW` at most; an import can never verify |
| Classification checks | Service code must start with 99; part code must not; unknown `tax_rule_code` rejected. These are `BusinessException`s, reported per row |
| Dry run | Runs every check and counts what would be created but does not save |
| Transaction | One transaction per request. Per-row business errors are collected and the valid rows are saved (partial import). Any other exception rolls the whole request back |
| Report | `{dryRun, totalRows, created, duplicates:[{row,name,reason}], invalid:[...]}` inside the usual `data` envelope; HTTP 400 for request-level failures (`success:false`, `message`, `errors`, `requestId`) |

## 3. Limitations found in the first run (item 1 is FIXED, see section 0; the rest stand)

1. **Malformed code fails the whole request, and dry run does not see it.** The database check `ck_catalog_item_code_format` requires `^[0-9]{4,8}$`. A code such as `8421 31` (with a space) is not rejected by the service, so a dry run reports it as creatable; the real run then fails at save with HTTP 500 (`DataIntegrityViolationException`, body `null`) and **rolls back every row in that request** (verified: the preceding valid row in the same request was not saved). Mitigation here: the CSV files carry no code column, so none of the 680 rows can trigger this. Any future file with codes must be pre-validated against `^[0-9]{4,8}$`. This is a backend gap (not fixable from the Flutter or data side).
2. Dry run is not a perfect predictor in that case only; for every other rule it matched the real run (680 predicted, 680 created).
3. Row numbers in the report count data rows from 1 (the header is not counted).
4. The report's `name` echoes the trimmed, case-preserved input.
5. Unrelated observation (out of scope): an unauthenticated `GET /api/v1/auth/me` returned HTTP 500 with an unhandled class-cast error in the log instead of 401. Not changed.

## 4. Files tested

| File | Rows | Characters |
|---|---|---|
| `GarageST_Catalog_Import_Batch_01.csv` | 250 | 48,700 |
| `GarageST_Catalog_Import_Batch_02.csv` | 250 | 46,760 |
| `GarageST_Catalog_Import_Batch_03.csv` | 180 | 27,353 |
| Total | 680 (488 PART, 192 SERVICE) | |

All are inside the 500-row and 200,000-character limits (asserted by `build.py`). Columns: `type,name,category,default_unit,description`. No `hsn_sac_code` and no `tax_rule_code`.

## 5. Existing tests run

`./mvnw.cmd test` with the catalog classes, results read from the surefire reports:

| Class | Tests | Result |
|---|---|---|
| `CatalogImportServiceImplTest` | 8 | pass |
| `CatalogServiceImplTest` | 12 | pass |
| `CatalogManagementServiceTest` | 14 | pass |
| `CatalogReviewServiceImplTest` | 7 | pass |
| `CatalogControllerSecurityTest` | 2 | pass |
| `CatalogSchemaIT` (local test database) | 5 | pass |
| `CatalogSeedIT` (local test database) | 6 | pass |
| `EstimateItemServiceImplCatalogTest` (matched by the pattern) | 14 | pass |

54 catalog tests plus 14 estimate-item catalog tests: 0 failures, 0 errors, 0 skipped. The "duplicate key value violates unique constraint" line in the console is the expected output of the schema test that proves uniqueness. The full backend suite was not re-run (no backend code changed in this task).

## 6. Dry run

Starting state of the isolated schema: 36 rows (the V70 starter items: 16 PART, 20 SERVICE, all `UNCLASSIFIED`), 0 rows with a code or rule, 0 `gst_tax_rule` rows.

| Batch | Rows | Would create | Duplicates | Invalid |
|---|---|---|---|---|
| 01 | 250 | 250 | 0 | 0 |
| 02 | 250 | 250 | 0 | 0 |
| 03 | 180 | 180 | 0 | 0 |
| Total | 680 | 680 | 0 | 0 |

**Read-only proof:** after the dry run the table was queried again: still 36 rows (PART 16, SERVICE 20), 0 with a code or rule. Zero duplicates also confirms no name in the 680 collides with the 36 V70 starter names.

## 7. Real import (isolated schema only)

| Batch | HTTP | Created | Duplicates | Invalid |
|---|---|---|---|---|
| 01 | 200 | 250 | 0 | 0 |
| 02 | 200 | 250 | 0 | 0 |
| 03 | 200 | 180 | 0 | 0 |

Database after: 716 rows = PART 504 + SERVICE 212 (36 starter + 680 imported), all `UNCLASSIFIED`, 0 rows with a code or rule, 0 `gst_tax_rule` rows.

**Comparison (`compare_import.py`):** every one of the 680 CSV rows was matched to a database row by type and normalised name, and type, name, category, unit, description, status (`UNCLASSIFIED`), `hsn_sac_code` (null), `tax_rule_code` (null), `active` (true) and `created_by_user_id` (set) were compared: **680 of 680 identical, 0 problems, 0 unexpected extra rows**. The 36 starter rows were byte-for-byte unchanged.

## 8. Failure and partial-import behaviour

All run against the isolated schema. Rows named `ZZ Import Test ...` are test data.

| Case | Outcome |
|---|---|
| Invalid unit / blank unit | Row invalid: "Unit must be one of: BOX, GM, HR, JOB, KG, KIT, LTR, ML, MTR, NOS, PAIR, PKT, ROLL, SET." |
| Invalid type | Row invalid: "Type must be PART or SERVICE." |
| Blank name | Row invalid: "Name is required and at most 150 characters." |
| Name 151 characters | Row invalid (same message) |
| Category 61 / description 501 characters | Row invalid with the matching message |
| Repeated within the file (case and spacing differ) | Second row reported duplicate "Repeated earlier in this file." |
| Existing item, different case or spacing | Duplicate "Already in the catalog; not changed." |
| Existing V70 starter name | Duplicate, not changed |
| Same name, other type | Not a duplicate |
| Quoted field with comma and doubled quote | Accepted |
| Part with a `99...` code | Row invalid: "A part needs an HSN code; 99xx codes are service (SAC) codes." |
| Service with an HSN-style code | Row invalid: "A service needs a SAC code (starting with 99)." |
| Unknown `tax_rule_code` | Row invalid: "Unknown tax rule: ..." |
| Missing required header column | HTTP 400 "The CSV header must contain: type, name, default_unit." |
| Empty CSV | HTTP 400 `Validation Failed`, field `csv` must not be blank |
| 501 rows | HTTP 400 "An import is limited to 500 rows." |
| CSV text over 200,000 characters | HTTP 400 `size must be between 0 and 200000` |
| No bearer token | HTTP 401 |
| **Re-import of batch 01 (real run)** | 250 duplicates, 0 created; nothing changed (idempotent) |
| **Partial import (real run)** with 2 valid and 2 invalid rows (one bad unit, one part with a SAC) | HTTP 200: created 2, invalid 2. The 2 valid rows were saved with their well-formed codes as `PENDING_REVIEW` (never verified) |
| **Malformed code (real run)**, valid row followed by a row with `8421 31` | HTTP 500; **neither** row saved (whole request rolled back) |

Result: business-rule failures are reported per row and valid rows are kept; a database-level failure aborts the whole request.

## 9. Cleanup

The backend process was stopped, the schema `webcheck` was dropped (`DROP SCHEMA ... CASCADE`; check afterwards: 0 remaining; databases and the `public` schema unchanged; no `SUPER_ADMIN` grant in `public`). The test rows (the two `ZZ` test rows and everything imported) existed only in that schema and are gone. Scratch outputs (logs, dumps) remain in the session scratchpad outside the repository. No database record, existing catalog item or configuration anywhere else was changed.

## 10. What this proves and what it does not

- **Proven:** the 680-row dataset passes the real import code, and each field arrives in the database exactly as in the CSV, as `UNCLASSIFIED`, with no code and no rate, in a freshly migrated V70 schema.
- **Not proven:** anything about a staging or production database (their contents are unknown and must be checked by query after any import there); any classification, any rate, any legal conclusion.
- **Not done:** no import into any shared or production database, no commit, no push, no deployment.

## 11. Re-running

```
python docs/gst-source/work/build.py                       # regenerate workbook, CSV, batches
python docs/gst-source/work/run_import.py dry  <pwfile> <superadmin-username>
python docs/gst-source/work/run_import.py real <pwfile> <superadmin-username>
python docs/gst-source/work/compare_import.py <baseline.tsv> <after.tsv>
python docs/gst-source/work/import_edge_cases.py <pwfile> <superadmin-username>
```

Only against a local, isolated backend.
