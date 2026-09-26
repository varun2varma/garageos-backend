package com.garageos.modules.media.service;

import com.garageos.modules.media.entity.JobCardMedia;

/**
 * Single place that owns the "what happens after a Drive attempt fails"
 * decision (classify, persist durable status, schedule/deny retry) so the
 * synchronous first attempt inside MediaServiceImpl.uploadMedia and the
 * background backoff retry (MediaUploadRetryScheduler) apply exactly the
 * same rule — there is only one upload code path in this module (both
 * manager and technician uploads already call MediaController.uploadMedia
 * / MediaServiceImpl.uploadMedia; there is no separate technician/manager
 * pipeline to converge), so this is the one place that logic needs to live.
 */
public interface MediaUploadRetryService {

    /**
     * Attempts the Drive upload for a row whose bytes are already durably
     * buffered (uploadStatus PENDING/UPLOADING/RETRY_WAIT/AUTH_REQUIRED),
     * classifying and persisting the outcome (COMPLETED, RETRY_WAIT with a
     * backoff nextRetryAt, AUTH_REQUIRED, or terminal FAILED once backoff
     * is exhausted). Never throws for a classified Drive/auth failure —
     * only for a programming error (e.g. the row's buffered bytes are
     * missing). Returns the saved row in its resulting state.
     */
    JobCardMedia attemptUpload(JobCardMedia media);

    /**
     * Marks every row currently in AUTH_REQUIRED as PENDING again, eligible
     * for immediate retry. Called after a successful Google Drive
     * reauthorization (GoogleDriveOAuthService.exchangeCode) — AUTH_REQUIRED
     * itself never auto-retries, so without this a row that hit it would
     * stay stuck even after the underlying authorization problem is fixed.
     */
    int wakeAuthRequiredRows();
}
