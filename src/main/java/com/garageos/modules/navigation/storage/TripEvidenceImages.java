package com.garageos.modules.navigation.storage;

import com.garageos.modules.media.util.EvidenceImageOps;
import com.garageos.modules.media.util.ImageOrientation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Produces the canonical pair for pickup and delivery photos from the original uploaded bytes:
 * <ul>
 *   <li><b>evidence</b>: the upright JPEG. An already-upright JPEG is kept byte-for-byte (nothing is re-compressed);
 *       a photo that carries an EXIF orientation, or is not a JPEG, is decoded upright and re-encoded once. No text is
 *       burned into it: it is the photo as taken;</li>
 *   <li><b>thumbnail</b>: a genuine downscaled JPEG, longest edge at most {@value EvidenceImageOps#THUMBNAIL_MAX_DIMENSION}
 *       px, made with the same resize and JPEG writer as job-card media.</li>
 * </ul>
 * Pure and deterministic for the same input, so a retry produces identical evidence bytes.
 */
public final class TripEvidenceImages {

    public record Rendition(byte[] evidence, byte[] thumbnail, int evidenceWidth, int evidenceHeight,
                            int thumbnailWidth, int thumbnailHeight) {
    }

    private TripEvidenceImages() {
    }

    /** @throws IOException when the bytes are not a decodable image */
    public static Rendition render(byte[] original) throws IOException {

        BufferedImage upright = ImageOrientation.decodeUpright(original);

        if (upright == null) {
            throw new IOException("Not a decodable image.");
        }

        boolean untouched = isJpeg(original) && ImageOrientation.readJpegOrientation(original) == ImageOrientation.NORMAL;

        byte[] evidence = untouched ? original : ImageOrientation.encodeJpeg(upright, 0.92f);

        BufferedImage thumbnail = EvidenceImageOps.resize(upright, EvidenceImageOps.THUMBNAIL_MAX_DIMENSION);
        ByteArrayOutputStream thumbnailBytes = new ByteArrayOutputStream();
        EvidenceImageOps.writeJpeg(thumbnail, thumbnailBytes);

        return new Rendition(evidence, thumbnailBytes.toByteArray(), upright.getWidth(), upright.getHeight(),
                thumbnail.getWidth(), thumbnail.getHeight());
    }

    /** Dimensions of a stored image, or null when it cannot be decoded (used to verify a thumbnail). */
    public static int[] dimensions(byte[] image) {
        try {
            BufferedImage decoded = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(image));
            return decoded == null ? null : new int[]{decoded.getWidth(), decoded.getHeight()};
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static boolean isJpeg(byte[] b) {
        return b != null && b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
    }
}
