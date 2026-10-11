# -*- coding: utf-8 -*-
"""Builds GarageST_Automotive_Catalog_and_GST_Master.xlsx and the import CSV.
Reads ONLY: the downloaded official HSN_SAC.xlsx, the saved CBIC page (parsed JSON), the V70 migration (to detect
existing starter names) and the two research data files. No code or description is typed into the output by hand:
every candidate code is looked up in the official file and rejected if it is not there."""
import sys, re, json, csv, hashlib, os, collections
sys.path.insert(0, os.path.dirname(__file__))
import xlsxwriter
from xlsx_read import read
import data_parts, data_services

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
OUT_XLSX = os.path.join(ROOT, 'GarageST_Automotive_Catalog_and_GST_Master.xlsx')
OUT_CSV = os.path.join(ROOT, 'GarageST_Catalog_Import_Candidates.csv')
SRC_FILE = os.path.join(ROOT, 'HSN_SAC.xlsx')
SRC_URL = 'https://tutorial.gst.gov.in/downloads/HSN_SAC.xlsx'
PORTAL_URL = 'https://services.gst.gov.in/services/searchhsnsac'
RETRIEVED = '2026-10-10'
ALLOWED_UNITS = {'NOS', 'SET', 'PAIR', 'LTR', 'ML', 'KG', 'GM', 'MTR', 'KIT', 'BOX', 'PKT', 'ROLL', 'JOB', 'HR'}
STATUS = {'C': 'CANDIDATE', 'NR': 'NEEDS_REVIEW', 'U': 'UNCLASSIFIED'}

sha = hashlib.sha256(open(SRC_FILE, 'rb').read()).hexdigest()
size = os.path.getsize(SRC_FILE)
book = read(SRC_FILE)
HSN = book['HSN_MSTR']; SAC = book['SAC_MSTR']
hsn_map = {r[0]: r[1] for r in HSN[1:]}
sac_map = {r[0]: r[1] for r in SAC[1:]}
cbic = json.load(open(os.path.join(os.path.dirname(__file__), 'cbic_rates_parsed.json'), encoding='utf-8'))
cbic_goods = cbic['noid0']['rows']; cbic_services = cbic['noid1']['rows']

# ----------------------------------------------------------------------------- existing V70 starter names
v70 = open(os.path.join(ROOT, '..', '..', 'src', 'main', 'resources', 'db', 'migration',
                        'V70__catalog_management_and_starter_items.sql'), encoding='utf-8').read()
starter = {(m.group(1), re.sub(r'\s+', ' ', m.group(2)).strip().lower())
           for m in re.finditer(r"\('(PART|SERVICE)',\s*'([^']+)'", v70)}

def norm(s):
    return re.sub(r'\s+', ' ', s.strip().lower())

def lookup(code):
    """Official description for a candidate code, or None when the code is not in the official file."""
    if code in hsn_map: return ('HSN', hsn_map[code])
    if code in sac_map: return ('SAC', sac_map[code])
    return None

# ----------------------------------------------------------------------------- parse research data
def parse(text, kind):
    items, sec = [], None
    for raw in text.strip().splitlines():
        line = raw.strip()
        if not line: continue
        if line.startswith('##'):
            f = [x.strip() for x in line[2:].split('|')]
            if kind == 'P':
                sec = dict(cat=f[0], sub=f[1], sysfam=f[2], cls=f[3], unit=f[4],
                           codes=[c for c in f[5].split(',') if c], st=f[6], note=f[7])
            else:
                sec = dict(cat=f[0], sub=f[1], cls=f[2], unit=f[3],
                           codes=[c for c in f[4].split(',') if c], st=f[5], note=f[6])
            continue
        f = [x.strip() for x in line.split('|')]
        ov = {}
        if kind == 'P':
            name, aliases, desc, attrs = f[0], f[1], f[2], f[3]
            ovs = f[4] if len(f) > 4 else ''
        else:
            name, aliases, desc = f[0], f[1], f[2]
            attrs = ''
            ovs = f[3] if len(f) > 3 else ''
        for part in [p for p in ovs.split('&') if p]:
            k, _, v = part.partition('=')
            ov[k.strip()] = v.strip()
        codes = [c for c in ov['codes'].split(',') if c] if 'codes' in ov else list(sec['codes'])
        st = ov.get('st', sec['st'] if codes else 'U')
        if not codes: st = 'U'
        items.append(dict(
            name=name, aliases=[a.strip() for a in aliases.split(',') if a.strip()], desc=desc, attrs=attrs,
            cat=sec['cat'], sub=sec['sub'], sysfam=sec.get('sysfam', ''), cls=ov.get('cls', sec['cls']),
            unit=ov.get('unit', sec['unit']), codes=codes, st=STATUS[st], note=ov.get('note', sec['note'])))
    return items

parts = parse(data_parts.PARTS, 'P')
services = parse(data_services.SERVICES, 'S')
PREFIX = {'OUTSOURCED_SERVICE': 'Commonly outsourced to an external workshop (marked as outsourced work). ',
          'WORKSHOP_CONSUMABLE': 'Workshop consumable (inventory-tracked, billing treatment chosen by the garage). ',
          'EV_SPECIALIST': 'Specialist high-voltage component. '}
for _it in parts + services:
    _p = PREFIX.get(_it['cls'], '')
    if _it['cat'] == 'Electric Vehicle Services': _p = 'Specialist EV work requiring trained personnel and equipment. '
    if _p and not _it['desc'].startswith(_p): _it['desc'] = _p + _it['desc']

def enrich(it, kind):
    d = it['desc'].rstrip('.')
    if kind == 'P':
        ctx = '%s item in the %s category' % (it['sub'], it['cat'])
        if it['sysfam']: ctx += ' (%s system)' % it['sysfam']
        if it['attrs']: ctx += '. Variants are distinguished by: %s' % it['attrs']
    else:
        ctx = 'Labour operation in %s / %s; priced per %s' % (it['cat'], it['sub'], 'job' if it['unit'] == 'JOB' else 'hour')
    it['desc'] = ('%s. %s.' % (d, ctx))[:500]
for it in parts: enrich(it, 'P')
for it in services: enrich(it, 'S')
for i, it in enumerate(parts, 1): it['id'] = 'PRT-%04d' % i; it['type'] = 'PART'
for i, it in enumerate(services, 1): it['id'] = 'SVC-%04d' % i; it['type'] = 'SERVICE'
allitems = parts + services

# ----------------------------------------------------------------------------- validate codes against the official file
problems = []
for it in allitems:
    it['code_info'] = []
    for c in it['codes']:
        info = lookup(c)
        if info is None:
            problems.append('%s %s: code %s is not in the official file' % (it['id'], it['name'], c))
            continue
        kind, odesc = info
        if it['type'] == 'PART' and (kind != 'HSN' or c.startswith('99')):
            problems.append('%s %s: part mapped to a service code %s' % (it['id'], it['name'], c))
        if it['type'] == 'SERVICE' and kind != 'SAC':
            problems.append('%s %s: service mapped to a goods code %s' % (it['id'], it['name'], c))
        it['code_info'].append((c, kind, odesc))
    # an item keeps only codes that exist in the official list
    it['codes'] = [c for c, _, _ in it['code_info']]
    if not it['codes'] and it['st'] != 'UNCLASSIFIED':
        it['st'] = 'UNCLASSIFIED'
if problems:
    print('CODE PROBLEMS (items fall back to the codes that do exist):'); print('\n'.join(problems))

