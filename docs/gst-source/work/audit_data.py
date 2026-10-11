# -*- coding: utf-8 -*-
# Coverage-audit decisions (research data, not an official source). Used by build.py.
# Every decision is a GarageST research judgement for catalog relevance. None is a tax classification.

# Chapters reviewed at heading level (automotive parts, fluids, consumables, tools or components may sit in them).
REVIEWED_CHAPTERS = {
 '27': 'Mineral fuels, oils: lubricating oils, greases',
 '28': 'Inorganic chemicals: possible workshop chemicals',
 '29': 'Organic chemicals: refrigerant gases, solvents',
 '31': 'Fertilisers: urea (diesel exhaust fluid)',
 '32': 'Paints, varnishes, putty, dyes',
 '34': 'Soaps, cleaning, polishes, lubricating preparations',
 '35': 'Glues and adhesives',
 '38': 'Miscellaneous chemical products: additives, brake fluid, coolant',
 '39': 'Plastics and articles: hoses, trims, containers',
 '40': 'Rubber and articles: tyres, tubes, belts, hoses, seals',
 '56': 'Wadding, felt, nonwovens: possible cleaning or trim textiles',
 '57': 'Carpets: car mats',
 '59': 'Coated textiles: possible trim and hoses',
 '63': 'Made-up textile articles: seat covers, cloths, covers',
 '68': 'Stone/plaster/cement/asbestos: friction material, brake linings',
 '70': 'Glass: safety glass, mirrors',
 '72': 'Iron and steel: possible workshop raw material (context)',
 '73': 'Articles of iron or steel: fasteners, springs, chains, pipes',
 '76': 'Aluminium articles',
 '82': 'Tools and cutlery: workshop hand tools',
 '83': 'Miscellaneous base-metal articles: locks, fittings',
 '84': 'Machinery: engines, pumps, compressors, filters, bearings, gaskets, workshop machinery',
 '85': 'Electrical machinery: batteries, starters, lighting, wiring, audio, EV components',
 '86': 'Railway (context only)',
 '87': 'Vehicles and parts',
 '90': 'Instruments: sensors, meters, speedometers, diagnostic equipment',
 '94': 'Furniture: vehicle seats',
 '96': 'Miscellaneous manufactured articles: brushes',
}
# Chapters excluded as a whole. Reason is the chapter-level judgement. The straggler sweep below still flags individual
# headings inside these chapters whose description names vehicles, engines, brakes, tyres or similar.
EXCLUDED_CHAPTER_REASON = {
 'default': 'Chapter covers goods with no vehicle-workshop part, fluid or consumable use (food, agriculture, textiles, minerals, '
            'footwear, jewellery, arms, toys, art and similar)',
 '98': 'Chapter 98 (project imports and special classifications) is a customs-administration chapter',
}
RELEVANT_CHAPTER_EXTRA_NOTE = 'No vehicle-specific entry found at heading level'

# Extra four-digit headings selected after the audit (heading: reason). Merged with build.py HSN_HEADINGS.
ADD_HEADINGS = {
 '3917': 'Plastic tubes, pipes and hoses (vehicle hoses may sit here or in 4009)',
 '3919': 'Self-adhesive plates, sheets, tapes (masking tape, trim tape, film)',
 '3923': 'Plastic containers and closures (caps, bottles)',
 '3926': 'Other articles of plastics (trims, clips, covers)',
 '7326': 'Other articles of iron or steel (clips, brackets, heat shields)',
 '7616': 'Other articles of aluminium',
 '8424': 'Fire extinguishers, spray guns (vehicle fire extinguisher, spray appliances)',
 '8425': 'Jacks and hoists (workshop jacks, vehicle lifts)',
 '8503': 'Parts of electric motors and generators',
 '8518': 'Loudspeakers (vehicle audio)',
 '8525': 'Cameras (dash cameras, reversing cameras)',
 '8529': 'Aerials and parts of radio/TV apparatus (vehicle antennas)',
 '8543': 'Electrical machines and apparatus n.e.c. (EV charging equipment candidate)',
 '8505': 'Electromagnetic couplings, clutches and brakes (850520: compressor and fan clutches)',
 '8705': 'Special purpose motor vehicles (context: workshop and recovery vehicles)',
}

