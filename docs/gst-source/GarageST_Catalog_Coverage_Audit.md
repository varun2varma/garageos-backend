# GarageST Automotive Catalog: Coverage Audit and Quality Gate

Prepared 2026-10-10. Research and data preparation only. No application code, schema, Flyway migration, tax setting or production data was changed. Nothing here is tax advice or a verified classification.

## 1. Result in brief

| Question | Answer |
|---|---|
| Is the official source complete and untouched? | Yes for what the portal provides: `HSN_SAC.xlsx` (SHA-256 `051108e31063f1ef0d6dfb005a622bcc848878fa2d94fae51a73e67f8916871e`, 668,474 bytes) is unchanged, and `Official_HSN_Raw` / `Official_SAC_Raw` in the workbook are value-identical to its two sheets (checked by script). The file holds codes and descriptions only: no rates, no unit of quantity, no dates. |
| Was every official row reviewed? | Every **four-digit HSN heading (1,367)** and every **SAC row (681)** has a recorded decision (`HSN_Review`, `SAC_Review`, `Coverage_Matrix`). |
| Is coverage proven exhaustive? | **No.** The HSN review is at heading level, assisted by a wording sweep. The 8-digit entries inside headings that were not selected were not each read by a person. The seven headings flagged in the first audit were resolved individually in the finalization pass (section 5), but the wording sweep that found them is itself limited. Coverage is "reviewed and documented", not "proven complete". |
| Are there GST rates? | **No.** The current Central Tax (Rate) notification text still could not be retrieved, and the 2023 CBIC schedule is stale. Every record stays `NOT_VERIFIED`; no rate is stated anywhere. |
| Catalog after finalization | 495 parts + 238 services = 733 records; 703 import-eligible (495 PART, 208 SERVICE); 30 excluded with reasons (19 already seeded by V70, 11 garage-specific service packages). |
| Tested import | 703 rows imported into an isolated throw-away schema of the local test database and compared field by field (section 8 and the validation report). Not imported anywhere else. |

## 2. Official source verification

| Item | Value |
|---|---|
| Source | GST Portal "Download HSN Directory in Excel Format", `https://tutorial.gst.gov.in/downloads/HSN_SAC.xlsx`, retrieved 2026-10-10 |
| `HSN_MSTR` | columns `HSN_CD`, `HSN_Description`; 21,935 data rows; 2, 4, 6 and 8-digit entries; 1,367 distinct four-digit headings across the chapter rows |
| `SAC_MSTR` | columns `SAC_CD`, `SAC_Description`; 681 data rows (32 group/heading rows including the 2-digit row, and 649 six-digit service codes) |
| Raw sheets in the workbook | Identical to the source (script comparison returns True for both) |
| Source status | The portal's search tool is a facilitation aid, not legally binding (GSTN advisory). A code found there is a candidate, not proof of classification. |

## 3. Method

1. **Chapters.** Each of the HSN chapters was given a scope decision. 28 chapters (`27, 28, 29, 31, 32, 34, 35, 38, 39, 40, 56, 57, 59, 63, 68, 70, 72, 73, 76, 82, 83, 84, 85, 86, 87, 90, 94, 96`) can contain parts, fluids, consumables, tools or components and were reviewed heading by heading. All other chapters (food, agriculture, textiles, minerals, footwear, jewellery, arms, toys, art and similar) were excluded **by chapter**.
2. **Headings.** In the reviewed chapters each four-digit heading is `SELECTED` (91 headings, with the reason recorded) or `REVIEWED_NOT_SELECTED`.
3. **Wording sweep (stragglers).** For every heading, including those in excluded chapters, the descriptions of all entries beneath it were searched for vehicle, engine, brake, tyre, lubricant, clutch and similar wording. A hit on an unselected heading is either resolved with a written reason (for example complete vehicles in 8701 to 8704 are out of scope for a parts and services catalog) or left `FLAGGED_FURTHER_REVIEW`.
4. **SAC.** All 681 rows were read group by group. 25 are `SELECTED` (including the 9987 group row), 31 are other group rows, 164 are `REVIEWED_RELATED_NOT_SELECTED` (wording touches transport, repair, cleaning, rental and similar but is not a vehicle-workshop service) and 461 are `REVIEWED_NOT_SELECTED`.
5. **Preserved fields.** Every row keeps code, official description, source sheet, parent chapter/group, decision, reason, and the count of catalog items mapped to it.