# ----------------------------------------------------------------------------- de-duplication checks
dups, alias_issues = [], []
seen = {}
for it in allitems:
    k = (it['type'], norm(it['name']))
    if k in seen: dups.append(('DUPLICATE NAME', it['id'], seen[k], it['name']))
    seen[k] = it['id']
names_by_type = collections.defaultdict(dict)
for it in allitems: names_by_type[it['type']][norm(it['name'])] = it['id']
alias_owner = {}
for it in allitems:
    for a in it['aliases']:
        k = (it['type'], norm(a))
        if k in names_by_type[it['type']].items() or norm(a) in names_by_type[it['type']]:
            if names_by_type[it['type']][norm(a)] != it['id']:
                alias_issues.append(('ALIAS EQUALS ANOTHER ITEM NAME', it['id'], names_by_type[it['type']][norm(a)], a))
        if k in alias_owner and alias_owner[k] != it['id']:
            alias_issues.append(('ALIAS SHARED', it['id'], alias_owner[k], a))
        alias_owner[k] = it['id']
print('ALIAS ISSUES:', alias_issues)
print('items:', len(parts), 'parts,', len(services), 'services; dup names', len(dups), 'alias issues', len(alias_issues))

# ----------------------------------------------------------------------------- historical CBIC schedule hits (reference only)
def hist_goods(code4):
    hits = []
    for r in cbic_goods:
        if len(r) < 7: continue
        heading = re.sub(r'\s', '', r[2])
        if re.search(r'(?<!\d)%s' % code4, heading):
            hits.append('[Sch %s #%s] CGST %s / IGST %s' % (r[0], r[1], r[4] or '-', r[6] or '-'))
    return hits[:6]
def hist_service(code):
    hits = []
    for r in cbic_services:
        if len(r) < 6: continue
        if code[:4] in r[1] or code in r[1]:
            hits.append('[#%s] CGST %s / IGST %s' % (r[0], r[3][:20], r[5][:20]))
    return hits[:6]

# ----------------------------------------------------------------------------- automotive HSN / SAC selection (rule based)
HSN_HEADINGS = {
 '2710': 'Petroleum oils incl. lubricating oils (engine, gear oils)', '2903': 'Halogenated hydrocarbons (refrigerant gases)',
 '3102': 'Mineral or chemical nitrogenous fertilisers (urea; relevant to diesel exhaust fluid)',
 '3204': 'Colouring matter / dyes (A/C leak-detection dye)', '3208': 'Paints and varnishes (polymer based)',
 '3209': 'Paints and varnishes (aqueous)', '3210': 'Other paints and varnishes', '3214': 'Putty, mastics, sealants',
 '3402': 'Washing and cleaning preparations', '3403': 'Lubricating preparations, greases', '3405': 'Polishes and creams (coachwork)',
 '3506': 'Prepared glues and adhesives', '3811': 'Fuel and oil additives, anti-corrosive preparations', '3814': 'Solvents, thinners',
 '3819': 'Hydraulic brake fluids', '3820': 'Anti-freezing preparations (coolant)', '3824': 'Chemical preparations (refrigerant blends)',
 '4006': 'Rubber repair articles (tyre patches)', '4009': 'Rubber tubes, pipes, hoses', '4010': 'Rubber transmission belts',
 '4011': 'New pneumatic tyres', '4012': 'Retreaded or used tyres, flaps', '4013': 'Inner tubes', '4016': 'Other rubber articles (seals, mats)',
 '5703': 'Carpets (car mats)', '6307': 'Cleaning cloths', '6304': 'Furnishing articles (seat covers)', '6813': 'Friction material (brake linings, pads)',
 '7007': 'Safety glass', '7009': 'Glass mirrors incl. rear-view mirrors', '7306': 'Steel tubes and pipes (brake lines)',
 '7210': '', '7312': 'Steel ropes and cables (control cables)', '7315': 'Chain and parts', '7318': 'Screws, bolts, nuts, washers, rivets', '7320': 'Springs',
 '8301': 'Locks (vehicle locks)', '8302': 'Mountings and fittings for motor vehicles', '8308': 'Clasps, hose clamps and similar',
 '8407': 'Spark-ignition engines', '8408': 'Compression-ignition engines', '8409': 'Parts of engines',
 '8412': 'Other engines and motors', '8413': 'Pumps for liquids (fuel, oil, water pumps)', '8414': 'Air pumps, compressors, fans',
 '8415': 'Air-conditioning machines', '8419': 'Heat exchange units', '8421': 'Filtering machinery (oil, fuel, air filters, catalytic converters)',
 '8481': 'Taps, valves', '8482': 'Ball and roller bearings', '8483': 'Transmission shafts, gears, clutches, pulleys',
 '8484': 'Gaskets and mechanical seals', '8501': 'Electric motors and generators', '8504': 'Transformers, static converters',
 '8507': 'Electric accumulators (batteries)', '8511': 'Ignition and starting equipment', '8512': 'Lighting, signalling, wipers',
 '8527': 'Radio receivers (car audio)', '8531': 'Electric signalling apparatus', '8533': 'Electrical resistors', '8536': 'Switches, relays, fuses, connectors',
 '8537': 'Control panels and modules', '8538': 'Parts of switchgear (see source-data note)', '8539': 'Lamps (see source-data note)',
 '8544': 'Insulated wire, cable, wiring sets for vehicles', '8706': 'Chassis fitted with engines', '8707': 'Bodies for motor vehicles',
 '8708': 'Parts and accessories of motor vehicles', '8711': 'Motorcycles (context only)', '8714': 'Parts of motorcycles and cycles',
 '8716': 'Trailers and parts', '9025': 'Thermometers, pyrometers', '9026': 'Instruments for flow, level, pressure', '9029': 'Speed indicators, tachometers',
 '9031': 'Measuring or checking instruments n.e.c.', '9032': 'Automatic regulating instruments (thermostats)', '9401': 'Seats (vehicle seats)', '9603': 'Brushes',
}
HSN_HEADINGS = {k: v for k, v in HSN_HEADINGS.items() if v}
import audit_data as A
HSN_HEADINGS.update(A.ADD_HEADINGS)
DIRECT = re.compile(r'motor vehicle|vehicle|engine|tyre|brake|clutch|steering|spark|wiper|ignition|starting|motor cars|motor cycles|motorcycle|lorries|coachwork|cycles', re.I)
auto_hsn = []
for code, desc in HSN[1:]:
    h = code[:4]
    if h in HSN_HEADINGS:
        direct = bool(DIRECT.search(desc))
        note = ''
        if code in ('8538', '8539'):
            note = ('SOURCE DATA NOTE: in the official file the heading-level description of %s shows the text of the preceding heading; '
                    'the sub-entries below it are consistent with the tariff. Preserved unchanged in Official_HSN_Raw.' % code)
        auto_hsn.append([code, desc, HSN_HEADINGS[h],
                         'Description names vehicle/engine use' if direct else 'Heading-level relevance only: confirm per product',
                         note, SRC_URL, 'HSN_MSTR'])
SAC_SEL = ['9987', '998714', '998719', '998729', '998736', '998739', '998538', '998539', '998346', '998881', '998882', '998889',
           '996744', '996749', '996511', '999719', '998597']
