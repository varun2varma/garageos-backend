package com.garageos.modules.media.storage;

import com.garageos.core.enums.media.StorageProvider;
import com.garageos.core.exception.MediaException;
import com.garageos.modules.media.config.R2Properties;
import com.garageos.modules.media.entity.JobCardMedia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cloudflare R2 (S3-compatible) implementation of {@link MediaStorageProvider}.
 *
 * R2 is accessed entirely through the S3 API — no Cloudflare-specific SDK is
 * needed, just an S3 client pointed at R2's per-account endpoint with the
 * "auto" region R2 expects. The bucket is assumed PRIVATE (never configured
 * for public read) — every access, upload or playback, is a short-lived
 * presigned URL generated here, never a static public link.
 *
 * Never logs {@link R2Properties#getAccessKeyId()}/{@link R2Properties#getSecretAccessKey()}
 * or a full presigned URL (a presigned URL IS a bearer credential for that
 * one object) — only the object key and outcome are logged.
 *
 * The S3 client/presigner are built lazily and re-checked on every call via
 * {@link R2Properties#isConfigured()} rather than at application startup, so
 * the app starts cleanly before R2 credentials exist (per this feature's own
 * requirement) — a call made before configuration throws a clear
 * {@link MediaException} naming the missing environment variable, never a
 * silent no-op.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class R2MediaStorageProvider implements MediaStorageProvider {

    private final R2Properties properties;

    private volatile S3Client s3Client;
    private volatile S3Presigner presigner;

    @Override
    public StorageProvider getProviderType() {
        return StorageProvider.R2;
    }

    @Override
    public boolean isAvailable() {
        return properties.isConfigured();
    }

    @Override
    public UploadAuthorization createUploadAuthorization(String storageKey, String contentType) {

        requireConfigured();

        Duration ttl = Duration.ofMinutes(properties.getUploadUrlTtlMinutes());

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(storageKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presigned = presigner().presignPutObject(presignRequest);

        log.info(
                "[MEDIA][R2] Upload authorization issued. storageKey={}, ttlMinutes={}",
                storageKey,
                properties.getUploadUrlTtlMinutes()
        );

        return UploadAuthorization.builder()
                .uploadUrl(presigned.url().toString())
                .method("PUT")
                .requiredHeaders(Map.of("Content-Type", contentType))
                .storageKey(storageKey)
                .expiresAt(LocalDateTime.now().plus(ttl))
                .build();
    }

    @Override
    public Long confirmUpload(String storageKey) {

        requireConfigured();

        try {

            HeadObjectResponse head = s3Client().headObject(
                    HeadObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(storageKey)
                            .build()
            );

            log.info(
                    "[MEDIA][R2] Upload confirmed. storageKey={}, size={}",
                    storageKey,
                    head.contentLength()
            );

            return head.contentLength();

        } catch (NoSuchKeyException ex) {

            log.warn(
                    "[MEDIA][R2] Completion reported but object does not exist. storageKey={}",
                    storageKey
            );

            throw new MediaException(
                    MediaException.MediaErrorCode.MEDIA_INVALID_REQUEST,
                    "The uploaded file could not be found in storage. Please upload again."
            );
        }
    }

    @Override
    public PlaybackAccess createPlaybackAccess(JobCardMedia media) {

        requireConfigured();

        Duration ttl = Duration.ofMinutes(properties.getPlaybackUrlTtlMinutes());

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(
                        GetObjectRequest.builder()
                                .bucket(properties.getBucketName())
                                .key(media.getStorageKey())
                                .build()
                )
                .build();

        PresignedGetObjectRequest presigned = presigner().presignGetObject(presignRequest);

        log.info(
                "[MEDIA][R2] Playback access issued. mediaId={}, ttlMinutes={}",
                media.getId(),
                properties.getPlaybackUrlTtlMinutes()
        );

        return PlaybackAccess.builder()
                .url(presigned.url().toString())
                .direct(true)
                .headers(Map.of())
                .expiresAt(LocalDateTime.now().plus(ttl))
                .available(true)
                .build();
    }

    /** Same as {@link #createPlaybackAccess(JobCardMedia)} but signing {@code storageKey} directly, for a thumbnail object. */
    public PlaybackAccess createPlaybackAccessForKey(String storageKey) {

        requireConfigured();

        Duration ttl = Duration.ofMinutes(properties.getPlaybackUrlTtlMinutes());

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(
                        GetObjectRequest.builder()
                                .bucket(properties.getBucketName())
                                .key(storageKey)
                                .build()
                )
                .build();

        PresignedGetObjectRequest presigned = presigner().presignGetObject(presignRequest);

        return PlaybackAccess.builder()
                .url(presigned.url().toString())
                .direct(true)
                .headers(Map.of())
                .expiresAt(LocalDateTime.now().plus(ttl))
                .available(true)
                .build();
    }

    @Override
    public void delete(JobCardMedia media) {

        requireConfigured();

        if (media.getStorageKey() == null) {
            return;
        }

        s3Client().deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(properties.getBucketName())
                        .key(media.getStorageKey())
                        .build()
        );

        log.info("[MEDIA][R2] Object deleted. storageKey={}", media.getStorageKey());
    }

    @Override
    public byte[] downloadBytes(String storageKey) {

        requireConfigured();

        ResponseBytes<GetObjectResponse> response = s3Client().getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket(properties.getBucketName())
                        .key(storageKey)
                        .build()
        );

        return response.asByteArray();
    }

    @Override
    public void uploadBytes(String storageKey, byte[] content, String contentType) {

        requireConfigured();

        s3Client().putObject(
                PutObjectRequest.builder()
                        .bucket(properties.getBucketName())
                        .key(storageKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content)
        );

        log.info("[MEDIA][R2] Derived asset uploaded. storageKey={}, size={}", storageKey, content.length);
    }

    @Override
    public boolean exists(String storageKey) {

        requireConfigured();

        try {
            s3Client().headObject(
                    HeadObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(storageKey)
                            .build()
            );
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        }
    }

    private void requireConfigured() {

        if (!properties.isConfigured()) {

            String missing = properties.missingVariableName();

            log.warn("[MEDIA][R2] R2 is not configured. missingVariable={}", missing);

            throw new MediaException(
                    MediaException.MediaErrorCode.MEDIA_STORAGE_NOT_CONFIGURED,
                    missing + " is not configured. New media uploads are unavailable until R2 is set up."
            );
        }
    }

    private S3Client s3Client() {

        if (s3Client == null) {
            synchronized (this) {
                if (s3Client == null) {
                    s3Client = S3Client.builder()
                            .region(Region.of("auto"))
                            .endpointOverride(URI.create(properties.endpoint()))
                            .credentialsProvider(credentialsProvider())
                            .build();
                }
            }
        }

        return s3Client;
    }

    private S3Presigner presigner() {

        if (presigner == null) {
            synchronized (this) {
                if (presigner == null) {
                    presigner = S3Presigner.builder()
                            .region(Region.of("auto"))
                            .endpointOverride(URI.create(properties.endpoint()))
                            .credentialsProvider(credentialsProvider())
                            .build();
                }
            }
        }

        return presigner;
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey())
        );
    }
}
