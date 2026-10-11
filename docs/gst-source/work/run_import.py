# -*- coding: utf-8 -*-
"""Drive POST /api/v1/catalog/import for the batch files against a LOCAL, ISOLATED backend.

Usage:  python run_import.py <dry|real> <password_file> <username> [batch_file ...]
Refuses any base URL other than http://localhost:8080 / http://127.0.0.1:8080.
The password is read from a file and never printed. The dry-run flag is sent explicitly in every request.
"""
import glob, json, os, sys, urllib.request, urllib.error

BASE = os.environ.get('GARAGEST_BASE_URL', 'http://localhost:8080')
if BASE not in ('http://localhost:8080', 'http://127.0.0.1:8080'):
    sys.exit('Refusing: this script only talks to a local backend, got ' + BASE)

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))


def call(path, body, token=None, method='POST'):
    data = None if body is None else json.dumps(body).encode('utf-8')
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    if token: req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            return r.status, json.loads(r.read().decode('utf-8') or 'null')
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8', 'replace')
        try: return e.code, json.loads(raw)
        except Exception: return e.code, {'raw': raw[:300]}


def main():
    mode, pwfile, user = sys.argv[1], sys.argv[2], sys.argv[3]
    files = sys.argv[4:] or sorted(glob.glob(os.path.join(ROOT, 'GarageST_Catalog_Import_Batch_*.csv')))
    if mode not in ('dry', 'real'): sys.exit('mode must be dry or real')
    pw = open(pwfile, encoding='utf-8').read().strip()
    st, resp = call('/api/v1/auth/login', {'username': user, 'password': pw})
    data = (resp or {}).get('data', resp) if isinstance(resp, dict) else {}
    token = data.get('accessToken') if isinstance(data, dict) else None
    if st != 200 or not token: sys.exit('login failed: HTTP %s' % st)
    roles = (data.get('user') or {}).get('roles')
    print('login ok, roles:', roles)
    totals = dict(total=0, created=0, duplicates=0, invalid=0)
    for f in files:
        csv_text = open(f, encoding='utf-8', newline='').read()
        st, resp = call('/api/v1/catalog/import', {'csv': csv_text, 'dryRun': mode == 'dry'}, token)
        rep = (resp or {}).get('data', resp) if isinstance(resp, dict) else {}
        if st != 200:
            print(os.path.basename(f), 'HTTP', st, json.dumps(resp)[:300]); continue
        print('%s: HTTP %s dryRun=%s totalRows=%s created=%s duplicates=%s invalid=%s' % (
            os.path.basename(f), st, rep.get('dryRun'), rep.get('totalRows'), rep.get('created'),
            len(rep.get('duplicates') or []), len(rep.get('invalid') or [])))
        for k in ('duplicates', 'invalid'):
            for row in (rep.get(k) or [])[:10]:
                print('   ', k, row)
        totals['total'] += rep.get('totalRows') or 0
        totals['created'] += rep.get('created') or 0
        totals['duplicates'] += len(rep.get('duplicates') or [])
        totals['invalid'] += len(rep.get('invalid') or [])
    print('TOTALS', totals)


if __name__ == '__main__':
    main()
