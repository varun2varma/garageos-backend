# -*- coding: utf-8 -*-
"""Edge-case checks of POST /api/v1/catalog/import against a LOCAL ISOLATED backend (see run_import.py).

Usage: python import_edge_cases.py <password_file> <username>
Test rows are named with the prefix 'ZZ Import Test' so they can be told apart from real data.
"""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
import run_import as R

HDR = 'type,name,category,default_unit,description\n'
HDR_CODES = 'type,name,category,default_unit,description,hsn_sac_code,tax_rule_code\n'


def imp(token, csv_text, dry):
    st, resp = R.call('/api/v1/catalog/import', {'csv': csv_text, 'dryRun': dry}, token)
    rep = resp.get('data', resp) if isinstance(resp, dict) else resp
    return st, rep


def show(title, st, rep):
    if st == 200:
        print('%-58s HTTP 200 dry=%s total=%s created=%s dup=%s invalid=%s' % (title, rep.get('dryRun'), rep.get('totalRows'), rep.get('created'), len(rep['duplicates']), len(rep['invalid'])))
        for k in ('duplicates', 'invalid'):
            for r in rep[k][:3]: print('      %s row %s %r: %s' % (k, r['row'], r['name'][:60], r['reason']))
    else:
        print('%-58s HTTP %s %s' % (title, st, json.dumps(rep)[:230]))