## 4. Coverage numbers

| HSN four-digit headings | Count |
|---|---|
| Total reviewed | 1,367 |
| SELECTED as automotive-relevant | 92 |
| Reviewed in a relevant chapter, not selected | 528 |
| Excluded by chapter (no wording hit) | 747 |
| Flagged for further research (unresolved) | 0 |

Selected headings cover 2,544 official HSN rows (the heading and everything beneath it). The first release selected 76 headings; the audit added 14 (`3917, 3919, 3923, 3926, 7326, 7616, 8424, 8425, 8503, 8518, 8525, 8529, 8543, 8705`).

| SAC rows | Count |
|---|---|
| Total reviewed | 681 |
| SELECTED | 25 |
| Group/heading rows | 31 |
| Reviewed, related wording, not selected | 164 |
| Reviewed, not selected | 461 |

**Relationship between official SAC descriptions and GarageST services:** see the `SAC_Mapping_Rationale` sheet (official description verbatim, services mapped, how the entry relates, open ambiguity). Most workshop operations have no dedicated SAC; many share the broad repair entry 998714, and applicability is a tax-professional decision.

**Did the first 16 SAC selections omit relevant rows?** Yes. The audit added: `998349` other technical and scientific services, `998531` disinfecting services (cabin sanitisation), `998599` other support services, `998883` job work for bus body building, `998873` fabricated metal product and metal treatment services, `997311` rental of transport equipment without operator, `996743` parking lot services, `998523` security systems services, `998717` repair of commercial and industrial machinery. All are research candidates, none is mapped as verified.

## 5. The seven flagged headings, resolved (finalization pass)

Each heading's official sub-entries were read from `HSN_SAC.xlsx`.

| Heading | Official sub-entries (abridged) | Decision and reason |
|---|---|---|
| 2711 | 271111 natural gas, 271112 propane, 271113 butanes, 2711 19/21/29 | **Excluded.** Fuels (CNG/LPG), not parts, fluids or workshop services |
| 8426 | cranes, mobile lifting frames, 842691 "designed for mounting on road vehicles" | **Excluded.** Lifting equipment: workshop or vehicle equipment, not a billable catalog part |
| 8462 | forging and press machine tools, press brakes, hydraulic presses | **Excluded.** Industrial metal-forming machinery |
| 8467 | hand-held pneumatic, hydraulic or motor tools (drills, saws) | **Excluded.** Garage-owned tools, not a catalog item sold to customers |
| 8477 | rubber and plastics machinery (847751 tyre moulding or retreading machines) | **Excluded.** Manufacturing machinery |
| 8502 | electric generating sets, rotary converters | **Excluded.** Workshop utility equipment |
| 8505 | 850520 "electro-magnetic couplings, clutches and brakes" | **Selected.** `850520` added as an alternate NEEDS_REVIEW candidate for AC Compressor Clutch (an electromagnetic clutch) |

This is a scope judgement for a typical independent workshop. A workshop that sells or rents tools, equipment or fuel would need a different catalog scope.

Headings `3917, 3919, 3923, 3926, 7326, 7616, 8424, 8425, 8503, 8505, 8518, 8525, 8529, 8543, 8705` were added to the selected list in the two passes. **Method limit, restated:** selection and review are at heading level plus a wording sweep. 8-digit entries inside non-selected headings were not individually read, and a part whose official description does not use the sweep's keywords could be missed. The workbook does not claim exhaustive coverage.

### 5a. Shared-catalog scope (finalization decisions)

The model has only type PART/SERVICE, category, default unit and classification; it has no class field, so the class is carried by category and a description marker.

| Class | Decision |
|---|---|
| Parts and fluids | In the shared catalog |
| Workshop consumables (7) | In, as PART with a "Workshop consumable" description marker; the garage decides whether to bill them as other charges |
| Outsourced services (16) | In, as SERVICE with a "Commonly outsourced" description marker |
| Specialist EV services (9) and EV components (13) | In, with a "Specialist" marker |
| Service packages (11) | **Not in the shared catalog**: composition and price are garage-specific; their underlying operations are separate services |
| V70 starter items (36) | Preserved; 19 overlapping names excluded from the import so no duplicate is created |
| Prices, costs, suppliers, stock, availability, garage tax settings | Never catalog fields |

