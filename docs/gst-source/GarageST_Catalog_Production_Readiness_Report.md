# GarageST Master Catalog: Implementation and Production-Readiness Report

Prepared 2026-10-10. This report states what is ready, what is not, and what must happen before the catalog is loaded into production. It is not a statement that the catalog is tax-complete or ready for GST billing: **it is not.**

## 1. Verdict

| Area | Status |
|---|---|
| Catalog content (identity data) | Ready for review and a staged import: 733 records, 703 import-eligible, 0 name or alias conflicts |
| Import API correctness | Fixed and tested (malformed codes, per-row isolation, idempotency, limits) |
| Platform-admin management (backend and Flutter) | Present and authorized; one gap (no import screen) |
| GST classification | **Not ready.** 0 verified mappings, 0 rates, 0 GST rules |
| GST rates | **Blocked.** The current Central Tax (Rate) notification text could not be retrieved |
| Approved-estimate correction path | **Blocker** (section 5) |
| Production | **Not touched.** Nothing was imported, committed, pushed or deployed |

## 2. What was changed in this pass

Backend (only these two source files; both previously uncommitted work from earlier tasks):
- `CatalogServiceImpl.applyClassification`: HSN/SAC format check `^[0-9]{4,8}$` (same rule as the DTOs and the database check).
- `CatalogImportServiceImpl.importCsv`: per-row saves, no all-or-nothing rollback, database-refused rows reported.
- Tests: `CatalogImportServiceImplTest` grew from 8 to 15 tests.
- `GARAGEST_DEVELOPER_HANDBOOK.md` section 27E: validation and transaction semantics.

Data and research (under `docs/gst-source/`): workbook (26 sheets) regenerated from `work/build.py`; new sheets `SAC_Mapping_Rationale` and `Decisions_Required`; import files regenerated as three batches (250, 250 and 203 rows); coverage audit and import validation reports updated; scripts `audit_data.py`, `patch_scope.py`, `run_import.py`, `compare_import.py`, `import_edge_cases.py` added or updated. `HSN_SAC.xlsx` and the raw sheets are unchanged (value comparison True for both).

No Flutter code, schema, migration, tax setting or GST rule was changed.

## 3. Shared versus garage-specific data

The shared `catalog_item` holds identity and classification only: type, name, description, category, default unit, candidate HSN/SAC, classification status and provenance, tax-rule code, active flag, audit fields and a lock version. Selling prices, discounts, purchase costs, suppliers, stock, availability, package composition and garage billing settings stay with the garage and are not catalog fields. The model has no aliases or class field; aliases and the consumable/outsourced/specialist marker are carried in the workbook and in the description text. (Aliases are therefore not imported; the search-synonym value stays in the workbook until the model supports it.)

## 4. Platform-admin catalog management review

Backend: every `/api/v1/catalog` administration endpoint is SUPER_ADMIN only, enforced by `@PreAuthorize` and again in the service, and `CatalogControllerSecurityTest` fails if an endpoint loses its rule. Verified over HTTP in the isolated run: an ordinary user gets 403 and a caller without a token gets 401 on import, create, update, activate, classify, admin list, tax-rule list/create/update, review queue and link.

Flutter (read, not changed): the Master data area offers Parts and Services lists with search, status and active filters and paging; add and edit with HSN/SAC and verification validation; activate and deactivate with confirmation; the classification review queue with link and promote; and tax-rule list, create and activate/deactivate. Loading and error states are present. Role visibility is UI only; the backend is the control. `flutter analyze` (catalog feature) has no issues and 31 catalog tests pass.

Gaps: there is **no import screen** (import is API-only for a SUPER_ADMIN), and **no edit of an existing tax rule** (create and activate/deactivate only). I did not build either; they are separate features. No default admin account or bootstrap secret was added; the existing one-time bootstrap (handbook 27F) is unchanged.

## 5. Approved-estimate concern (inspected, not changed)

