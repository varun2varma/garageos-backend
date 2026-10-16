# GST and catalog classification status (release gate)

**This document does not assert any tax rate, HSN/SAC code, exemption or effective date.** It records what the
software does, what is verified by tests, and what still needs authoritative human verification. As of
2026-10-16 **no catalog classification is verified**.

## 1. Verified by source and tests (software behaviour, not tax law)

| Fact | Evidence |
|---|---|
| Parts are entered GST-inclusive; labour GST-exclusive | `PriceTaxMode.forType`, `GstCalculatorTest` |
| ₹1,500 inclusive at 18 % -> taxable ₹1,271.19, GST ₹228.81, total ₹1,500.00 | `GstConsistencyTest` |
| Estimate, invoice and PDF use one calculator (`GstCalculator`) with BigDecimal, scale 2, HALF_UP, whole-rupee round-off | `GstCalculator`, `EstimateTotalsCalculator`, `InvoiceTaxAssembler` |
| The Flutter estimate summary shows the backend's subtotal / GST / grand total (no hard-coded GST) | `estimate_summary_screen.dart` |
| An unclassified taxable line is billed at the interim fallback and stored as `CONFIGURED_FALLBACK` (never `CATALOG_RULE`); without a fallback it is refused | `GstConsistencyTest`, `EstimateTaxSupport` |
| `OTHERS` (consumables) are billed with **no GST** by a legacy category rule; source `NONE` | `PriceTaxMode`, `GstConsistencyTest` — **a placeholder, not a verified exemption** |
| Strict mode (`garagest.tax.require-classification=true`) blocks invoice issuance while a taxable line lacks a verified classification and rate. It is evaluated server-side in `InvoiceTaxAssembler`; no request field or client can switch it off | `InvoiceTaxAssembler:51` |

## 2. How production can accidentally permit unverified tax treatment

1. `garagest.tax.fallback-rate-percent` **defaults to 18** (`GARAGEST_TAX_FALLBACK_RATE_PERCENT`).
2. `garagest.tax.require-classification` **defaults to false** (`GARAGEST_TAX_REQUIRE_CLASSIFICATION`).
3. There are **no `gst_tax_rule` rows** and **no VERIFIED catalog items** in a freshly migrated database (verified
   on the disposable test databases: 0 rules, 36 UNCLASSIFIED items).

So, unless an operator sets the two variables, every taxable line is invoiced at an interim 18 % that nobody has
verified. The startup log states this (`[GST] Unclassified taxable lines are billed at the INTERIM configured rate...`),
and `GET /api/v1/catalog/tax-policy` now exposes it to SUPER_ADMIN / OWNER.

## 3. Why the defaults were NOT flipped in this pass (impact assessment)

Setting `GARAGEST_TAX_REQUIRE_CLASSIFICATION=true` (and clearing the fallback) is the correct fail-closed end state,
but with an unclassified catalog it makes **every** invoice with a part or labour line impossible to issue until a
SUPER_ADMIN has created tax rules and verified each item. Changing the default in code would therefore stop all
invoicing on deploy. The decision belongs to the Owner and their accountant. Recommended sequence:

1. SUPER_ADMIN creates the tax rules from official sources (see handbook §27D "Verifying official HSN/SAC and GST rates").
2. Classify and verify the catalog items below (and every manual item reported as `NEEDS_REVIEW`).
3. Run staging with `GARAGEST_TAX_REQUIRE_CLASSIFICATION=true` and issue invoices for each category.
4. Only then set it in production and remove the fallback (`GARAGEST_TAX_FALLBACK_RATE_PERCENT` blank).

## 4. Unresolved classifications (all UNCLASSIFIED; no rate, no HSN/SAC, no rule)

**Parts (16):** Air Filter; Battery; Brake Discs; Brake Fluid; Brake Pads; Cabin Filter; Coolant; Drive Belt;
Engine Oil; Fasteners (Nuts and Bolts); Fuel Filter; Headlamp Bulb; Oil Filter; Spark Plug; Transmission Fluid;
Wiper Blade.

**Services (20):** AC Inspection; AC Servicing; Air Filter Replacement; Battery Replacement; Battery Testing; Brake
Fluid Replacement; Brake Inspection; Brake Pad Replacement; Clutch Inspection; Coolant Replacement; Electrical
Diagnostics; Engine Diagnostics; Engine Oil Replacement; General Periodic Service; Oil Filter Replacement; Road
Test and Inspection; Suspension Inspection; Tyre Rotation; Wheel Alignment; Wheel Balancing.

**Consumables (`OTHERS`):** no catalog items; treatment is the legacy no-GST placeholder and needs a determination.

### Evidence required per item (nothing below may be filled in from memory)

- The HSN (goods) or SAC (services) code confirmed on the official GSTN "Search HSN/SAC" tool, with the date checked.
- The applicable rate confirmed against the current CBIC / GST Council notification (cite the notification number and
  effective date), including any cess or composite-supply/ITC considerations a tax professional identifies.
- For each rate: `gst_tax_rule` code, rate, effective date, source reference and verified-on date.
- A tax professional's sign-off for anything ambiguous (e.g. whether a consumable consumed in a repair is a separate
  supply or part of a composite supply of labour).
- A decision (and sign-off) on how `OTHERS` lines must be priced (inclusive / exclusive) and taxed.
- Place-of-supply handling, customer GSTIN, reverse-charge statement and delivery address are not captured on the
  invoice (handbook §27D) and need the accountant's review before an invoice is relied on as a tax invoice.

## 5. Status

GST readiness: **not claimed**. The calculation mechanics are tested; the data they depend on is unverified.