auto_sac = []
why_sac = {'9987': 'Group: maintenance, repair and installation (except construction) services',
 '998714': 'Maintenance and repair services of transport machinery and equipment (vehicle servicing and repair)',
 '998719': 'Other machinery and equipment (residual)', '998729': 'Other goods n.e.c. (residual)',
 '998736': 'Installation of electrical machinery and apparatus (accessory fitting)', '998739': 'Installation of other goods (accessory fitting)',
 '998538': 'Cleaning of transportation equipment (vehicle washing)', '998539': 'Other cleaning services n.e.c.',
 '998346': 'Technical testing and analysis services (inspection / diagnostics candidate)',
 '998881': 'Motor vehicle and trailer manufacturing services (job work; body/paint candidate)', '998882': 'Other transport equipment manufacturing services',
 '998889': 'Transport equipment manufacturing services, other', '996744': 'Towing services for commercial and private vehicles',
 '996749': 'Other supporting services for road transport n.e.c.', '996511': 'Road transport of goods (vehicle carrier candidate)',
 '999719': 'Other washing, cleaning and dyeing services n.e.c.', '998597': 'Landscape care (not used; listed for completeness)'}
why_sac.update(A.SAC_ADD)
SAC_SEL = SAC_SEL + [c for c in A.SAC_ADD if c not in SAC_SEL]
for code in SAC_SEL:
    if code in sac_map and code != '998597':
        auto_sac.append([code, sac_map[code], why_sac[code], 'OFFICIAL ENTRY (SAC_MSTR)', SRC_URL])

# which official rows are actually referenced
used = collections.Counter()
for it in allitems:
    for c in it['codes']: used[c] += 1
missing_in_auto = [c for c in used if (c in hsn_map and c[:4] not in HSN_HEADINGS)]
if missing_in_auto: print('Referenced HSN codes outside the relevance list:', missing_in_auto)
sac_missing = [c for c in used if c in sac_map and c not in SAC_SEL]
if sac_missing: print('Referenced SAC codes outside the SAC list:', sac_missing)

# ----------------------------------------------------------------------------- import candidates
IMPORTABLE_PART = {'PART', 'FLUID_LUBRICANT', 'DETAILING_PRODUCT', 'EV_SPECIALIST', 'WORKSHOP_CONSUMABLE'}
IMPORTABLE_SERVICE = {'LABOUR_OPERATION', 'OUTSOURCED_SERVICE'}
imp, excluded = [], []
for it in allitems:
    ok = it['cls'] in (IMPORTABLE_PART if it['type'] == 'PART' else IMPORTABLE_SERVICE)
    reason = ''
    if not ok:
        reason = {'SERVICE_PACKAGE': 'Garage-specific: package composition and pricing belong to each garage; the underlying operations are separate catalog services'}.get(it['cls'], 'Class not imported')
    elif (it['type'], norm(it['name'])) in starter:
        ok = False; reason = 'Already seeded by migration V70 (name match)'
    elif len(it['name']) > 150 or len(it['desc']) > 500 or len(it['cat']) > 60 or it['unit'] not in ALLOWED_UNITS:
        ok = False; reason = 'Fails import field validation'
    it['import'] = ok; it['import_reason'] = reason
    (imp if ok else excluded).append(it)

with open(OUT_CSV, 'w', newline='', encoding='utf-8') as f:
    w = csv.writer(f)
    w.writerow(['type', 'name', 'category', 'default_unit', 'description'])
    for it in imp:
        w.writerow([it['type'], it['name'], it['cat'], it['unit'], it['desc'][:500]])
# API-limit-respecting batches (max 500 rows and 200,000 characters of CSV text per request)
import glob, io
for old_f in glob.glob(os.path.join(ROOT, 'GarageST_Catalog_Import_Candidates_part*.csv')) + glob.glob(os.path.join(ROOT, 'GarageST_Catalog_Import_Batch_*.csv')):
    os.remove(old_f)
BATCH_ROWS = 250   # well inside the 500-row limit; also keeps each request small
batches = [imp[i:i + BATCH_ROWS] for i in range(0, len(imp), BATCH_ROWS)]
batch_files = []
for n, chunk in enumerate(batches, 1):
    buf = io.StringIO(newline='')
    w = csv.writer(buf, lineterminator='\n')
    w.writerow(['type', 'name', 'category', 'default_unit', 'description'])
    for it in chunk: w.writerow([it['type'], it['name'], it['cat'], it['unit'], it['desc'][:500]])
    text = buf.getvalue()
    assert len(chunk) <= 500 and len(text) <= 200000, 'batch exceeds API limits'
    fn = 'GarageST_Catalog_Import_Batch_%02d.csv' % n
    with open(os.path.join(ROOT, fn), 'w', newline='', encoding='utf-8') as f: f.write(text)
    batch_files.append((fn, len(chunk), len(text)))

# ----------------------------------------------------------------------------- workbook
wb = xlsxwriter.Workbook(OUT_XLSX, {'strings_to_urls': False, 'constant_memory': False})
H = wb.add_format({'bold': True, 'bg_color': '#1F3864', 'font_color': 'white', 'text_wrap': True, 'valign': 'top', 'border': 1})
W = wb.add_format({'text_wrap': True, 'valign': 'top'})
B = wb.add_format({'bold': True})
T = wb.add_format({'bold': True, 'font_size': 14})
RED = wb.add_format({'text_wrap': True, 'valign': 'top', 'font_color': '#9C0006', 'bg_color': '#FFC7CE'})
YEL = wb.add_format({'text_wrap': True, 'valign': 'top', 'bg_color': '#FFF2CC'})

def table(ws, headers, rows, widths, freeze=1, filt=True, wrap=True):
    for c, h in enumerate(headers): ws.write(0, c, h, H)
    for r, row in enumerate(rows, 1):
        for c, v in enumerate(row):
            ws.write(r, c, v, W if wrap else None)
    for c, wd in enumerate(widths): ws.set_column(c, c, wd)
    ws.freeze_panes(freeze, 0)
    if filt and rows: ws.autofilter(0, 0, len(rows), len(headers) - 1)

def codes_cols(it):
    first = it['code_info'][0] if it['code_info'] else None
    alt = '; '.join('%s' % c for c, _, _ in it['code_info'][1:])
    return first, alt