`CatalogReviewServiceImpl.resolve` copies the catalog classification onto an estimate line only when the estimate is not `APPROVED` and has no invoice. For an approved or invoiced estimate it records the catalog reference and sets the line to `RESOLVED`, but **does not copy the classification**, so the line stays without a code or rate and leaves the review queue. There is no supported way afterwards to correct such a line. Under `require-classification=true` (recommended for production once data exists), an invoice cannot be issued from such an estimate.

Safest supported path today: none for the existing approved line. Practical workaround within current behaviour: create a revised estimate from the corrected catalog item (needs customer re-approval).

Recommended backward-compatible design (a separate task, not done here): an explicit, audited "classify approved line" action for a tax-authorised role that (a) is allowed only while no invoice exists, (b) never changes quantity, price or totals, (c) records who, when and why in `audit_event`, and (d) for an invoiced estimate, leaves the issued invoice untouched and uses a credit-note or reissue process decided by the accountant. Financial snapshot semantics must not change.

**Update:** the correction path for an approved estimate without an invoice is now implemented (handbook section 27G): an audited, SUPER_ADMIN-only action that is refused with HTTP 409 if it would change the approved tax. Invoiced estimates still have no correction path (credit-note/reissue remains a blocker).

## 6. GST position

- HSN/SAC code existence in the downloaded directory, applicability to a product, rate, conditions and effective dates, catalog identity and garage price are kept apart everywhere.
- 0 mappings are VERIFIED; 0 GST rules exist; no rate appears in any file. The 2023 CBIC schedule is kept only as a labelled historical sheet.
- Source URLs and the retrieval date (2026-10-10) are recorded in the workbook README and the source report. The access limitation (current notification not retrievable) is recorded.
- The import files carry **no** `hsn_sac_code` or `tax_rule_code`. The API now handles codes safely (`PENDING_REVIEW` at most, malformed codes rejected per row), but loading unconfirmed codes would put unreviewed guesses in front of the review queue, so they stay in the workbook until a tax professional confirms them.

## 7. Production load plan (proposal, for a human to approve and run)

1. Take a verified backup of the target database. Confirm by query that the target contains only the 36 V70 starter rows in `catalog_item`.
2. On staging first (never by this tool): run the three batch files with `dryRun=true`; expect 0 duplicates and 0 invalid; compare counts before and after the dry run.
3. Run `dryRun=false` batch by batch; compare the database to the CSV with `compare_import.py`-style checks; confirm all rows `UNCLASSIFIED`.
4. Do the classification work in the Master data screen only after the tax professional's decisions (section 8); verify items one at a time with a tax rule and a recorded source.
5. Only then consider `GARAGEST_TAX_REQUIRE_CLASSIFICATION=true` (handbook 27F).

## 8. Open decisions (also in the workbook sheet `Decisions_Required`)

Tax professional: current rates, conditions and effective dates; whether 998714 suits repair, tyre, body, electrical and EV work and the composite-supply treatment; the correct HSN for each NEEDS_REVIEW part; the 8538/8539 source anomaly; the pricing convention.
Product owner: per-garage service packages; consumables as billable items; the approved-estimate correction path; a class/kind field; a structured error code in import reports; how the 36 generic V70 starter names relate to the specific variants here (both now coexist).

## 9. Test summary

Backend, current code: 12 classes, 91 tests, 0 failures, 0 errors, 0 skipped (all Catalog classes including the two database-backed ITs against the local test database, EstimateItem catalog test, PlatformAdmin tests, CorsConfigTest). The full backend suite was not run. Flutter: `flutter analyze lib/features/catalog` no issues; `flutter test test/features/catalog` 31 passed. Platform builds were not run (no Flutter code changed).

## 10. Confirmations

No production or staging database was contacted. The only database written to was a throw-away schema `webcheck` in the local `garageos_test` database, created and dropped by this task (0 remaining; `public` and the `garageos` database untouched). No commit, push, deployment or production setting change occurred. No credential is printed in any report. No existing catalog record was deleted and no estimate or invoice snapshot was modified.
