package com.garageos.core.enums.media;

/**
 * The only two values {@code job_card_media.visibility} is allowed to hold
 * (see V33__enhance_job_card_media.sql's column and the string constants in
 * MediaServiceImpl). Used only at the API boundary for the visibility
 * correction endpoint's request validation — Jackson's enum binding rejects
 * any value that isn't exactly one of these two, so there is no way for a
 * caller to pass an arbitrary string through to the stored column.
 */
public enum MediaVisibility {
    CUSTOMER_VISIBLE,
    INTERNAL
}