# README --------------------------------------------------------------------------------------
ws = wb.add_worksheet('README'); ws.set_column(0, 0, 34); ws.set_column(1, 1, 120)
rows = [
 ('Title', 'GarageST Automotive Catalog and GST Master (research and data preparation; NOT imported anywhere)'),
 ('Status', 'DRAFT FOR REVIEW. Nothing here is verified. No GST rate is stated. No record is marked VERIFIED.'),
 ('Purpose', 'A broad, OEM-neutral automotive parts and workshop-services catalog, mapped to CANDIDATE official HSN/SAC entries taken from the downloaded official GST list, ready for review by an accountant or tax professional before any import.'),
 ('What is official', 'Sheets Official_HSN_Raw and Official_SAC_Raw are the GST Portal HSN/SAC directory exactly as downloaded (values unchanged). Descriptions in Automotive_HSN, Automotive_SAC and the "Official description" columns are copied from that file by script; candidate codes that are not in the file are rejected.'),
 ('What is research (inference)', 'The item taxonomy, names, aliases, descriptions, units, attributes, the choice of relevant headings, and every mapping from an item to a candidate code are GarageST research and inference, not GST law. A candidate is a starting point for review.'),
 ('Primary official source', 'GST Portal "Download HSN Directory in Excel Format". URL: %s (linked from %s). Retrieved %s. File HSN_SAC.xlsx, %d bytes, SHA-256 %s. HSN_MSTR: %d rows; SAC_MSTR: %d rows. The file contains codes and descriptions ONLY (no GST rate, no UQC, no effective dates).' % (SRC_URL, PORTAL_URL, RETRIEVED, size, sha, len(HSN) - 1, len(SAC) - 1)),
 ('Other official sources read', 'GSTN advisory on Search HSN (tutorial.gst.gov.in .../advisory_on_search_hsn_code_functionality_final.pdf): the portal search is a facilitation tool, results are not legally binding and are based on e-invoice usage. GSTN advisory on HSN validation in GSTR-1 Table 12 (.../updated_advisory_on_hsn_validation_21.01.25.pdf): 4-digit (AATO up to Rs 5 crore) or 6-digit (above) HSN is mandatory. CBIC GST rates page (cbic-gst.gov.in/hindi/gst-goods-services-rates.html).'),
 ('GST RATES - LIMITATION', 'The CBIC rates page embeds a goods and services rate schedule headed "as on 01.04.2023" and the English CBIC page links 2017 schedules. These pre-date later rate changes, so they are NOT used as evidence. The current Central Tax (Rate) notification text (the portal lists, for example, notification 01/2026-Central Tax (Rate) dated 30-Apr-2026 amending notification 9/2025-Central Tax (Rate)) could not be downloaded through any public route that was tried. Therefore GST_Verification has NO rates and every record is NOT_VERIFIED. Sheet CBIC_Rates_2023_Ref is kept for historical reference only.'),
 ('Origin column', 'Earlier research pass = present in the first release of this workbook. Coverage-audit addition (research-derived) = added in the coverage audit because a common part or service was missing. Both are research inference, not official data.'),
 ('Statuses', 'UNCLASSIFIED: no supportable official entry found. CANDIDATE: an official entry names this class of product or service (still requires review). NEEDS_REVIEW: more than one plausible entry, or applicability depends on facts not captured. VERIFIED: not used.'),
 ('Record classes and shared-catalog scope', 'Shared catalog (type PART or SERVICE only; the model has no class field, so the class is carried by category and description): parts and fluids; workshop consumables (PART, category Workshop Consumables, description marked as consumable); outsourced services (SERVICE, description marked as commonly outsourced); specialist EV services and components (description marked as specialist); labour operations. NOT in the shared catalog: service packages (11) because package composition and pricing are garage-specific; selling prices, purchase costs, suppliers, stock and garage tax configuration are never catalog fields. Underlying operations of each package are separate catalog services.'),
 ('Sheets', 'RAW OFFICIAL: Official_HSN_Raw, Official_SAC_Raw. CURATED OFFICIAL AUTOMOTIVE: Automotive_HSN, Automotive_SAC. COVERAGE AUDIT: Coverage_Matrix, HSN_Review (every 4-digit heading), SAC_Review (every SAC row), Source_Anomalies. CATALOG: Parts_Master, Services_Master, Catalog_Audit_Log. MAPPINGS: Candidate_Mappings, Verified_Mappings (empty by design), GST_Verification. QUEUES: Unclassified, Needs_Review, Import_Eligible, Excluded_With_Reasons. COUNTS: Summary, Audit_Counts, Import_Validation. HISTORICAL ONLY: CBIC_Rates_2023_Ref.'),
 ('Import contract (verified in code)', 'POST /api/v1/catalog/import (SUPER_ADMIN only) with {"csv": "...", "dryRun": true|false}. Header type,name,category,default_unit,description[,hsn_sac_code,tax_rule_code]. Units: NOS SET PAIR LTR ML KG GM MTR KIT BOX PKT ROLL JOB HR. Name <=150, category <=60, description <=500, max 500 rows per request. It only ADDS: an existing (type, normalised name) is reported as a duplicate and never changed; a supplied code can only become PENDING_REVIEW, never VERIFIED. This workbook leaves hsn_sac_code and tax_rule_code blank for every row.'),
 ('How to import reviewed records', '1) Review Parts_Master / Services_Master / Needs_Review. 2) Run GarageST_Catalog_Import_Candidates.csv through the import API with dryRun=true and read the report. 3) Re-run with dryRun=false in a NON-production database first. 4) Classification is done afterwards in the app (Master data) or POST /catalog/items/{id}/classification by a SUPER_ADMIN once an accountant has confirmed the code, the rate and its source.'),
 ('Verification workflow', 'For each item: confirm the product description and the exact HSN/SAC with the GST Portal Search HSN tool and the Customs Tariff text; obtain the applicable Central Tax (Rate) notification and conditions in force on the supply date; record CGST, SGST/UTGST, IGST, conditions and effective dates in GST_Verification; create the GST rule with its source in the app; only then verify the item.'),
 ('Completeness', 'This is a comprehensive working catalog of common automotive parts and workshop services, NOT a complete official tariff and not exhaustive of every part. The official HSN/SAC list was downloaded in full and preserved. Every four-digit HSN heading (1,367) and every SAC row (681) was given a recorded review decision (HSN_Review, SAC_Review, Coverage_Matrix). The review is heading-level and wording-assisted; 8-digit entries inside non-selected headings were not individually read, and entries flagged FLAGGED_FURTHER_REVIEW still need product-level research. Coverage of the official list is therefore reviewed, not proven exhaustive.'),
]
for r, (k, v) in enumerate(rows):
    ws.write(r, 0, k, B); ws.write(r, 1, v, W)

def unescape(v):
    # The source XML stores control characters as _xHHHH_ escapes; write the real character so the cell value is identical.
    return re.sub(r'_x([0-9A-Fa-f]{4})_', lambda m: chr(int(m.group(1), 16)), v)

# Official raw --------------------------------------------------------------------------------
ws = wb.add_worksheet('Official_HSN_Raw')
for c, h in enumerate(HSN[0]): ws.write(0, c, h, H)
for r, row in enumerate(HSN[1:], 1):
    ws.write_string(r, 0, unescape(row[0])); ws.write_string(r, 1, unescape(row[1]))
ws.set_column(0, 0, 14); ws.set_column(1, 1, 130); ws.freeze_panes(1, 0); ws.autofilter(0, 0, len(HSN) - 1, 1)
ws = wb.add_worksheet('Official_SAC_Raw')
for c, h in enumerate(SAC[0]): ws.write(0, c, h, H)
for r, row in enumerate(SAC[1:], 1):
    ws.write_string(r, 0, unescape(row[0])); ws.write_string(r, 1, unescape(row[1]))
ws.set_column(0, 0, 14); ws.set_column(1, 1, 130); ws.freeze_panes(1, 0); ws.autofilter(0, 0, len(SAC) - 1, 1)

# Automotive HSN / SAC ------------------------------------------------------------------------
ws = wb.add_worksheet('Automotive_HSN')
table(ws, ['HSN code (original)', 'Official description (original)', 'Why this heading was selected (research)', 'Relevance level',
           'Source data note', 'Source URL', 'Source sheet', 'Items mapped (count)'],
      [r + [used.get(r[0], 0)] for r in auto_hsn], [14, 80, 46, 36, 50, 40, 12, 12])
ws = wb.add_worksheet('Automotive_SAC')
table(ws, ['SAC code (original)', 'Official description (original)', 'Why selected (research)', 'Entry type', 'Source URL', 'Items mapped (count)'],
      [r + [used.get(r[0], 0)] for r in auto_sac], [14, 80, 60, 26, 40, 12])

