package com.garageos.modules.navigation.storage;

import com.garageos.core.exception.MediaException;
import com.garageos.core.exception.MediaException.MediaErrorCode;
import com.garageos.modules.media.storage.R2MediaStorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Root-cause fix for "driver photos/videos are not saved to the cloud": trip evidence was written only to the
 * application server's local disk ({@link LocalMediaStorageService}), which is neither the cloud store used by the
 * rest of the media system (Cloudflare R2) nor durable on a redeployed host.
 *
 * This is the storage the trip-evidence code now uses. When R2 is configured the bytes go to R2 under
 * {@value #CLOUD_PREFIX}; a failed upload throws (HTTP 502) and nothing is persisted, so the client reports the
 * failure and retries. When R2 is NOT configured it falls back to local disk (development/test environments), which is
 * logged loudly. Reads and deletes route by the key prefix, so rows written before this change keep working.
 */
@Slf4j
@Primary
@Service
public class CloudAwareMediaStorageService implements MediaStorageService {

    public static final String CLOUD_PREFIX = "trip-evidence/";

    private final LocalMediaStorageService local;
    private final R2MediaStorageProvider r2;

    public CloudAwareMediaStorageService(LocalMediaStorageService local, R2MediaStorageProvider r2) {
        this.local = local;
        this.r2 = r2;
    }

    @Override
    public String upload(MultipartFile file, String folder) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Media file is required");
        }

        if (!r2.isAvailable()) {
            log.warn("[MEDIA][TRIP] R2 is not configured; storing trip evidence on local disk (not durable). folder={}", folder);
            return local.upload(file, folder);
        }

        String key = CLOUD_PREFIX + folder + "/" + UUID.randomUUID() + extensionOf(file.getOriginalFilename());

        try {
            r2.uploadBytes(key, file.getBytes(),
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        } catch (IOException | RuntimeException ex) {
            throw new MediaException(MediaErrorCode.MEDIA_CLOUD_UPLOAD_FAILED,
                    "The evidence could not be saved to cloud storage. Nothing was stored; please retry.", ex);
        }

        return key;
    }

    @Override
    public String getUrl(String storageKey) {
        return isCloud(storageKey) ? "/api/v1/navigation/trips/media" : local.getUrl(storageKey);
    }

    @Override
    public byte[] readBytes(String storageKey) {

        if (!isCloud(storageKey)) {
            return local.readBytes(storageKey);
        }

        try {
            return r2.downloadBytes(storageKey);
        } catch (RuntimeException ex) {
            throw new MediaException(MediaErrorCode.MEDIA_CLOUD_UPLOAD_FAILED,
                    "The evidence could not be read from cloud storage.", ex);
        }
    }

    @Override
    public String storeEvidence(byte[] content, String contentType, String folder, String extension) {

        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Media file is required");
        }

        if (!r2.isAvailable()) {
            log.warn("[MEDIA][TRIP] R2 is not configured; storing trip evidence on local disk (not durable). folder={}", folder);
            return local.uploadBytes(content, folder, extension);
        }

        String key = CLOUD_PREFIX + folder + "/" + UUID.randomUUID() + (extension == null ? "" : extension);

        try {
            r2.uploadBytes(key, content, contentType == null ? "application/octet-stream" : contentType);
        } catch (RuntimeException ex) {
            throw new MediaException(MediaErrorCode.MEDIA_CLOUD_UPLOAD_FAILED,
                    "The evidence could not be saved to cloud storage. Nothing was stored; please retry.", ex);
        }

        return key;
    }

    @Override
    public String uploadBytes(byte[] content, String folder, String extension) {
        return local.uploadBytes(content, folder, extension);
    }

    @Override
    public void delete(String storageKey) {
        if (isCloud(storageKey)) {
            try {
                r2.deleteKey(storageKey);
            } catch (RuntimeException ex) {
                log.warn("[MEDIA][TRIP] Could not delete cloud object. key={}", storageKey, ex);
            }
        } else {
            local.delete(storageKey);
        }
    }

    public static boolean isCloud(String storageKey) {
        return storageKey != null && storageKey.startsWith(CLOUD_PREFIX);
    }

    private static String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        String ext = name.substring(dot).toLowerCase();
        String body = ext.substring(1);
        for (int i = 0; i < body.length(); i++) {
            if (!Character.isLetterOrDigit(body.charAt(i))) return "";
        }
        return body.length() <= 5 ? ext : "";
    }
}
