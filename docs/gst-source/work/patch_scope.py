# One-off patch applying the catalog-scope decisions to build.py / audit_data.py / data_parts.py (idempotent).
import re
base = __file__.rsplit('\\', 1)[0].rsplit('/', 1)[0] if False else None
import os
W = os.path.dirname(os.path.abspath(__file__))

def rw(name, fn):
    p = os.path.join(W, name)
    s = open(p, encoding='utf-8', newline='').read()
    s2 = fn(s)
    if s2 != s: open(p, 'w', encoding='utf-8', newline='').write(s2)
    print(name, 'changed' if s2 != s else 'unchanged')

def build(s):
    s = s.replace("IMPORTABLE_PART = {'PART', 'FLUID_LUBRICANT', 'DETAILING_PRODUCT', 'EV_SPECIALIST'}",
                  "IMPORTABLE_PART = {'PART', 'FLUID_LUBRICANT', 'DETAILING_PRODUCT', 'EV_SPECIALIST', 'WORKSHOP_CONSUMABLE'}")
    s = s.replace("IMPORTABLE_SERVICE = {'LABOUR_OPERATION'}", "IMPORTABLE_SERVICE = {'LABOUR_OPERATION', 'OUTSOURCED_SERVICE'}")
    old = s[s.index("        reason = {'WORKSHOP_CONSUMABLE'"):s.index(".get(it['cls'], 'Class not imported')")]
    new = ("        reason = {'SERVICE_PACKAGE': 'Garage-specific: package composition and pricing belong to each garage; the underlying operations are separate catalog services'}")
    s = s.replace(old, new)
    # scope-clarifying description prefixes (done once, right after parsing)
    marker = "def enrich(it, kind):"
    prefix_block = (
        "PREFIX = {'OUTSOURCED_SERVICE': 'Outsourced work performed by an external workshop. ',\n"
        "          'WORKSHOP_CONSUMABLE': 'Workshop consumable (inventory-tracked, billing treatment chosen by the garage). ',\n"
        "          'EV_SPECIALIST': 'Specialist high-voltage component. '}\n"
        "for _it in parts + services:\n"
        "    _p = PREFIX.get(_it['cls'], '')\n"
        "    if _it['cat'] == 'Electric Vehicle Services': _p = 'Specialist EV work requiring trained personnel and equipment. '\n"
        "    if _p and not _it['desc'].startswith(_p): _it['desc'] = _p + _it['desc']\n\n")
    if 'PREFIX = {' not in s:
        s = s.replace(marker, prefix_block + marker, 1)
    return s

def audit(s):
    if "'8505': 'Electromagnetic couplings" not in s:
        s = s.replace(" '8705': 'Special purpose motor vehicles",
                      " '8505': 'Electromagnetic couplings, clutches and brakes (850520: compressor and fan clutches)',\n '8705': 'Special purpose motor vehicles", 1)
    add = {
     '2711': 'Natural gas, propane and butane are fuels (CNG/LPG): not a parts, fluid or service catalog item; fuel sale is outside this catalog',
     '8426': 'Cranes and lifting frames (842691 is for mounting on road vehicles): workshop or vehicle equipment, not a billable part',
     '8462': 'Metal forming machine tools and presses: industrial machinery, not workshop parts',
     '8467': 'Hand-held power tools: garage-owned workshop tools, not a billable catalog item',
     '8477': 'Rubber and plastics machinery (847751 tyre retreading machines): manufacturing machinery',
     '8502': 'Electric generating sets: workshop utility equipment, not an automotive part',
    }
    for k, v in add.items():
        if "'%s': '" % k not in s.split('FLAG_RESOLUTION')[1]:
            s = s.replace("FLAG_RESOLUTION = {", "FLAG_RESOLUTION = {\n '%s': '%s'," % (k, v), 1)
        else:
            s = re.sub(r" '%s': '[^\n]*" % k, " '%s': '%s'," % (k, v), s.split('FLAG_RESOLUTION')[0]) + 'FLAG_RESOLUTION' + s.split('FLAG_RESOLUTION')[1]
    s = s.replace(" '8505': 'Electro-magnetic and permanent magnets: wording hit only, not automotive',\n", "")
    return s

def parts(s):
    return s.replace("AC Compressor Clutch | Compressor Clutch Coil | Electromagnetic clutch. | Vehicle fit | codes=841490",
                     "AC Compressor Clutch | Compressor Clutch Coil | Electromagnetic clutch. | Vehicle fit | codes=841490,850520")

rw('build.py', build)
rw('audit_data.py', audit)
rw('data_parts.py', parts)