for it in allitems: it['origin'] = 'Earlier research pass' if ('%s|%s' % (it['type'], it['name'])) in json.load(open(os.path.join(os.path.dirname(__file__), 'baseline_v1_items.json'), encoding='utf-8')) else 'Coverage-audit addition (research-derived)'
# Masters -------------------------------------------------------------------------------------
def master_rows(items, kind):
    rows = []
    for it in items:
        first, alt = codes_cols(it)
        starter_flag = 'Yes' if (it['type'], norm(it['name'])) in starter else ''
        if kind == 'P':
            rows.append([it['id'], it['type'], it['cls'], it['cat'], it['sub'], it['name'], '; '.join(it['aliases']), it['desc'], it['unit'],
                         it['attrs'], it['sysfam'], first[0] if first else '', first[2] if first else '', alt,
                         SRC_URL if first else '', it['st'], 'NOT_VERIFIED', it['note'], starter_flag,
                         'Yes' if it['import'] else 'No', it['import_reason'], it['origin']])
        else:
            rows.append([it['id'], it['type'], it['cls'], it['cat'], it['sub'], it['name'], '; '.join(it['aliases']), it['desc'], it['unit'],
                         first[0] if first else '', first[2] if first else '', alt, SRC_URL if first else '', it['st'], 'NOT_VERIFIED',
                         it['note'], starter_flag, 'Yes' if it['import'] else 'No', it['import_reason'], it['origin']])
    return rows
ws = wb.add_worksheet('Parts_Master')
table(ws, ['Ref ID (workbook only)', 'Type', 'Record class', 'Category', 'Subcategory', 'Standard item name', 'Aliases / search synonyms', 'Technical description',
           'Default unit', 'Generic attributes (variant distinguishers)', 'Vehicle / system family', 'Candidate HSN (first)', 'Official description of candidate',
           'Alternate candidate codes', 'Source URL', 'Classification status', 'Verification status', 'Review notes', 'In V70 starter', 'In import CSV', 'Import exclusion reason', 'Origin'],
      master_rows(parts, 'P'), [11, 8, 18, 22, 24, 30, 34, 44, 8, 40, 16, 12, 50, 22, 28, 15, 15, 60, 9, 9, 40, 30])
ws.data_validation(1, 15, len(parts), 15, {'validate': 'list', 'source': ['UNCLASSIFIED', 'CANDIDATE', 'NEEDS_REVIEW', 'VERIFIED']})
ws = wb.add_worksheet('Services_Master')
table(ws, ['Ref ID (workbook only)', 'Type', 'Record class', 'Category', 'Subcategory', 'Standard service name', 'Aliases / search synonyms', 'Work description and scope',
           'Default labour unit', 'Candidate SAC (first)', 'Official description of candidate', 'Alternate candidate codes', 'Source URL', 'Classification status',
           'Verification status', 'Review notes', 'In V70 starter', 'In import CSV', 'Import exclusion reason', 'Origin'],
      master_rows(services, 'S'), [11, 9, 20, 22, 22, 32, 34, 40, 9, 12, 50, 22, 28, 15, 15, 60, 9, 9, 40, 30])
ws.data_validation(1, 13, len(services), 13, {'validate': 'list', 'source': ['UNCLASSIFIED', 'CANDIDATE', 'NEEDS_REVIEW', 'VERIFIED']})

# GST verification ----------------------------------------------------------------------------
gv = []
for it in allitems:
    if not it['code_info']:
        gv.append([it['id'], it['name'], '', '', '', '', '', '', '', '', '', '', SRC_URL, '', 'NOT_VERIFIED', 'No supportable official entry found; ' + it['note']])
        continue
    for c, kind, odesc in it['code_info']:
        hist = (hist_goods(c[:4]) if kind == 'HSN' else hist_service(c))
        gv.append([it['id'], it['name'], c, kind, odesc,
                   'NOT RETRIEVED: current Central Tax (Rate) notification text (e.g. 9/2025 as amended) could not be downloaded',
                   '', '', '', '', '', '', SRC_URL, RETRIEVED + ' (classification source retrieved; no rate evidence)', 'NOT_VERIFIED',
                   ('Candidate only. ' + it['note'] + (' | HISTORICAL_ONLY (CBIC page as on 01.04.2023, superseded, NOT evidence): ' + ' ; '.join(hist) if hist else '')).strip()])
ws = wb.add_worksheet('GST_Verification')
table(ws, ['Item ref ID', 'Item name', 'Candidate HSN/SAC', 'Type', 'Official tariff/service description', 'Rate notification / schedule reference',
           'CGST rate %', 'SGST/UTGST rate %', 'IGST rate %', 'Conditions / exceptions', 'Effective from', 'Effective to', 'Source URL', 'Verification date',
           'Verification status', 'Reviewer notes'], gv, [11, 30, 12, 7, 50, 44, 9, 10, 9, 28, 11, 11, 28, 24, 15, 80])
ws.data_validation(1, 14, len(gv), 14, {'validate': 'list', 'source': ['NOT_VERIFIED', 'IN_REVIEW', 'VERIFIED', 'REJECTED']})

# Needs review --------------------------------------------------------------------------------
nr = []
for it in allitems:
    if it['st'] == 'CANDIDATE': continue
    codes = ', '.join(it['codes'])
    if it['st'] == 'UNCLASSIFIED':
        q = 'Which official HSN/SAC entry, if any, applies to this item (material, composition, use)? None was supportable from the official list.'
    elif len(it['codes']) > 1:
        q = 'Which of the candidate entries (%s) applies, given the item attributes, and what GST rate and conditions apply on the supply date?' % codes
    else:
        q = 'Does candidate %s describe this item exactly, and what GST rate and conditions apply on the supply date?' % codes
    nr.append([it['id'], it['type'], it['name'], it['cat'], it['st'], codes, it['note'], q, 'Tax professional' if it['type'] == 'SERVICE' or it['st'] == 'UNCLASSIFIED' else 'Tax professional / product specialist'])
nr = [['GEN-1', 'ALL', 'GST rates for every record', '', 'BLOCKER', '', 'Current Central Tax (Rate) notification text not retrieved; CBIC page is as on 01.04.2023.',
       'Provide the applicable rate notification, rate and conditions in force for each classification.', 'Accountant']] + \
     [['GEN-2', 'SERVICE', 'Service packages and periodic services combining parts and labour', '', 'DECISION', '', 'Composite versus mixed supply.',
       'How should bundled service packages and parts supplied with labour be classified and taxed?', 'Tax professional']] + nr
ws = wb.add_worksheet('Needs_Review')
table(ws, ['Ref ID', 'Type', 'Item', 'Category', 'Status', 'Candidate codes', 'Reason (evidence missing / ambiguity)', 'Exact question to resolve', 'Suggested reviewer'],
      nr, [9, 9, 34, 22, 14, 22, 70, 70, 24])

# Import candidates ---------------------------------------------------------------------------
ws = wb.add_worksheet('Catalog_Import_Candidates')
table(ws, ['type', 'name', 'category', 'default_unit', 'description'],
      [[it['type'], it['name'], it['cat'], it['unit'], it['desc'][:500]] for it in imp], [10, 44, 28, 12, 80])
ws.write(len(imp) + 2, 0, 'hsn_sac_code and tax_rule_code are intentionally NOT included: no classification is verified. Same content as GarageST_Catalog_Import_Candidates.csv.', B)

# Import validation ---------------------------------------------------------------------------
ws = wb.add_worksheet('Import_Validation'); ws.set_column(0, 0, 46); ws.set_column(1, 4, 24); ws.set_column(5, 5, 60)
r = 0
def line(*vals, fmt=None):
    global r
    for c, v in enumerate(vals): ws.write(r, c, v, fmt)
    r += 1
