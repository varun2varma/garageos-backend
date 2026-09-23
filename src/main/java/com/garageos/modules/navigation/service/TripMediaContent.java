package com.garageos.modules.navigation.service;

/**
 * In-memory holder for a downloaded trip-evidence file's bytes plus the
 * metadata needed to build the HTTP response — same shape and purpose as
 * com.garageos.modules.media.service.MediaContent for job-card media, kept
 * as a separate type since these are two distinct aggregates (see
 * NavigationTripMediaService's own doc comment).
 */
public record TripMediaContent(byte[] content, String contentType, String fileName) {
}
