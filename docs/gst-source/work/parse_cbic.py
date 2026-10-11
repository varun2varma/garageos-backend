import re, html, json
t=open('cbic_rates.html',encoding='utf-8',errors='ignore').read()
def clean(s):
    s=re.sub(r'<br\s*/?>',' ',s); s=re.sub(r'<[^>]+>','',s); s=html.unescape(s).replace('\xa0',' ')
    return re.sub(r'\s+',' ',s).strip()
tables=re.findall(r'<table\b[^>]*?(?:id="([^"]*)")?[^>]*>(.*?)</table>',t,re.S)
out={}
for tid,body in tables:
    heads=[clean(h) for h in re.findall(r'<th[^>]*>(.*?)</th>',body,re.S)]
    rows=[]
    for tr in re.findall(r'<tr[^>]*>(.*?)</tr>',body,re.S):
        cells=[clean(c) for c in re.findall(r'<td[^>]*>(.*?)</td>',tr,re.S)]
        if cells: rows.append(cells)
    out[tid or 'noid%d'%len(out)]={'heads':heads,'rows':rows}
    print(tid,heads,len(rows))
json.dump(out,open('cbic_rates_parsed.json','w',encoding='utf-8'),ensure_ascii=False)
# anything describing date / amendment in page text
txt=clean(t)
for m in re.finditer(r'(as (?:amended|on)[^.]{0,120}|updated[^.]{0,100}|Notification No[^.]{0,100})',txt[:200000],re.I):
    print('NOTE:',m.group(0)[:160]); 
    break
print(txt[:700])