line('Check', 'Result', fmt=H)
checks = [
 ('Parts / services in workbook', '%d / %d' % (len(parts), len(services))),
 ('Rows in import CSV', str(len(imp))),
 ('Excluded from import', str(len(excluded))),
 ('Duplicate names within a type (workbook)', str(len(dups))),
 ('Alias problems (alias = other item name / shared alias)', str(len(alias_issues))),
 ('Names > 150 chars', str(sum(1 for i in imp if len(i['name']) > 150))),
 ('Descriptions > 500 chars', str(sum(1 for i in imp if len(i['desc']) > 500))),
 ('Categories > 60 chars', str(sum(1 for i in imp if len(i['cat']) > 60))),
 ('Invalid units (all items)', str(sum(1 for i in allitems if i['unit'] not in ALLOWED_UNITS))),
 ('Missing required fields in import rows', str(sum(1 for i in imp if not (i['name'] and i['cat'] and i['unit'] and i['desc'])))),
 ('Candidate codes NOT found in official file (rejected)', str(len(problems))),
 ('Parts mapped to a service-type code / services to goods code', str(sum(1 for p in problems if 'mapped to' in p))),
 ('Items overlapping the 36 V70 starter names (excluded from CSV)', str(sum(1 for i in allitems if (i['type'], norm(i['name'])) in starter))),
 ('Official HSN rows downloaded / selected as automotive-relevant', '%d / %d' % (len(HSN) - 1, len(auto_hsn))),
 ('Official SAC rows downloaded / selected as automotive-relevant', '%d / %d' % (len(SAC) - 1, len(auto_sac))),
 ('Records marked VERIFIED', '0'),
]
for k, v in checks: line(k, v)
r += 1; line('Issue type', 'Item A', 'Item B / owner', 'Text', fmt=H)
for d in dups + alias_issues: line(*d)
if problems:
    r += 1; line('Code problems', fmt=H)
    for p in problems: line(p)
r += 1; line('Counts by category and status', 'PART/SERVICE', 'CANDIDATE', 'NEEDS_REVIEW', 'UNCLASSIFIED', fmt=H)
cc = collections.Counter((i['type'], i['cat'], i['st']) for i in allitems)
for (t, cat) in sorted({(i['type'], i['cat']) for i in allitems}):
    line(cat, t, cc[(t, cat, 'CANDIDATE')], cc[(t, cat, 'NEEDS_REVIEW')], cc[(t, cat, 'UNCLASSIFIED')])
r += 1; line('Excluded from import (class / reason)', 'Count', fmt=H)
for k, v in collections.Counter(i['import_reason'] for i in excluded).items(): line(k, v)

# Summary --------------------------------------------------------------------------------------
ws = wb.add_worksheet('Summary'); ws.set_column(0, 0, 44); ws.set_column(1, 4, 16)
r = 0
def srow(*vals, fmt=None):
    global r
    for c, v in enumerate(vals): ws.write(r, c, v, fmt)
    r += 1
srow('Official source', 'Count', fmt=H)
srow('HSN rows downloaded (HSN_MSTR)', len(HSN) - 1); srow('SAC rows downloaded (SAC_MSTR)', len(SAC) - 1)
srow('HSN rows selected as automotive-relevant (rule based)', len(auto_hsn)); srow('SAC rows selected as automotive-relevant', len(auto_sac))
r += 1; srow('Catalog records', 'Count', fmt=H)
srow('Parts', len(parts)); srow('Services', len(services)); srow('Total', len(allitems))
r += 1; srow('By record class', 'Count', fmt=H)
for k, v in sorted(collections.Counter(i['cls'] for i in allitems).items()): srow(k, v)
r += 1; srow('By classification status', 'Parts', 'Services', fmt=H)
for s in ['CANDIDATE', 'NEEDS_REVIEW', 'UNCLASSIFIED', 'VERIFIED']:
    srow(s, sum(1 for i in parts if i['st'] == s), sum(1 for i in services if i['st'] == s))
r += 1; srow('Verification status', 'Records', fmt=H)
srow('NOT_VERIFIED', len(allitems)); srow('VERIFIED', 0)
r += 1; srow('By category', 'Type', 'Records', fmt=H)
for (t, cat), n in sorted(collections.Counter((i['type'], i['cat']) for i in allitems).items()): srow(cat, t, n)
r += 1; srow('Import', 'Count', fmt=H)
srow('Rows in import CSV', len(imp)); srow('Excluded (V70 overlap, garage-specific service packages)', len(excluded))

# CBIC historical reference --------------------------------------------------------------------
ws = wb.add_worksheet('CBIC_Rates_2023_Ref')
ws.write(0, 0, 'HISTORICAL REFERENCE ONLY. CBIC page "GST rates for Goods and Services as on 01.04.2023" (cbic-gst.gov.in/hindi/gst-goods-services-rates.html, page footer updated 06 May 2025). SUPERSEDED - NOT EVIDENCE of current rates. Not used for any verification.', RED)
heads = cbic['noid0']['heads']
for c, h in enumerate(heads): ws.write(2, c, h, H)
for rr, row in enumerate(cbic_goods, 3):
    for c, v in enumerate(row[:len(heads)]): ws.write(rr, c, v, W)
base = len(cbic_goods) + 5
ws.write(base - 1, 0, 'SERVICES schedule (same page, same date caveat)', B)
for c, h in enumerate(cbic['noid1']['heads']): ws.write(base, c, h, H)
for rr, row in enumerate(cbic_services, base + 1):
    for c, v in enumerate(row): ws.write(rr, c, v, W)
ws.set_column(0, 1, 12); ws.set_column(2, 2, 34); ws.set_column(3, 3, 70); ws.set_column(4, 7, 14); ws.freeze_panes(3, 0)

# ---- Coverage audit sheets (appended into build.py before wb.close()) ----
# --- origin vs baseline
_base = json.load(open(os.path.join(os.path.dirname(__file__), 'baseline_v1_items.json'), encoding='utf-8'))
def _bkey(it): return '%s|%s' % (it['type'], it['name'])
for it in allitems: it['origin'] = 'Earlier research pass' if _bkey(it) in _base else 'Coverage-audit addition (research-derived)'

# --- HSN review of every four-digit heading
STRONG = re.compile(A.STRAGGLER_PATTERN, re.I)
hsn_rows = [(unescape(c), unescape(d)) for c, d in HSN[1:]]
by_head = collections.OrderedDict()
chap_title = {}
for c, d in hsn_rows:
    if len(c) == 2: chap_title[c] = d
    if len(c) >= 4 and c[:4].isdigit(): by_head.setdefault(c[:4], []).append((c, d))
sel_set = set(HSN_HEADINGS)
hsn_review = []
for h, ents in by_head.items():
    ch = h[:2]
    hd = next((d for c, d in ents if c == h), ents[0][1])
    hits = [c for c, d in ents if STRONG.search(d)]
    if h in A.FLAG_RESOLUTION and h not in sel_set: hits = []
    mapped = sum(n for c, n in used.items() if c[:4] == h)
    plural = 'y' if len(hits) == 1 else 'ies'
    if h in sel_set:
        dec, why = 'SELECTED', HSN_HEADINGS[h]
    elif h in A.FLAG_RESOLUTION:
        dec, why = 'REVIEWED_NOT_SELECTED', A.FLAG_RESOLUTION[h]
    elif ch in A.REVIEWED_CHAPTERS:
        if hits: dec, why = 'FLAGGED_FURTHER_REVIEW', 'Chapter reviewed; %d entr%s name vehicle/engine/brake/tyre/lubricant use but the heading was not selected: needs product-level research' % (len(hits), plural)
        else: dec, why = 'REVIEWED_NOT_SELECTED', 'Chapter reviewed (%s). %s' % (A.REVIEWED_CHAPTERS[ch], A.RELEVANT_CHAPTER_EXTRA_NOTE)
    else:
        if hits: dec, why = 'FLAGGED_FURTHER_REVIEW', 'Straggler sweep of an excluded chapter: %d entr%s match vehicle/engine/brake/tyre/lubricant wording; confirm they are not automotive' % (len(hits), plural)
        else: dec, why = 'EXCLUDED_BY_CHAPTER', A.EXCLUDED_CHAPTER_REASON.get(ch, A.EXCLUDED_CHAPTER_REASON['default'])
    hsn_review.append([ch, chap_title.get(ch, ''), h, hd, len(ents), dec, why, len(hits), ', '.join(hits[:4]), mapped, 'HSN_MSTR'])
