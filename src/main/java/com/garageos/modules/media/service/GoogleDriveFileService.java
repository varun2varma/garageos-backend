package com.garageos.modules.media.service;

import com.google.api.services.drive.model.File;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface GoogleDriveFileService {

    File uploadFile(
            MultipartFile multipartFile,
            String fileName,
            String parentFolderId
    ) throws GeneralSecurityException, IOException;

    /**
     * Same upload as {@link #uploadFile}, from bytes already durably
     * buffered on local storage rather than a live {@link MultipartFile}.
     * Needed because a MultipartFile's backing temp file does not survive
     * past the original HTTP request — a retry attempt (synchronous
     * refresh-and-retry, or the scheduled backoff retry) runs after that
     * request has already completed, so it must re-upload from the copy
     * MediaServiceImpl persisted via MediaStorageService instead.
     */
    File uploadBytes(
            byte[] content,
            String contentType,
            String fileName,
            String parentFolderId
    ) throws GeneralSecurityException, IOException;

    /**
     * Downloads the full byte content of a previously-uploaded Drive file.
     *
     * Note (MVP scope): this reads the entire file into memory and does not
     * support HTTP Range requests, so large videos are not partially
     * streamed either to this server or on to the client. Sufficient for
     * playback of typical job-card media, but flagged as a known limitation
     * rather than silently accepted as complete.
     */
    byte[] downloadFile(
            String driveFileId
    ) throws GeneralSecurityException, IOException;
}