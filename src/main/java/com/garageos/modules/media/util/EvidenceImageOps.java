package com.garageos.modules.media.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The resize and JPEG-write steps of the evidence pipeline, shared so job-card media
 * ({@code MediaProcessingScheduler}) and pickup / delivery evidence produce thumbnails the same way. The behaviour is
 * exactly what the scheduler always did: bilinear scaling that never enlarges, default JPEG writer settings.
 */
public final class EvidenceImageOps {

    /** Longest edge of a generated thumbnail, in pixels. */
    public static final int THUMBNAIL_MAX_DIMENSION = 320;

    private EvidenceImageOps() {
    }

    public static BufferedImage resize(BufferedImage source, int maxDimension) {

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

    public static void writeJpeg(BufferedImage image, ByteArrayOutputStream out) throws IOException {
        if (!ImageIO.write(image, "jpg", out)) {
            throw new IOException("No JPEG writer available.");
        }
    }
}