## 6. Source anomalies (preserved, never corrected)

`Source_Anomalies` lists 49 items found by script: 21 duplicate SAC descriptions, 13 adjacent and 12 non-adjacent identical four-digit heading texts, 1 group of codes with an unusual length, and the 8538/8539 case.

**8537 / 8538 / 8539, observed evidence.** In `HSN_MSTR` the heading row `8538` repeats the text of `8537` ("boards, panels, consoles, desks, cabinets... for electric control"). The heading row `8539` carries wording about "parts suitable for use solely or principally with the apparatus of headings 8535, 8536 or 8536" (8536 appears twice). The sub-entries beneath tell a different story: `853810` describes boards for goods of 8537, while `853910` is "sealed beam lamp units", `853921` "tungsten halogen" and `853931` "fluorescent, hot cathode". The heading texts therefore look shifted by one heading relative to their sub-entries. The file alone cannot say which text is intended, and no correction was made. **Effect:** lamp and bulb items (halogen, LED, HID, signal bulbs) are `NEEDS_REVIEW`, and the first two were demoted from `CANDIDATE` in this audit.

## 7. Catalog audit

### Findings and what was done

| Check | Finding | Action |
|---|---|---|
| Missing common items | 19 gaps found: audio head unit, car speaker, antenna, dash camera, vehicle fire extinguisher, hydraulic jack, roof rack, steering wheel cover, gear knob, pedal assembly, exhaust heat shield, reflector, vehicle cover, sunshade (14 parts); TPMS sensor replacement, wheel weight replacement, fire extinguisher fitting, flat tyre roadside assistance, fuel delivery assistance (5 services) | Added, labelled `Coverage-audit addition (research-derived)` in the new `Origin` column |
| Duplicate concepts | Candidates that duplicated existing items were not added or were folded in as aliases: Car Audio Installation, Dash Camera Installation, Roof Rack Installation and Battery Jump Start (aliases of existing services); Wheel Balancing Weight (covered by the clip-on and adhesive items); Fuel Filler Cap, Sump Pan, TPMS Sensor (existing items already cover them). Earlier pass had already removed Complete Engine, Brake Slack Adjuster and Hub Seal duplicates. | Name uniqueness per type and alias collisions both re-checked: 0 remaining |
| Over-confident statuses | Halogen Bulb, LED Bulb (8539 anomaly), Fuel Pump - Electric and Fuel Pump Module (8413 30 names pumps for combustion engines), Retreaded Tyre (two plausible entries), Windscreen, Rear Windscreen (8708 22 versus 7007 11) were `CANDIDATE` | Demoted to `NEEDS_REVIEW` with notes |
| Exact tariff line | Turbocharger had only the six-digit parent | `84148030` added as first candidate (it exists in the official file); status stays `NEEDS_REVIEW` |
| Parts versus services | Parts carry HSN-type candidates, services carry SAC-type candidates; the build rejects a goods code on a service or a 99xxxx code on a part | 0 violations |
| Packages, outsourced, consumables | 11 packages, 16 outsourced services, 7 consumables | Packages stay out of the shared catalog (garage-specific); outsourced services and consumables are in, with description markers (section 5a) |
| Generic names | "Gear Set", "Sensor Replacement", "Electrical Diagnostics" are broad | Kept (they are real shop terms); "Sensor Replacement" was demoted from `CANDIDATE`, the others were already `NEEDS_REVIEW`; a product decision is needed on whether to split them |
| Meaningless variants | None found: size, voltage and fitment are kept as attributes, not separate items | No change |
| Names, units, lengths | Every name 150 characters or fewer, category 60 or fewer, description 500 or fewer, units inside the 14-unit whitelist, required fields present | 0 violations |
| Unsupported claims | Descriptions state function only; no OEM part numbers, vehicle compatibility, prices or rates | No change |
| Code-to-item review | Every distinct candidate code was printed with its official description against the items using it (195 distinct codes) | See the status changes above; broad codes (8409 91/99, 8708 xx, 4016 93) stay `NEEDS_REVIEW` |

### Audit log (workbook sheet `Catalog_Audit_Log`)

`ADDED` 19, `STATUS_CHANGED` 8, `PRIMARY_CODE_CHANGED` 1, nothing removed. Mapping to the first release is keyed on type and name.

