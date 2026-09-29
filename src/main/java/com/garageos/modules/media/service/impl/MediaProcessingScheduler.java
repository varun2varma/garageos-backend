package com.garageos.modules.media.service.impl;

import com.garageos.core.enums.media.MediaUploadStatus;
import com.garageos.core.enums.media.StorageProvider;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.repository.JobCardMediaRepository;
import com.garageos.modules.media.storage.MediaStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Drives R2-backed media from UPLOADED to COMPLETED, mirroring
 * {@link MediaUploadRetryScheduler}'s own fixed-delay-poll pattern (the only
 * other {@code @Scheduled} job in this application).
 *
 * Scope note (deliberate, Phase 2/3): generates a real thumbnail AND a
 * derived "evidence" image (original + a subtle metadata footer — location/
 * time/captured-by/stage) for IMAGE rows only, using the JDK's own
 * {@code javax.imageio}/{@code java.awt} — no new dependency. The overlay is
 * drawn with plain text labels rather than emoji glyphs deliberately: a
 * headless server JVM (Render) has no guarantee of an emoji-capable font
 * installed, and a missing-glyph "tofu" box would look worse than a plain
 * label. VIDEO thumbnail/evidence generation (extracting a representative
 * frame) needs a real media-processing library (e.g. FFmpeg) that does not
 * exist in this codebase and is NOT added here without explicit approval —
 * video rows are finalized without either, exactly as before. Neither
 * derived asset EVER modifies {@code original} — both are separate R2
 * objects; a failure generating either (corrupt image, unsupported format,
 * oversized file, transient storage error) never blocks finalization — the
 * original is already durably stored and fully servable regardless.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaProcessingScheduler {

    /** Longest edge of a generated thumbnail, in pixels. */
    private static final int THUMBNAIL_MAX_DIMENSION = 320;

    /** Skip thumbnail generation above this size — not worth holding in memory for a grid tile. */
    private static final long THUMBNAIL_SOURCE_MAX_BYTES = 20L * 1024 * 1024;

    private final JobCardMediaRepository jobCardMediaRepository;
    private final List<MediaStorageProvider> mediaStorageProviders;

    @Scheduled(fixedDelay = 15_000L)
    public void processUploadedMedia() {

        List<JobCardMedia> eligible =
                jobCardMediaRepository.findByStorageProviderAndUploadStatus(
                        StorageProvider.R2.name(),
                        MediaUploadStatus.UPLOADED.name()
                );

        if (eligible.isEmpty()) {
            return;
        }

        log.info(
                "[MEDIA_PROCESSING_SCHEDULER] {} R2 media row(s) ready to finalize.",
                eligible.size()
        );

        for (JobCardMedia media : eligible) {

            try {

                if ("IMAGE".equals(media.getMediaType())) {
                    generateImageThumbnail(media);
                }
                // VIDEO: duration is captured client-side at upload-complete
                // time (see MediaServiceImpl.completeUpload); no thumbnail
                // frame is extracted — see this class's own doc comment.

                media.setUploadStatus(MediaUploadStatus.COMPLETED.name());

                jobCardMediaRepository.save(media);

                log.info(
                        "[MEDIA_PROCESSING_SCHEDULER] Media finalized. mediaId={}, jobCardId={}, hasThumbnail={}",
                        media.getId(),
                        media.getJobCardId(),
                        media.getThumbnailKey() != null
                );

            } catch (Exception ex) {

                log.error(
                        "[MEDIA_PROCESSING_SCHEDULER] Failed to finalize mediaId={}. error={}",
                        media.getId(),
                        ex.getMessage(),
                        ex
                );
            }
        }
    }

    /**
     * Best-effort, in two independent try/catch blocks so a failure
     * generating one derived asset never prevents the other — any failure
     * here (corrupt/unsupported image, oversized source, storage error) is
     * caught and logged; the media is still finalized (COMPLETED) without
     * the missing asset(s) rather than left stuck.
     */
    private void generateImageThumbnail(JobCardMedia media) {

        if (media.getFileSize() != null && media.getFileSize() > THUMBNAIL_SOURCE_MAX_BYTES) {

            log.info(
                    "[MEDIA_PROCESSING_SCHEDULER] Skipping thumbnail/evidence — source too large. mediaId={}, size={}",
                    media.getId(),
                    media.getFileSize()
            );
            return;
        }

        BufferedImage sourceImage;
        MediaStorageProvider r2;

        try {

            r2 = resolveProvider(StorageProvider.R2);

            byte[] original = r2.downloadBytes(media.getStorageKey());

            sourceImage = ImageIO.read(new ByteArrayInputStream(original));

            if (sourceImage == null) {
                log.warn(
                        "[MEDIA_PROCESSING_SCHEDULER] Source is not a decodable image — skipping thumbnail/evidence. mediaId={}",
                        media.getId()
                );
                return;
            }

        } catch (IOException | RuntimeException ex) {

            log.warn(
                    "[MEDIA_PROCESSING_SCHEDULER] Could not read source image, finalizing without derived assets. mediaId={}, error={}",
                    media.getId(),
                    ex.getMessage()
            );
            return;
        }

        try {

            BufferedImage thumbnail = resize(sourceImage, THUMBNAIL_MAX_DIMENSION);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", output);

            String thumbnailKey = deriveDerivedKey(media.getStorageKey(), "thumbnail.jpg");

            r2.uploadBytes(thumbnailKey, output.toByteArray(), "image/jpeg");

            media.setThumbnailKey(thumbnailKey);

        } catch (IOException | RuntimeException ex) {

            log.warn(
                    "[MEDIA_PROCESSING_SCHEDULER] Thumbnail generation failed, finalizing without one. mediaId={}, error={}",
                    media.getId(),
                    ex.getMessage()
            );
        }

        try {

            BufferedImage evidence = renderEvidenceImage(sourceImage, media);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(evidence, "jpg", output);

            String evidenceKey = deriveDerivedKey(media.getStorageKey(), "evidence.jpg");

            r2.uploadBytes(evidenceKey, output.toByteArray(), "image/jpeg");

            media.setEvidenceKey(evidenceKey);

        } catch (IOException | RuntimeException ex) {

            log.warn(
                    "[MEDIA_PROCESSING_SCHEDULER] Evidence image generation failed, finalizing without one. mediaId={}, error={}",
                    media.getId(),
                    ex.getMessage()
            );
        }
    }

    /**
     * Original + a subtle bottom gradient footer with capture metadata —
     * never a modification of the original object itself (this writes to a
     * brand-new {@code evidence.jpg} key; {@code original} is only ever
     * read, never rewritten). Text is drawn plainly (no emoji — see this
     * class's own doc comment) and only includes fields that are actually
     * present; a photo with no location data simply gets a shorter footer,
     * never a fabricated "Location unavailable" placeholder photographed as
     * if it were real evidence.
     */
    private BufferedImage renderEvidenceImage(BufferedImage source, JobCardMedia media) {

        int width = source.getWidth();
        int height = source.getHeight();

        List<String> lines = buildEvidenceLines(media);

        int lineHeight = Math.max(16, height / 40);
        int padding = lineHeight / 2;
        int footerHeight = lines.isEmpty() ? 0 : (lines.size() * lineHeight) + (padding * 2);

        BufferedImage evidence = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = evidence.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, null);

            if (footerHeight > 0) {

                // Subtle bottom gradient (transparent -> dark), not a solid
                // block — tasteful rather than a "cheap watermark" look.
                GradientPaint gradient = new GradientPaint(
                        0, height - footerHeight - lineHeight, new Color(0, 0, 0, 0),
                        0, height, new Color(0, 0, 0, 160)
                );
                g.setPaint(gradient);
                g.fillRect(0, height - footerHeight - lineHeight, width, footerHeight + lineHeight);

                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.PLAIN, (int) (lineHeight * 0.7)));

                int y = height - footerHeight + lineHeight - (lineHeight / 3);
                for (String line : lines) {
                    g.drawString(line, padding, y);
                    y += lineHeight;
                }

                // Small brand mark, bottom-right — one line, understated.
                g.setFont(new Font("SansSerif", Font.BOLD, (int) (lineHeight * 0.65)));
                String brand = "GarageST";
                int brandWidth = g.getFontMetrics().stringWidth(brand);
                g.drawString(brand, width - brandWidth - padding, height - padding);
            }

        } finally {
            g.dispose();
        }

        return evidence;
    }

    private static final DateTimeFormatter EVIDENCE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private List<String> buildEvidenceLines(JobCardMedia media) {

        List<String> lines = new ArrayList<>();

        if (media.getMediaStage() != null) {
            lines.add(stageLabel(media.getMediaStage()));
        }

        if (media.getCapturedAt() != null) {
            lines.add("Captured: " + EVIDENCE_TIMESTAMP_FORMAT.format(media.getCapturedAt()));
        }

        if (media.getCapturedByNameSnapshot() != null) {
            lines.add("By: " + media.getCapturedByNameSnapshot());
        }

        if (media.getLocationName() != null) {
            lines.add("Location: " + media.getLocationName());
        } else if (media.getLatitude() != null && media.getLongitude() != null) {
            lines.add(String.format(Locale.ENGLISH, "Location: %.4f, %.4f", media.getLatitude(), media.getLongitude()));
        }

        return lines;
    }

    private String stageLabel(String stage) {
        return switch (stage) {
            case "BEFORE_SERVICE" -> "Before Service";
            case "DURING_REPAIR" -> "During Repair";
            case "AFTER_REPAIR" -> "After Repair";
            default -> stage;
        };
    }

    private BufferedImage resize(BufferedImage source, int maxDimension) {

        int width = source.getWidth();
        int height = source.getHeight();

        double scale = Math.min(1.0, (double) maxDimension / Math.max(width, height));

        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        return resized;
    }

    /** "garage/.../media/{id}/original.jpg" -> "garage/.../media/{id}/{fileName}" */
    private String deriveDerivedKey(String storageKey, String fileName) {
        int lastSlash = storageKey.lastIndexOf('/');
        return storageKey.substring(0, lastSlash + 1) + fileName;
    }

    private MediaStorageProvider resolveProvider(StorageProvider type) {
        return mediaStorageProviders.stream()
                .filter(provider -> provider.getProviderType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No MediaStorageProvider registered for " + type));
    }
}