# SAC rows selected beyond the first-pass list (code: reason). Rows found in the group-by-group review of all 681 SAC rows.
SAC_ADD = {
 '998349': 'Other technical and scientific services n.e.c. (inspection or testing not in 998346)',
 '998531': 'Disinfecting services (vehicle cabin sanitisation candidate)',
 '998599': 'Other support services n.e.c. (residual)',
 '998883': 'Job work in relation to bus body building (bus body repair candidate)',
 '998873': 'Fabricated metal product and metal treatment services (machine shop, coating candidate)',
 '997311': 'Leasing or rental of transport equipment without operator (courtesy vehicle candidate)',
 '996743': 'Parking lot services (vehicle storage candidate)',
 '998523': 'Security systems services (vehicle alarm and tracker candidate)',
 '998717': 'Maintenance and repair of commercial and industrial machinery (workshop equipment service; context)',
 '998719': 'Maintenance and repair of other machinery (residual)',
}

# SAC groups: 4-digit group -> explanation used for non-selected rows (default is the group description from the file).
SAC_RELATED_PATTERN = (r'vehicle|motor|transport|repair|maintenance|clean|testing|towing|tyre|paint|install|rental|leasing|'
                       r'parking|security|inspection|certification|washing|coating|metal|body|job work')
# Strong keywords used by the straggler sweep of excluded chapters (heading level).
STRAGGLER_PATTERN = (r'motor vehicle|motor car|motor cycle|motorcycle|automotive|vehicles? of chapter 87|tyre|tire|spark.?ignition|'
                     r'internal combustion|lubricat|brake|clutch|windscreen|coachwork|chassis')

# Documented source anomalies are computed in build.py; this text explains handling.
ANOMALY_HANDLING = ('Preserved exactly as in HSN_SAC.xlsx (Official_HSN_Raw is byte-for-value identical to the source). The anomaly is '
                    'recorded here, never corrected. Any code mapped near an anomaly is NEEDS_REVIEW.')

# Headings flagged by the wording sweep that were then judged out of scope (heading: reason).
FLAG_RESOLUTION = {
 '8502': 'Electric generating sets: workshop utility equipment, not an automotive part',
 '8477': 'Rubber and plastics machinery (847751 tyre retreading machines): manufacturing machinery',
 '8467': 'Hand-held power tools: garage-owned workshop tools, not a billable catalog item',
 '8462': 'Metal forming machine tools and presses: industrial machinery, not workshop parts',
 '8426': 'Cranes and lifting frames (842691 is for mounting on road vehicles): workshop or vehicle equipment, not a billable part',
 '2711': 'Natural gas, propane and butane are fuels (CNG/LPG): not a parts, fluid or service catalog item; fuel sale is outside this catalog',
 '8701': 'Complete vehicles (tractors): vehicles are not workshop parts, fluids or services; out of scope for this catalog',
 '8702': 'Complete vehicles (buses): out of scope for this catalog',
 '8703': 'Complete vehicles (cars): out of scope for this catalog',
 '8704': 'Complete vehicles (goods vehicles): out of scope for this catalog',
 '8607': 'Railway rolling-stock parts: not road-vehicle workshop scope',
 '9701': 'Paintings: wording hit only (coachwork/art), not automotive',
 '9805': 'Ship stores: wording hit only, not automotive',
 '7802': 'Lead waste and scrap: wording hit only (batteries scrap context), not a catalog item',
 '7902': 'Zinc waste and scrap: not a catalog item',
 '3916': 'Plastic monofilament: wording hit only, raw material',
 '3915': 'Plastic waste and scrap: raw material',
 '3920': 'Plastic plates and film: raw material (protective film is a service item, not a goods mapping)',
 '3921': 'Plastic plates and film: raw material',
 '4002': 'Synthetic rubber in primary forms: raw material',
 '4008': 'Rubber plates and rods: raw material',
 '5402': 'Synthetic filament yarn: raw material (tyre-cord wording)',
 '5403': 'Artificial filament yarn: raw material (tyre-cord wording)',
 '5607': 'Twine and cordage: wording hit only',
 '5902': 'Tyre cord fabric: raw material for tyre makers, not a workshop item',
 '3903': 'Polymers of styrene in primary forms: raw material',
 '3825': 'Residual chemical products: wording hit only',
 '2902': 'Cyclic hydrocarbons: raw chemical',
}