hsn_dec = collections.Counter(r[5] for r in hsn_review)
ws = wb.add_worksheet('HSN_Review')
table(ws, ['Chapter', 'Chapter description (official)', '4-digit heading', 'Heading description (official)', 'Entries under heading (all levels)', 'Review decision',
           'Reason (research judgement)', 'Wording hits (count)', 'Example hit codes', 'Catalog items mapped', 'Source sheet'],
      hsn_review, [8, 40, 10, 60, 10, 24, 80, 9, 30, 9, 11])

# --- coverage matrix per chapter
cov = []
for ch in sorted({r[0] for r in hsn_review}):
    rr = [r for r in hsn_review if r[0] == ch]
    total_entries = sum(r[4] for r in rr)
    cnt = collections.Counter(r[5] for r in rr)
    if ch in A.REVIEWED_CHAPTERS: scope = 'REVIEWED (heading level): ' + A.REVIEWED_CHAPTERS[ch]
    else: scope = 'EXCLUDED BY CHAPTER (straggler sweep applied)'
    gaps = []
    if cnt['FLAGGED_FURTHER_REVIEW']: gaps.append('%d heading(s) flagged for product-level research' % cnt['FLAGGED_FURTHER_REVIEW'])
    if ch in A.REVIEWED_CHAPTERS and cnt['REVIEWED_NOT_SELECTED']: gaps.append('%d heading(s) reviewed, none vehicle-specific' % cnt['REVIEWED_NOT_SELECTED'])
    cov.append([ch, chap_title.get(ch, ''), scope, len(rr), total_entries, cnt['SELECTED'], cnt['REVIEWED_NOT_SELECTED'], cnt['EXCLUDED_BY_CHAPTER'],
                cnt['FLAGGED_FURTHER_REVIEW'], sum(r[9] for r in rr), '; '.join(gaps)])
ws = wb.add_worksheet('Coverage_Matrix')
table(ws, ['Chapter', 'Chapter description (official)', 'Scope decision', '4-digit headings', 'Entries (all levels)', 'Headings selected', 'Reviewed, not selected',
           'Excluded by chapter', 'Flagged for further research', 'Catalog items mapped', 'Gaps / further research'],
      cov, [8, 44, 60, 9, 10, 9, 10, 10, 11, 9, 56])

# --- SAC review of all rows
sac_rows = [(unescape(c), unescape(d)) for c, d in SAC[1:]]
rel = re.compile(A.SAC_RELATED_PATTERN, re.I)
group_desc = {c: d for c, d in sac_rows if len(c) in (2, 4)}
sac_sel = set(SAC_SEL) - {'998597'}
sac_review = []
for c, d in sac_rows:
    grp = group_desc.get(c[:4], group_desc.get(c[:2], ''))[:80]
    if c in sac_sel:
        dec, why = 'SELECTED', why_sac.get(c, '')
    elif len(c) < 6:
        dec, why = 'GROUP_HEADER', 'Group/heading row (not a service code)'
    elif rel.search(d):
        dec, why = 'REVIEWED_RELATED_NOT_SELECTED', 'Reviewed: wording touches transport, repair, cleaning, rental or similar but the service is not a vehicle-workshop service; group: ' + grp
    else:
        dec, why = 'REVIEWED_NOT_SELECTED', 'Reviewed: outside vehicle-workshop scope; group: ' + grp
    sac_review.append([c, d, len(c), dec, why, used.get(c, 0), 'SAC_MSTR'])
sac_dec = collections.Counter(r[3] for r in sac_review)
ws = wb.add_worksheet('SAC_Review')
table(ws, ['SAC code (original)', 'Official description (original)', 'Code length', 'Review decision', 'Reason (research judgement)', 'Catalog items mapped', 'Source sheet'],
      sac_review, [12, 80, 8, 30, 90, 9, 11])

# --- source anomalies (computed, never corrected)
anom = []
head_desc = {c: d for c, d in hsn_rows if len(c) == 4}
heads_sorted = sorted(head_desc)
def norm_t(x): return re.sub(r'[^a-z0-9]', '', x.lower())
adj_pairs = []
for a, b in zip(heads_sorted, heads_sorted[1:]):
    if norm_t(head_desc[a]) == norm_t(head_desc[b]):
        adj_pairs.append((a, b))
        kids_b = [d for c, d in hsn_rows if len(c) == 6 and c.startswith(b)][:2]
        anom.append(['IDENTICAL HEADING TEXT (adjacent)', a + ' / ' + b, head_desc[b][:120],
                     'Heading %s carries the same text as %s. First sub-entries under %s: %s' % (b, a, b, ' | '.join(k[:60] for k in kids_b)),
                     'Affects the reading of %s; items near it are NEEDS_REVIEW' % b])
descs = collections.defaultdict(list)
for c in heads_sorted: descs[norm_t(head_desc[c])].append(c)
adj_set = {x for p in adj_pairs for x in p}
for k, v in descs.items():
    if len(v) > 1 and not (set(v) <= adj_set):
        anom.append(['IDENTICAL HEADING TEXT (non-adjacent)', ' / '.join(v), head_desc[v[0]][:120], 'Same normalised description for different headings', 'Verify against the Customs Tariff text'])
odd = [c for c, d in hsn_rows if len(c) not in (2, 4, 6, 8)]
anom.append(['CODE LENGTH OTHER THAN 2/4/6/8', '%d codes' % len(odd), ', '.join(odd[:12]), 'Codes with unexpected length in HSN_MSTR', 'Preserved; not used for mapping'])
anom.append(['SOURCE HEADING TEXT 8538/8539', '8538 / 8539', 'HSN_MSTR 8538: ' + head_desc.get('8538', '')[:70] + ' || 8539: ' + head_desc.get('8539', '')[:110],
             'Observed in the official file: the heading row 8538 repeats the text of 8537; the heading row 8539 carries wording about parts of apparatus of headings 8535, 8536 (repeated) or 8537, '
             'whereas its sub-entries (853910 sealed beam lamp units, 853921 tungsten halogen, 853931 fluorescent) describe lamps, and 853810 describes boards for goods of 8537. '
             'The heading texts therefore appear shifted by one heading relative to their sub-entries. Which text is intended cannot be settled from the file alone. Raw sheet NOT altered.',
             'Lamp and bulb items (8539 sub-entries) are NEEDS_REVIEW; confirm the meaning of 8538 and 8539 against the Customs Tariff text before relying on them'])
sac_dup = collections.defaultdict(list)
for c, d in sac_rows: sac_dup[norm_t(d)].append(c)
for k, v in sac_dup.items():
    if len(v) > 1: anom.append(['DUPLICATE SAC DESCRIPTION', ' / '.join(v), sac_map.get(v[0], '')[:100], 'Same description for different SAC codes', 'Preserved'])