def main():
    pw = open(sys.argv[1], encoding='utf-8').read().strip()
    st, resp = R.call('/api/v1/auth/login', {'username': sys.argv[2], 'password': pw})
    token = resp['data']['accessToken']
    # 1. validation outcomes (dry run, so nothing is saved)
    cases = [
        ('invalid unit', HDR + 'PART,ZZ Import Test A,Cat,BAD,desc\n'),
        ('invalid type', HDR + 'TOOL,ZZ Import Test B,Cat,NOS,desc\n'),
        ('blank name', HDR + 'PART,,Cat,NOS,desc\n'),
        ('name 151 chars', HDR + 'PART,%s,Cat,NOS,desc\n' % ('N' * 151)),
        ('category 61 chars', HDR + 'PART,ZZ Import Test C,%s,NOS,desc\n' % ('C' * 61)),
        ('description 501 chars', HDR + 'PART,ZZ Import Test D,Cat,NOS,%s\n' % ('D' * 501)),
        ('missing default_unit value', HDR + 'PART,ZZ Import Test E,Cat,,desc\n'),
        ('repeated within file', HDR + 'PART,ZZ Import Test F,Cat,NOS,d\nPART,  zz   IMPORT test f ,Cat,NOS,d\n'),
        ('existing item, different case/spacing', HDR + 'PART,  cylinder   HEAD ,Cat,NOS,d\n'),
        ('existing V70 starter name', HDR + 'SERVICE,General Periodic Service,Cat,JOB,d\n'),
        ('same name, other type is not a duplicate', HDR + 'SERVICE,Cylinder Head,Cat,JOB,d\n'),
        ('service with HSN-style code', HDR_CODES + 'SERVICE,ZZ Import Test G,Cat,JOB,d,842131,\n'),
        ('MALFORMED code (space): dry run cannot see the DB check', HDR_CODES + 'PART,ZZ Import Test G2,Cat,NOS,d,8421 31,\n'),
        ('part with 99 code', HDR_CODES + 'PART,ZZ Import Test H,Cat,NOS,d,998714,\n'),
        ('unknown tax rule', HDR_CODES + 'PART,ZZ Import Test I,Cat,NOS,d,8421,NO_SUCH_RULE\n'),
        ('quoted field with comma and doubled quote', HDR + 'PART,"ZZ Import Test J, ""quoted""",Cat,NOS,"a, b"\n'),
        ('missing required header column', 'type,name,category\nPART,ZZ Import Test K,Cat\n'),
        ('empty file', '\n'),
    ]
    for title, text in cases:
        st, rep = imp(token, text, True)
        show(title, st, rep)
    big = HDR + ''.join('PART,ZZ Import Test Big %d,Cat,NOS,d\n' % i for i in range(501))
    st, rep = imp(token, big, True); show('501 rows', st, rep)
    huge = HDR + 'PART,ZZ Import Test Huge,Cat,NOS,%s\n' % ('x' * 200001)
    st, rep = imp(token, huge, True); show('csv text over 200,000 characters', st, rep)
    # 2. re-import of an existing batch: nothing may be created or changed
    first = os.path.join(R.ROOT, 'GarageST_Catalog_Import_Batch_01.csv')
    st, rep = imp(token, open(first, encoding='utf-8', newline='').read(), False)
    show('re-import batch 01 (real run)', st, rep)
    # 3. partial import with well-formed codes: valid rows are kept, invalid rows are reported (REAL run, isolated schema only)
    NL = chr(10)
    text = HDR_CODES + NL.join(['PART,ZZ Import Test P1,Cat,NOS,valid one,842131,', 'PART,ZZ Import Test P2,Cat,BAD,invalid unit,,',
                                'SERVICE,ZZ Import Test P3,Cat,JOB,valid two,998714,', 'PART,ZZ Import Test P4,Cat,NOS,part with SAC,998714,']) + NL
    st, rep = imp(token, text, False)
    show('partial import, real run (2 valid, 2 invalid)', st, rep)
    # 3b. a malformed code is NOT a BusinessException: the whole request rolls back (HTTP 500, nothing saved)
    text = HDR_CODES + NL.join(['PART,ZZ Import Test R1,Cat,NOS,fine,,', 'PART,ZZ Import Test R2,Cat,NOS,bad code,8421 31,']) + NL
    st, rep = imp(token, text, False)
    show('malformed code, real run (R1 fine, R2 bad)', st, rep)
    # 4. authorization: no token, and an ordinary (non-admin) user calling the admin endpoints directly
    st, resp = R.call('/api/v1/catalog/import', {'csv': HDR + 'PART,ZZ Import Test NoAuth,Cat,NOS,d' + chr(10), 'dryRun': True})
    print('%-58s HTTP %s' % ('import, no token', st))
    body = {'firstName': 'Plain', 'lastName': 'User', 'mobile': '9876501299', 'email': 'plain.user@example.test', 'username': 'plainuser', 'password': pw}
    R.call('/api/v1/auth/register', body)
    st, resp = R.call('/api/v1/auth/login', {'username': 'plainuser', 'password': pw})
    ptoken = resp['data']['accessToken']
    print('ordinary user roles:', resp['data']['user'].get('roles'))
    calls = [('POST', '/api/v1/catalog/import', {'csv': HDR + 'PART,ZZ Import Test Plain,Cat,NOS,d' + chr(10), 'dryRun': False}),
             ('POST', '/api/v1/catalog/items', {'itemType': 'PART', 'name': 'ZZ Plain Create', 'defaultUnit': 'NOS'}),
             ('GET', '/api/v1/catalog/admin/items', None),
             ('PUT', '/api/v1/catalog/items/1', {'name': 'x', 'defaultUnit': 'NOS', 'active': True, 'version': 0}),
             ('PATCH', '/api/v1/catalog/items/1/active', {'active': False, 'version': 0}),
             ('POST', '/api/v1/catalog/items/1/classification', {'hsnSacCode': '842131', 'version': 0}),
             ('GET', '/api/v1/catalog/tax-rules', None),
             ('POST', '/api/v1/catalog/tax-rules', {'ruleCode': 'ZZ', 'description': 'x'}),
             ('PATCH', '/api/v1/catalog/tax-rules/1', {'active': False}),
             ('GET', '/api/v1/catalog/review/estimate-items', None),
             ('POST', '/api/v1/catalog/review/estimate-items/1/link', {'catalogItemId': 1})]
    for method, path, b in calls:
        st1, _ = R.call(path, b, ptoken, method)
        st2, _ = R.call(path, b, None, method)
        print('%-58s ordinary user HTTP %s, no token HTTP %s' % (method + ' ' + path.replace('/api/v1/catalog', ''), st1, st2))


if __name__ == '__main__':
    main()
