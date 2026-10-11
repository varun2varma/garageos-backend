import zipfile, re, xml.etree.ElementTree as ET
NS={'m':'http://schemas.openxmlformats.org/spreadsheetml/2006/main','r':'http://schemas.openxmlformats.org/officeDocument/2006/relationships'}
def col_idx(ref):
    s=re.match(r'[A-Z]+',ref).group(0); n=0
    for ch in s: n=n*26+ord(ch)-64
    return n-1
def read(path):
    z=zipfile.ZipFile(path)
    ss=[]
    for si in ET.fromstring(z.read('xl/sharedStrings.xml')).findall('m:si',NS):
        ss.append(''.join(t.text or '' for t in si.iter('{%s}t'%NS['m'])))
    wb=ET.fromstring(z.read('xl/workbook.xml'))
    rels={r.get('Id'):r.get('Target') for r in ET.fromstring(z.read('xl/_rels/workbook.xml.rels'))}
    sheets={}
    for s in wb.find('m:sheets',NS):
        rid=s.get('{%s}id'%NS['r']); target=rels[rid]
        root=ET.fromstring(z.read('xl/'+target.lstrip('/').replace('xl/','')))
        rows=[]
        for row in root.iter('{%s}row'%NS['m']):
            vals={}
            for c in row.findall('m:c',NS):
                t=c.get('t'); v=c.find('m:v',NS)
                if t=='s' and v is not None: val=ss[int(v.text)]
                elif t=='inlineStr': val=''.join(x.text or '' for x in c.iter('{%s}t'%NS['m']))
                else: val=v.text if v is not None else ''
                vals[col_idx(c.get('r'))]=val
            if vals:
                m=max(vals)+1
                rows.append([vals.get(i,'') for i in range(m)])
        sheets[s.get('name')]=rows
    return sheets
