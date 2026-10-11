# -*- coding: utf-8 -*-
"""Compare the catalog_item dump taken after an isolated import with the batch CSV files and the pre-import dump.

Usage: python compare_import.py <baseline_dump.tsv> <after_dump.tsv> [--ignore-prefix ZZ]
Dump columns: type, name, category, unit, description, status, hsn_sac_code, tax_rule_code, active, created_by_set
"""
import csv, glob, os, re, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))


def norm(s): return re.sub(r'\s+', ' ', s.strip()).lower()


def load(path):
    rows = {}
    for line in open(path, encoding='utf-8').read().split('\n'):
        if not line: continue
        c = line.split('\t')
        rows[(c[0], norm(c[1]))] = c
    return rows


def main():
    base, after = load(sys.argv[1]), load(sys.argv[2])
    ignore = sys.argv[sys.argv.index('--ignore-prefix') + 1] if '--ignore-prefix' in sys.argv else None
    if ignore: after = {k: v for k, v in after.items() if not v[1].startswith(ignore)}
    expected = {}
    for f in sorted(glob.glob(os.path.join(ROOT, 'GarageST_Catalog_Import_Batch_*.csv'))):
        for r in csv.DictReader(open(f, encoding='utf-8', newline='')):
            expected[(r['type'], norm(r['name']))] = r
    problems = []
    for k, r in expected.items():
        if k not in after: problems.append(('MISSING in DB', k)); continue
        c = after[k]
        exp = [r['type'], r['name'].strip(), r['category'], r['default_unit'].strip().upper(), r['description'], 'UNCLASSIFIED', '', '', 't', 't']
        for i, label in enumerate(['type', 'name', 'category', 'unit', 'description', 'status', 'hsn_sac_code', 'tax_rule_code', 'active', 'created_by_set']):
            if c[i] != exp[i]: problems.append(('FIELD %s differs' % label, k, exp[i][:50], c[i][:50]))
    unchanged = sum(1 for k, v in base.items() if k in after and after[k] == v)
    extra = [k for k in after if k not in expected and k not in base]
    print('expected from CSV:', len(expected))
    print('found with all fields equal:', len(expected) - len({p[1] for p in problems}))
    print('pre-existing rows: %d, unchanged after import: %d' % (len(base), unchanged))
    print('rows in DB that are neither pre-existing nor from the CSV:', len(extra), extra[:5])
    print('problems:', len(problems))
    for p in problems[:15]: print('  ', p)
    print('DB total after:', len(after))


if __name__ == '__main__':
    main()
