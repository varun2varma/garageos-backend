# GarageST Automotive Catalog and GST Master: Source and Verification Report

Prepared 2026-10-10. Research and data preparation only. No application code, schema, migration, database, tax setting or existing catalog record was changed. The import was not run against any database.

> **SUPERSEDED COUNTS (2026-10-10 coverage audit).** The counts below are from the first release. Current counts, the full HSN/SAC review and the tested import are in `GarageST_Catalog_Coverage_Audit.md` and `GarageST_Import_Validation_Report.md`. Current: 495 parts, 238 services, 680 import rows in `GarageST_Catalog_Import_Batch_01..03.csv` (the old `_part1/_part2` files were removed).

## 1. Result in one paragraph

The complete official HSN/SAC directory was retrieved from the GST Portal, preserved unchanged and used as the basis for everything else. A broad automotive parts and workshop-services catalog was then built and mapped to **candidate** official codes by script (a code that is not in the official file is rejected). **No GST rate is stated and no record is verified**, because the current Central Tax (Rate) notification text could not be obtained. The workbook is a comprehensive working catalog, not a complete official tariff and not proof of any classification or rate.

## 2. Official sources

| Source | Result | Detail |
|---|---|---|
| GST Portal "Download HSN Directory in Excel Format" (linked from `services.gst.gov.in/services/searchhsnsac`) | **Downloaded and inspected** | URL `https://tutorial.gst.gov.in/downloads/HSN_SAC.xlsx`; public, no login or CAPTCHA; file `HSN_SAC.xlsx`; 668,474 bytes; SHA-256 `051108e31063f1ef0d6dfb005a622bcc848878fa2d94fae51a73e67f8916871e`; server Last-Modified 2026-10-09 13:05 GMT; retrieved 2026-10-10 08:58 UTC |
| Contents of that file | Codes and descriptions **only** | Sheet `HSN_MSTR`: 21,935 data rows (columns `HSN_CD`, `HSN_Description`; 2, 4, 6 and 8-digit entries; no duplicate codes). Sheet `SAC_MSTR`: 681 data rows (`SAC_CD`, `SAC_Description`). **No GST rate, UQC or effective date.** |
| GSTN advisory, revamped Search HSN | Read | `.../advisory_on_search_hsn_code_functionality_final.pdf`. Search results are a facilitation aid based on e-invoice usage, "not legally binding"; the full list is downloadable in Excel (the file above). |
| GSTN advisory, HSN validation in GSTR-1 Table 12 | Read | `.../updated_advisory_on_hsn_validation_21.01.25.pdf`. 4-digit HSN (turnover up to Rs 5 crore) or 6-digit (above) is mandatory; the same Excel list is downloadable from Table 12. |
| CBIC rates page `cbic-gst.gov.in/hindi/gst-goods-services-rates.html` | Retrieved, **stale** | Embeds goods (1,590 rows) and services (232 rows) schedules headed **"as on 01.04.2023"**; page footer "Last Updated 06 May 2025". Superseded by later changes, so not used as evidence. Kept only as the historical sheet `CBIC_Rates_2023_Ref`. |
| CBIC English page `cbic-gst.gov.in/gst-goods-services-rates.html` | Retrieved, **stale** | Links the 2017 chapter-wise schedules and services schedule. Not used. |
| CBIC Tax Information Portal `taxinformation.cbic.gov.in` | Reached, **rate text not obtained** | Its public update feed lists "01/2026-Central Tax (Rate)", 30-Apr-2026, "seeks to amend Notification No 9/2025 - Central tax (Rate)". The notification PDF could not be downloaded through the public routes tried (HTTP 401/404/500). The portal is a browser application with no consolidated machine-readable rate table found. |

## 3. What is missing (so the master data cannot be called complete)

1. **Current GST rates and conditions.** The Central Tax (Rate) notification text in force (for example 9/2025 as amended by 01/2026) was not retrieved. Without it no CGST/SGST/IGST rate, condition or effective date can be substantiated, so `GST_Verification` has none and every record is `NOT_VERIFIED`.
2. **Applicability of each code to each product.** The official file gives code and description only. Choosing between, for example, 8708 30 and 6813 81 for brake pads depends on facts the file does not hold.
3. **Source-data anomaly (preserved, not corrected).** In the official file the heading-level description of 8539 shows the text of 8538 (and 8538 that of 8537), while the sub-entries beneath are correct (for example 853921 "tungsten halogen"). Noted in `Automotive_HSN`.

## 4. Method

1. Downloaded and checksummed the official workbook; copied both sheets into the output unchanged (verified: identical to the source after reading back).
2. Selected automotive-relevant official rows by documented rules (not by hand-picking a sample): 76 four-digit headings that typically cover vehicle parts, lubricants, fluids, tyres, batteries, lighting, bearings, filters, consumables and EV components, plus a keyword flag for entries that name vehicle or engine use. SAC rows: 16 entries (the 9987 maintenance/repair group, 998714, installation, cleaning of transportation equipment, technical testing, towing, related residuals).
3. Built the taxonomy and items (OEM-neutral; no OEM part numbers or vehicle compatibility claims).
4. Mapped each item to candidate codes **by lookup**: the script rejects a code that is not in the official file, rejects goods codes on services and 99xxxx codes on parts, and copies the official description from the file. One round of the check removed four non-existent refrigerant sub-codes.
5. Statuses: `CANDIDATE` only where an official entry names the class (for example 8421 23 oil or petrol filters, 8421 31 intake air filters, 8511 10 sparking plugs, 8507 10 starting lead-acid batteries, 8708 80 suspension, 401110 tyres for cars, 998714 repair of transport machinery, 998538 cleaning of transportation equipment, 996744 towing); `NEEDS_REVIEW` where several entries are plausible or composition or construction decides; `UNCLASSIFIED` where no supportable entry exists. **`VERIFIED` is never used.**