### Final counts

| | Parts | Services | Total |
|---|---|---|---|
| Records | 495 | 238 | 733 |
| CANDIDATE | 97 | 130 | 227 |
| NEEDS_REVIEW | 388 | 96 | 484 |
| UNCLASSIFIED | 10 | 12 | 22 |
| VERIFIED | 0 | 0 | 0 |

By record class: PART 417, FLUID_LUBRICANT 43, DETAILING_PRODUCT 15, EV_SPECIALIST 13, WORKSHOP_CONSUMABLE 7, LABOUR_OPERATION 211, OUTSOURCED_SERVICE 16, SERVICE_PACKAGE 11.

Origin: earlier research pass 714 (481 parts, 233 services), coverage-audit additions 19.

## 8. Classification integrity

- Item name, official code, applicability, rate and garage price are separate things in the workbook. A candidate code is never a statement that the code applies.
- Status vocabulary matches the product: workbook `CANDIDATE / NEEDS_REVIEW / UNCLASSIFIED`; the backend has `UNCLASSIFIED / PENDING_REVIEW / VERIFIED`. The import file carries **no** code, so every imported item becomes `UNCLASSIFIED`.
- `Verified_Mappings` is empty by design. No rate, notification, effective date or condition was invented.
- The 2023 CBIC schedule appears only in `CBIC_Rates_2023_Ref` and as a labelled historical hint in `GST_Verification`; it is not evidence.
- No GST rules (`tax_rule_code`) are proposed anywhere.

## 9. Import eligibility

| | Count |
|---|---|
| Import-eligible | 703 (495 PART, 208 SERVICE) |
| Excluded | 30 (all SERVICE: 19 V70 overlaps, 11 packages) |

The 703 are the 733 records minus the 30 exclusions: 19 services whose names are already seeded by migration V70, and 11 garage-specific service packages. Workshop consumables and outsourced services are import-eligible with a marker in the description.

Batch files (limits are 500 rows and 200,000 characters per request): `GarageST_Catalog_Import_Batch_01.csv` 250 rows (48,700 bytes), `_02.csv` 250 rows (47,865 bytes), `_03.csv` 203 rows (32,826 bytes). `GarageST_Catalog_Import_Candidates.csv` holds the same 703 rows in one file for review; it exceeds the per-request row limit and is not for upload. The older `_part1/_part2` files were replaced.

## 10. Decisions needed

1. Obtain the current Central Tax (Rate) notification and have a tax professional confirm code, rate and conditions before any item is verified.
2. Package composition and pricing: model per garage in a future feature (not in the shared catalog).
3. Whether the 36 generic V70 starter names (for example Engine Oil, Oil Filter) should be treated as parents of the specific variants in this catalog (for example Engine Oil - Fully Synthetic); both now coexist.
4. Whether to split the generic items (Gear Set, Sensor Replacement, Electrical Diagnostics).
5. Whether to load candidate codes as `PENDING_REVIEW` (now safe: a malformed code is a per-row error). The files still carry no codes.

## 11. Files

`GarageST_Automotive_Catalog_and_GST_Master.xlsx` (26 sheets, including SAC_Mapping_Rationale and Decisions_Required: README, Summary, Audit_Counts, Official_HSN_Raw, Official_SAC_Raw, Automotive_HSN, Automotive_SAC, Coverage_Matrix, HSN_Review, SAC_Review, Source_Anomalies, Parts_Master, Services_Master, Catalog_Audit_Log, Candidate_Mappings, Verified_Mappings, GST_Verification, Unclassified, Needs_Review, Import_Eligible, Excluded_With_Reasons, Catalog_Import_Candidates, Import_Validation, CBIC_Rates_2023_Ref), `GarageST_Catalog_Import_Candidates.csv`, `GarageST_Catalog_Import_Batch_01..03.csv`, this report, `GarageST_Import_Validation_Report.md`, and in `work/`: `build.py`, `review_block.py`, `audit_data.py`, `data_parts.py`, `data_services.py`, `xlsx_read.py`, `parse_cbic.py`, `run_import.py`, `compare_import.py`, `import_edge_cases.py`, `baseline_v1_items.json`, `build_stats.json`. `HSN_SAC.xlsx` is unchanged.

Re-run: `python work/build.py` regenerates the workbook, CSV and batch files from the data files.
