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
