package com.garageos.modules.media.util;

/**
 * Derives the sibling R2 object keys of one media item under the
 * human-readable convention {@code GarageST/{garage}/{jobCard}/{stage}/
 * {jobCard}_{stage}_{seq}[_role].ext}. The role suffix (_source, _evidence,
 * _thumbnail) is inserted before the extension; an existing role suffix is
 * stripped first so every sibling of one item derives from the same base and
 * never collides with another item in the same folder.
 */
public final class MediaKeys {

    public static final String SOURCE = "source";
    public static final String EVIDENCE = "evidence";
    public static final String THUMBNAIL = "thumbnail";

    private MediaKeys() {
    }

    /** e.g. ".../X_001_evidence.jpg" + THUMBNAIL, "jpg" -> ".../X_001_thumbnail.jpg". */
    public static String derive(String storageKey, String role, String extension) {

        int lastSlash = storageKey.lastIndexOf('/');
        String directory = storageKey.substring(0, lastSlash + 1);
        String fileName = storageKey.substring(lastSlash + 1);

        int lastDot = fileName.lastIndexOf('.');
        String stem = lastDot >= 0 ? fileName.substring(0, lastDot) : fileName;

        for (String known : new String[]{SOURCE, EVIDENCE, THUMBNAIL}) {
            String suffix = "_" + known;
            if (stem.endsWith(suffix)) {
                stem = stem.substring(0, stem.length() - suffix.length());
                break;
            }
        }

        return directory + stem + "_" + role + "." + extension;
    }
}
