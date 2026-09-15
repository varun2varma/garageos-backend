package com.garageos.modules.media.service;

/**
 * In-memory holder for a downloaded media file's bytes plus the metadata
 * needed to build the HTTP response (Content-Type, filename). Used by the
 * authenticated media-content proxy endpoints (employee and customer sides)
 * so neither controller has to know how the bytes were actually fetched
 * from Google Drive.
 */
public record MediaContent(byte[] content, String contentType, String fileName) {
}