anom.append(['SOURCE HANDLING', '', '', A.ANOMALY_HANDLING, ''])
ws = wb.add_worksheet('Source_Anomalies')
table(ws, ['Anomaly type', 'Code(s)', 'Evidence', 'Detail', 'Effect on this workbook'], anom, [34, 22, 60, 90, 60])

# --- catalog audit log vs baseline
cur = {_bkey(it): it for it in allitems}
def _bcodes(b):
    v = b.get('codes', '')
    return v if isinstance(v, str) else ', '.join(v)
audit = []
for k, it in cur.items():
    if k not in _base:
        audit.append([it['id'], it['type'], it['name'], 'ADDED', 'Found missing in the coverage audit; research-derived', '', it['st'], ', '.join(it['codes'])])
        continue
    b = _base[k]; chg = []
    if b.get('status') != it['st']: chg.append('STATUS_CHANGED')
    if (_bcodes(b).replace(' ', '').split(',')[0] if _bcodes(b) else '') != (it['codes'][0] if it['codes'] else ''): chg.append('PRIMARY_CODE_CHANGED')
    if chg:
        audit.append([it['id'], it['type'], it['name'], '+'.join(chg), 'Over-confident or changed mapping corrected in the audit', '%s | %s' % (b.get('status'), _bcodes(b)), it['st'], ', '.join(it['codes'])])
for k, b in _base.items():
    if k not in cur:
        audit.append(['', k.split('|')[0], k.split('|', 1)[1], 'MERGED_OR_REMOVED_FROM_WORKBOOK', 'Workbook only; nothing is deleted from any database', '%s' % b.get('status'), '', ''])
audit_cnt = collections.Counter(r[3] for r in audit)
ws = wb.add_worksheet('Catalog_Audit_Log')
table(ws, ['Ref ID', 'Type', 'Item', 'Change', 'Reason', 'Before (status | codes)', 'After status', 'After codes'], audit, [10, 9, 40, 30, 70, 50, 16, 30])

# --- mapping / queue sheets
cm = []
for it in allitems:
    for i, (c, kind, odesc) in enumerate(it['code_info']):
        cm.append([it['id'], it['type'], it['name'], c, kind, odesc, 'Primary' if i == 0 else 'Alternate', it['st'], 'NOT_VERIFIED', it['origin']])
ws = wb.add_worksheet('Candidate_Mappings')
table(ws, ['Item ref ID', 'Type', 'Item', 'Candidate code', 'HSN/SAC', 'Official description', 'Rank', 'Classification status', 'Verification status', 'Origin'],
      cm, [10, 9, 40, 12, 7, 60, 10, 16, 14, 34])
ws = wb.add_worksheet('Verified_Mappings')
table(ws, ['Item ref ID', 'Item', 'Code', 'Rate source', 'Verified by', 'Verified on'], [], [10, 40, 12, 40, 20, 14], filt=False)
ws.write(2, 0, 'EMPTY BY DESIGN: no mapping is verified. Verification needs the current Central Tax (Rate) notification and a tax professional (see README).', B)
ws = wb.add_worksheet('Unclassified')
table(ws, ['Ref ID', 'Type', 'Item', 'Category', 'Record class', 'Reason'],
      [[i['id'], i['type'], i['name'], i['cat'], i['cls'], i['note']] for i in allitems if i['st'] == 'UNCLASSIFIED'], [10, 9, 40, 24, 22, 90])
bidx = {}
for n, chunk in enumerate(batches, 1):
    for it in chunk: bidx[it['id']] = 'GarageST_Catalog_Import_Batch_%02d.csv' % n
ws = wb.add_worksheet('Import_Eligible')
table(ws, ['Ref ID', 'Type', 'Name', 'Category', 'Unit', 'Classification status (workbook)', 'Batch file', 'Origin'],
      [[i['id'], i['type'], i['name'], i['cat'], i['unit'], i['st'], bidx[i['id']], i['origin']] for i in imp], [10, 9, 44, 26, 8, 18, 40, 34])
ws = wb.add_worksheet('Excluded_With_Reasons')
table(ws, ['Ref ID', 'Type', 'Name', 'Record class', 'Exclusion reason'],
      [[i['id'], i['type'], i['name'], i['cls'], i['import_reason']] for i in excluded], [10, 9, 44, 24, 90])

# --- audit counts
ws = wb.add_worksheet('Audit_Counts'); ws.set_column(0, 0, 60); ws.set_column(1, 1, 16)
ar = 0
def arow(k, v=None, fmt=None):
    global ar
    ws.write(ar, 0, k, fmt)
    if v is not None: ws.write(ar, 1, v)
    ar += 1
arow('Official source coverage', None, H)
arow('Total 4-digit HSN headings reviewed', len(hsn_review))
for k, v in sorted(hsn_dec.items()): arow('  ' + k, v)
arow('SAC rows in official file (all reviewed)', len(sac_review))
for k, v in sorted(sac_dec.items()): arow('  ' + k, v)
arow('Catalog audit changes vs earlier pass', None, H)
for k, v in sorted(audit_cnt.items()): arow('  ' + k, v)
arow('Source anomalies recorded', len(anom))

# --- SAC mapping rationale and decisions
sac_used = collections.defaultdict(list)
for it in services:
    for c in it['codes']: sac_used[c].append(it['name'])
rows_r = []
for c, (rel_text, amb) in A.SAC_RATIONALE.items():
    rows_r.append([c, sac_map.get(c, ''), len(sac_used.get(c, [])), '; '.join(sac_used.get(c, [])[:6]) + ('; ...' if len(sac_used.get(c, [])) > 6 else ''), rel_text, amb])
ws = wb.add_worksheet('SAC_Mapping_Rationale')
table(ws, ['SAC code', 'Official description (verbatim from SAC_MSTR)', 'Services mapped (count)', 'Examples', 'Relationship between the official entry and the GarageST services', 'Ambiguity / open question'],
      rows_r, [10, 60, 10, 50, 70, 70])
ws.write(len(rows_r) + 2, 0, 'The official list gives codes and descriptions only. GarageST workshop operations are not individual SAC entries; many operations share one broad entry, and applicability is a tax-professional decision.', B)
ws = wb.add_worksheet('Decisions_Required')
table(ws, ['Owner', 'Decision needed'], [list(d) for d in A.DECISIONS], [20, 150])

wb.close()

# plain-text numbers for the report
stats = dict(parts=len(parts), services=len(services), imp=len(imp), excluded=len(excluded),
             parts_status=collections.Counter(i['st'] for i in parts), svc_status=collections.Counter(i['st'] for i in services),
             cls=collections.Counter(i['cls'] for i in allitems), auto_hsn=len(auto_hsn), auto_sac=len(auto_sac),
             hsn=len(HSN) - 1, sac=len(SAC) - 1, problems=len(problems), dups=len(dups), alias=len(alias_issues),
             sha=sha, size=size, starter_overlap=sum(1 for i in allitems if (i['type'], norm(i['name'])) in starter),
             excl_reasons=collections.Counter(i['import_reason'] for i in excluded))
json.dump({k: (dict(v) if isinstance(v, collections.Counter) else v) for k, v in stats.items()},
          open(os.path.join(os.path.dirname(__file__), 'build_stats.json'), 'w'), indent=1)
print(json.dumps({k: (dict(v) if isinstance(v, collections.Counter) else v) for k, v in stats.items()}, indent=1))