# Relationship between the official SAC description and the GarageST services mapped to it.
SAC_RATIONALE = {
 '998714': ('Maintenance and repair of transport machinery: the closest official entry for servicing, repair, overhaul and part replacement on road vehicles.',
            'Whether work supplied together with parts is a composite or mixed supply, and whether tyre, body, electrical and EV work all sit here, is a tax-professional decision. Most workshop operations have no dedicated SAC of their own.'),
 '998346': ('Technical testing and analysis: candidate for diagnostics, inspection and emission testing performed as a stand-alone service.',
            'Diagnostics performed as part of a repair may belong with the repair (998714). The standalone-versus-ancillary test is not decided by the official description.'),
 '998538': ('Cleaning of transportation equipment: candidate for washing, detailing and interior cleaning.',
            'Whether coatings and film installation are cleaning or installation/job work is open.'),
 '998736': ('Installation of electrical machinery and apparatus: candidate for fitting electronic accessories (cameras, sensors, audio, trackers).',
            'Fitting an accessory sold by the garage together with the product may be a supply of goods with installation.'),
 '998739': ('Installation of other goods: candidate for fitting non-electrical accessories.', 'Same composite-supply question as 998736.'),
 '998881': ('Motor vehicle manufacturing job work: candidate for body and paint job work.', 'Body repair may instead be repair under 998714; the character of the supply decides.'),
 '996744': ('Towing services: matches vehicle towing directly.', 'Flatbed carriage may instead be goods transport (996511).'),
 '996749': ('Other supporting services for road transport: candidate for roadside assistance.', 'Residual entry; applicability unconfirmed.'),
 '996511': ('Road transport of goods: candidate for vehicle carrier or flatbed transport.', 'Applies only if the vehicle is treated as goods transported.'),
 '998729': ('Maintenance and repair of other goods n.e.c.: candidate for upholstery repair.', 'Residual entry; 998714 may apply instead.'),
 '999719': ('Other washing and cleaning services n.e.c.: candidate for sanitisation.', 'Residual entry; 998531 (disinfection) is an alternative.'),
}

DECISIONS = [
 ('Tax professional', 'Current GST rate, conditions and effective dates for every candidate classification (the current Central Tax (Rate) notification was not retrievable; the 2023 CBIC schedule is stale and not evidence).'),
 ('Tax professional', 'Whether 998714 is the right SAC for repair, tyre, body, electrical and EV work, and the composite-versus-mixed-supply treatment of repair with parts.'),
 ('Tax professional', 'Which HSN applies to each NEEDS_REVIEW part (for example 8708 versus 6813 for brake pads; 2710 versus 3403 for oils; 8409 versus the machine headings for engine parts).'),
 ('Tax professional', 'Meaning of headings 8538/8539 in the official file (heading texts appear shifted against their sub-entries) before relying on lamp and bulb mappings.'),
 ('Tax professional', 'Whether the pricing convention (parts GST-inclusive, labour GST-exclusive) is acceptable.'),
 ('Product owner', 'Service packages: model them per garage (composition and price) in a future feature; they are not in the shared catalog.'),
 ('Product owner', 'Whether workshop consumables should be billable catalog items or stay as other charges; they are importable as PART records with a consumable label.'),
 ('Product owner', 'A correction path for an approved or invoiced estimate line that was linked to a catalog item: today the link records the catalog reference but does not copy the classification onto an approved or invoiced estimate (see the readiness report).'),
 ('Product owner', 'Whether the catalog should add a first-class class/kind field (consumable, outsourced, specialist) instead of carrying it in category and description.'),
 ('Product owner', 'Whether the import API should offer a per-row structured error code; today the reason is free text.'),
]
