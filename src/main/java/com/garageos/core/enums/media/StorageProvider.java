package com.garageos.core.enums.media;

/**
 * Which backend a {@code JobCardMedia} row's bytes actually live in.
 *
 * Existing rows (created before this enum existed) default to GOOGLE_DRIVE —
 * see the V58 migration's column default — so nothing already in production
 * changes provider silently. New uploads default to R2 whenever R2 is
 * configured (see MediaServiceImpl.createUploadIntent); GOOGLE_DRIVE remains
 * a fully first-class, permanently-supported value, not a value only old
 * rows happen to have.
 */
public enum StorageProvider {

    GOOGLE_DRIVE,

    R2
}
