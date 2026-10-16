# Media key collision check (run BEFORE deploying migration V80)

Migration `V80__media_sequence_and_key_integrity.sql` adds unique indexes on
`job_card_media (job_card_id, media_stage, media_sequence)` and `job_card_media (storage_key)`.
It **aborts without changing data** if existing rows already collide. A collision means two rows
share one object set, so the first upload's bytes may already have been overwritten. Such rows are
**never reassigned or deleted automatically.**

## 1. Read-only detection (safe on production; SELECT only)

```sql
-- same storage key used by more than one row
SELECT storage_key, count(*) AS rows_sharing, array_agg(id ORDER BY id) AS media_ids
  FROM job_card_media
 WHERE storage_key IS NOT NULL
 GROUP BY storage_key
HAVING count(*) > 1;

-- same (job card, stage, sequence), derived from the file name
SELECT job_card_id, media_stage,
       CAST(substring(file_name FROM '_([0-9]+)\.[A-Za-z0-9]+$') AS INT) AS media_sequence,
       count(*) AS rows_sharing, array_agg(id ORDER BY id) AS media_ids
  FROM job_card_media
 WHERE file_name ~ '_[0-9]+\.[A-Za-z0-9]+$'
 GROUP BY 1, 2, 3
HAVING count(*) > 1;
```

Zero rows from both queries: V80 will apply cleanly.

## 2. If rows are returned

1. Do **not** delete or rename anything yet; export the result for the incident record.
2. For each group, inspect the object in storage (size/last-modified) and the rows' `created_at`,
   `uploaded_by`, `file_size`, `checksum`. The earliest row's original bytes are normally lost; the latest
   row owns the surviving object.
3. Decide per group with the garage: keep the surviving row, and mark the other row(s) as lost evidence
   (a manual, audited data fix by the operations owner), or re-capture the evidence.
4. Re-run the detection queries until both return zero rows, then deploy.

## 3. After deployment

New uploads allocate sequence numbers atomically from `job_card_media_counter`, so a collision can no
longer be created. Deleting a media row only removes an object that no other row references.