## 5. Deliverables (all in `D:\garageos-backend\docs\gst-source\`)

| File | Content |
|---|---|
| `GarageST_Automotive_Catalog_and_GST_Master.xlsx` | 13 sheets: README, Summary, Official_HSN_Raw, Official_SAC_Raw, Automotive_HSN, Automotive_SAC, Parts_Master, Services_Master, GST_Verification, Needs_Review, Catalog_Import_Candidates, Import_Validation, CBIC_Rates_2023_Ref (historical only) |
| `GarageST_Catalog_Import_Candidates.csv` (+ `_part1.csv` 500 rows, `_part2.csv` 161 rows) | Import-ready; the API accepts at most 500 rows per request, hence the split files |
| `HSN_SAC.xlsx`, the two GSTN advisory PDFs, the two saved CBIC pages | Original source files, unchanged |
| `work/` | The scripts and data used (re-runnable): `build.py`, `data_parts.py`, `data_services.py`, `parse_cbic.py`, `xlsx_read.py` |

SHA-256: workbook `346a2f73...`, HSN_SAC.xlsx `051108e3...` (full values in the README sheet and the listing above).

## 6. Counts

| | Count |
|---|---|
| Official HSN rows downloaded / selected as automotive-relevant | 21,935 / 2,168 |
| Official SAC rows downloaded / selected | 681 / 16 |
| Parts records (PART 403, FLUID_LUBRICANT 43, DETAILING_PRODUCT 15, EV_SPECIALIST 13, WORKSHOP_CONSUMABLE 7) | 481 |
| Services records (LABOUR_OPERATION 206, OUTSOURCED_SERVICE 16, SERVICE_PACKAGE 11) | 233 |
| Parts: CANDIDATE / NEEDS_REVIEW / UNCLASSIFIED | 103 / 368 / 10 |
| Services: CANDIDATE / NEEDS_REVIEW / UNCLASSIFIED | 131 / 90 / 12 |
| VERIFIED | **0** |
| Rows in the import CSV | 661 (474 PART, 187 SERVICE) |
| Excluded from the import | 53: 19 already in migration V70, 11 service packages, 16 outsourced services, 7 workshop consumables |
| Duplicate names, alias collisions, invalid units, over-length fields, missing fields, unknown codes | 0 each (see Import_Validation) |

## 7. Import contract (confirmed from the code)

`POST /api/v1/catalog/import` (SUPER_ADMIN only), body `{"csv": "...", "dryRun": true|false}`; header `type,name,category,default_unit,description[,hsn_sac_code,tax_rule_code]`; units NOS SET PAIR LTR ML KG GM MTR KIT BOX PKT ROLL JOB HR; name 150, category 60, description 500; maximum 500 rows; adds only and never overwrites; a supplied code can become `PENDING_REVIEW` at most. The CSV carries **no** `hsn_sac_code` or `tax_rule_code`. Re-check units and lengths with a dry run first.

## 8. Duplicate handling

Normalised name is unique per type; aliases are kept as search synonyms and must not equal another item's name or be shared (the script found and removed three conflicts: a shared "Complete Engine" alias and two concepts duplicated by synonym: brake slack adjuster and hub seal). 19 of the 36 V70 starter names appear in the workbook (flagged `In V70 starter`) and are excluded from the CSV.

## 9. Limitations and cautions

- Aliases, descriptions, units, attributes and the item-to-code mapping are research inference, not law. Several descriptions are generated from the item name and subcategory plus the variant attributes; they are working descriptions.
- Default units and categories are working defaults, not tax statements.
- Service packages and parts supplied with labour raise a composite versus mixed supply question that is unresolved.
- Diagnostic and inspection services might fall under repair (998714) or technical testing (998346); body repair and painting under repair or job work (998881); accessory installation under installation or repair services. All are `NEEDS_REVIEW`.
- A portal search result is a candidate, not proof; the GSTN advisory says the same.
- The 2023 CBIC schedule columns in `GST_Verification` notes are labelled historical and must not be used as evidence.

## 9a. Decisions needed

1. Obtain the current Central Tax (Rate) notification (and amendments) and the accountant's rate decisions; then fill `GST_Verification` and create GST rules with sources in the app.
2. Decide whether service packages, outsourced services and workshop consumables belong in the shared catalog or stay as estimate "other charges".
3. Decide whether to import the 661 rows into a **non-production** database first (dry run, then real run), then classify with a SUPER_ADMIN.
4. Decide whether candidate codes should be loaded as `PENDING_REVIEW` (the API can do this) or stay in the workbook until verified.

## 10. Confirmation

No application code, schema, migration, tax setting, database record or existing catalog record was modified, and no import was run. Only new files under `docs/gst-source/` were created.
