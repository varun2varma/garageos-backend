package com.garageos.modules.media.util;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * EXIF orientation handling for server-side image rendering.
 *
 * Root cause of "photos rotate after they are saved to the cloud": phone cameras store the sensor's raw pixels and
 * record the intended rotation in the EXIF Orientation tag (1..8). {@code ImageIO.read} ignores that tag and the
 * evidence/thumbnail JPEGs the server writes carry no EXIF, so every derived image showed the raw sensor orientation.
 * The fix is to apply the orientation to the pixels exactly once, at decode time, before anything is rendered. The
 * derived JPEGs are written without an Orientation tag, so it can never be applied twice.
 *
 * Orientation values (EXIF): 1 normal, 2 mirror horizontal, 3 rotate 180, 4 mirror vertical,
 * 5 transpose (mirror + rotate 270 CW), 6 rotate 90 CW, 7 transverse (mirror + rotate 90 CW), 8 rotate 270 CW.
 */
public final class ImageOrientation {

    public static final int NORMAL = 1;

    private ImageOrientation() {
    }

    /**
     * The EXIF orientation (1..8) of a JPEG, or {@link #NORMAL} when the bytes are not a JPEG, carry no EXIF, or the
     * tag is missing or out of range. Never throws on malformed input.
     */
    public static int readJpegOrientation(byte[] jpeg) {

        try {
            if (jpeg == null || jpeg.length < 4 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
                return NORMAL;
            }

            int pos = 2;

            while (pos + 4 <= jpeg.length) {

                if ((jpeg[pos] & 0xFF) != 0xFF) {
                    return NORMAL;
                }

                int marker = jpeg[pos + 1] & 0xFF;

                if (marker == 0xFF) { // fill byte
                    pos++;
                    continue;
                }

                if (marker == 0xD8 || (marker >= 0xD0 && marker <= 0xD7) || marker == 0x01) { // no length
                    pos += 2;
                    continue;
                }

                if (marker == 0xDA || marker == 0xD9) { // start of scan / end of image: no EXIF after this
                    return NORMAL;
                }

                int length = ((jpeg[pos + 2] & 0xFF) << 8) | (jpeg[pos + 3] & 0xFF);

                if (length < 2 || pos + 2 + length > jpeg.length) {
                    return NORMAL;
                }

                if (marker == 0xE1 && length >= 8) {
                    int orientation = orientationFromExifSegment(jpeg, pos + 4, length - 2);
                    if (orientation != 0) {
                        return orientation;
                    }
                }

                pos += 2 + length;
            }
        } catch (RuntimeException ex) {
            return NORMAL;
        }

        return NORMAL;
    }

    private static int orientationFromExifSegment(byte[] b, int start, int len) {

        // "Exif\0\0"
        if (len < 14 || b[start] != 'E' || b[start + 1] != 'x' || b[start + 2] != 'i' || b[start + 3] != 'f'
                || b[start + 4] != 0 || b[start + 5] != 0) {
            return 0;
        }

        int tiff = start + 6;
        int end = start + len;

        boolean little;
        if (b[tiff] == 'I' && b[tiff + 1] == 'I') {
            little = true;
        } else if (b[tiff] == 'M' && b[tiff + 1] == 'M') {
            little = false;
        } else {
            return 0;
        }

        if (u16(b, tiff + 2, little) != 42) {
            return 0;
        }

        long ifd0 = u32(b, tiff + 4, little);

        int ifd = (int) (tiff + ifd0);

        if (ifd < tiff || ifd + 2 > end) {
            return 0;
        }

        int entries = u16(b, ifd, little);

        for (int i = 0; i < entries; i++) {

            int entry = ifd + 2 + i * 12;

            if (entry + 12 > end) {
                return 0;
            }

            if (u16(b, entry, little) == 0x0112) {
                int type = u16(b, entry + 2, little);
                long count = u32(b, entry + 4, little);
                if (type != 3 || count < 1) { // SHORT
                    return 0;
                }
                int value = u16(b, entry + 8, little);
                return value >= 1 && value <= 8 ? value : 0;
            }
        }

        return 0;
    }

