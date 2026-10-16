package com.garageos.modules.finalreport.service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Re-encodes a report photo as a bounded-size JPEG so a report with many
 * camera photos stays a reasonable file size. Returns null for bytes that are
 * not a decodable image (the photo is then skipped, never fatal).
 */
public final class ReportImageScaler {

    static final int MAX_EDGE_PX = 900;
    private static final float JPEG_QUALITY = 0.72f;

    private ReportImageScaler() {
    }

    public static byte[] toReportJpeg(byte[] original) {

        if (original == null || original.length == 0) {
            return null;
        }

        try {
            // Applies the photo's EXIF orientation: a portrait photo stored sideways (media not processed into an
            // upright evidence image, e.g. legacy uploads) must still be printed upright in the report.
            BufferedImage source = com.garageos.modules.media.util.ImageOrientation.decodeUpright(original);

            if (source == null) {
                return null;
            }

            int w = source.getWidth();
            int h = source.getHeight();
            double scale = Math.min(1.0, (double) MAX_EDGE_PX / Math.max(w, h));
            int tw = Math.max(1, (int) Math.round(w * scale));
            int th = Math.max(1, (int) Math.round(h * scale));

            BufferedImage rgb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, tw, th);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(source, 0, 0, tw, th, null);
            } finally {
                g.dispose();
            }

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(rgb, null, null), param);
            } finally {
                writer.dispose();
            }

            return out.toByteArray();

        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