    private static int u16(byte[] b, int p, boolean little) {
        return little
                ? (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8)
                : ((b[p] & 0xFF) << 8) | (b[p + 1] & 0xFF);
    }

    private static long u32(byte[] b, int p, boolean little) {
        return little
                ? (b[p] & 0xFFL) | ((b[p + 1] & 0xFFL) << 8) | ((b[p + 2] & 0xFFL) << 16) | ((b[p + 3] & 0xFFL) << 24)
                : ((b[p] & 0xFFL) << 24) | ((b[p + 1] & 0xFFL) << 16) | ((b[p + 2] & 0xFFL) << 8) | (b[p + 3] & 0xFFL);
    }

    /**
     * Returns an image whose pixels are rotated/mirrored so that it displays upright, for the given EXIF orientation.
     * Orientation 1 (or any unknown value) returns the same instance untouched.
     */
    public static BufferedImage apply(BufferedImage source, int orientation) {

        if (source == null || orientation <= NORMAL || orientation > 8) {
            return source;
        }

        int w = source.getWidth();
        int h = source.getHeight();

        boolean swap = orientation >= 5; // 5..8 swap width and height

        AffineTransform t = new AffineTransform();

        switch (orientation) {
            case 2 -> { // mirror horizontal
                t.concatenate(AffineTransform.getTranslateInstance(w, 0));
                t.concatenate(AffineTransform.getScaleInstance(-1, 1));
            }
            case 3 -> { // rotate 180
                t.concatenate(AffineTransform.getTranslateInstance(w, h));
                t.concatenate(AffineTransform.getRotateInstance(Math.PI));
            }
            case 4 -> { // mirror vertical
                t.concatenate(AffineTransform.getTranslateInstance(0, h));
                t.concatenate(AffineTransform.getScaleInstance(1, -1));
            }
            case 5 -> { // transpose
                t.concatenate(AffineTransform.getRotateInstance(Math.PI / 2));
                t.concatenate(AffineTransform.getScaleInstance(1, -1));
            }
            case 6 -> { // rotate 90 CW
                t.concatenate(AffineTransform.getTranslateInstance(h, 0));
                t.concatenate(AffineTransform.getRotateInstance(Math.PI / 2));
            }
            case 7 -> { // transverse
                t.concatenate(AffineTransform.getTranslateInstance(h, w));
                t.concatenate(AffineTransform.getRotateInstance(Math.PI / 2));
                t.concatenate(AffineTransform.getScaleInstance(-1, 1));
            }
            case 8 -> { // rotate 270 CW
                t.concatenate(AffineTransform.getTranslateInstance(0, w));
                t.concatenate(AffineTransform.getRotateInstance(-Math.PI / 2));
            }
            default -> {
                return source;
            }
        }

        int outW = swap ? h : w;
        int outH = swap ? w : h;

        BufferedImage out = new BufferedImage(outW, outH,
                source.getType() == BufferedImage.TYPE_CUSTOM || source.getType() == 0
                        ? BufferedImage.TYPE_INT_RGB
                        : source.getType());

        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(source, t, null);
        } finally {
            g.dispose();
        }

        return out;
    }

    /** Decodes {@code bytes} with ImageIO and applies the EXIF orientation of the same bytes. Null when undecodable. */
    public static BufferedImage decodeUpright(byte[] bytes) throws java.io.IOException {

        BufferedImage decoded = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));

        if (decoded == null) {
            return null;
        }

        return apply(decoded, readJpegOrientation(bytes));
    }

    /** Encodes {@code image} as a JPEG at the given quality (0..1) with no EXIF, so no orientation tag survives. */
    public static byte[] encodeJpeg(BufferedImage image, float quality) throws java.io.IOException {

        BufferedImage rgb = image;

        if (image.getType() != BufferedImage.TYPE_INT_RGB && image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            try {
                g.drawImage(image, 0, 0, java.awt.Color.WHITE, null);
            } finally {
                g.dispose();
            }
        }

        javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpeg").next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }

        return out.toByteArray();
    }
}
