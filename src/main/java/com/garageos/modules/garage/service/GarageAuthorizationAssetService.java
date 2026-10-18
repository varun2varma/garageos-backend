package com.garageos.modules.garage.service;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.media.storage.R2MediaStorageProvider;
import com.garageos.modules.navigation.storage.MediaStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The garage authorized signature and stamp printed on the final invoice PDF.
 *
 * <ul>
 *   <li><b>Private:</b> stored like the logo (private object store, or local disk fallback); no route serves the
 *       bytes - only the invoice PDF embeds them.</li>
 *   <li><b>Owner only:</b> only an OWNER of that garage may upload or replace them.</li>
 *   <li><b>Validated:</b> real image type by magic bytes (PNG / JPEG), max 2 MB, max 4096 px; the client-supplied
 *       file name and content type are never trusted.</li>
 *   <li><b>Garage-scoped:</b> the PDF loads them by the invoice job card garage id, never by the caller.</li>
 * </ul>
 * This is the garage authorization. It is unrelated to the optional customer signature.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GarageAuthorizationAssetService {

    public enum Kind {
        SIGNATURE("signature"),
        STAMP("stamp"),
        /** One image holding both the signature and the stamp; stored once and referenced by both slots. */
        COMBINED("signature and stamp");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** [combined] is true when one stored image serves as both signature and stamp (uploaded as a single file). */
    public record Status(boolean hasSignature, boolean hasStamp, boolean combined) {
        public boolean complete() {
            return hasSignature && hasStamp;
        }
    }

    static final long MAX_BYTES = 2L * 1024 * 1024;
    static final int MAX_DIMENSION = 4096;
    static final String R2_PREFIX = "GarageST/garage-authorization/";

    private final GarageRepository garageRepository;
    private final MediaStorageService mediaStorageService;
    private final R2MediaStorageProvider r2;

    @Transactional(readOnly = true)
    public Status status(GarageUserPrincipal principal, Long garageId) {

        Garage garage = find(garageId);
        authorizeOwner(principal, garage);
        return statusOf(garage);
    }

    /** True when one image serves as both signature and stamp, so the invoice must print it once. */
    @Transactional(readOnly = true)
    public boolean isCombined(Long garageId) {
        return statusOf(find(garageId)).combined();
    }

    /** Which required assets the garage still lacks (empty = complete). Internal use by the invoice PDF. */
    @Transactional(readOnly = true)
    public List<Kind> missing(Long garageId) {

        Status s = statusOf(find(garageId));
        List<Kind> out = new ArrayList<>();
        if (!s.hasSignature()) out.add(Kind.SIGNATURE);
        if (!s.hasStamp()) out.add(Kind.STAMP);
        return out;
    }

    @Transactional
    public Status upload(GarageUserPrincipal principal, Long garageId, Kind kind, MultipartFile file) {

        Garage garage = find(garageId);
        authorizeOwner(principal, garage);

        byte[] bytes = readAndValidate(file, kind);
        String extension = sniffExtension(bytes);
        String contentType = ".png".equals(extension) ? "image/png" : "image/jpeg";

        String previousSignature = garage.getSignatureStorageKey();
        String previousStamp = garage.getStampStorageKey();
        String newKey = store(garage.getId(), kind, bytes, extension, contentType);

        if (kind == Kind.COMBINED) {
            garage.setSignatureStorageKey(newKey);
            garage.setSignatureContentType(contentType);
            garage.setSignatureUpdatedAt(LocalDateTime.now());
            garage.setStampStorageKey(newKey);
            garage.setStampContentType(contentType);
            garage.setStampUpdatedAt(LocalDateTime.now());
        } else if (kind == Kind.SIGNATURE) {
            garage.setSignatureStorageKey(newKey);
            garage.setSignatureContentType(contentType);
            garage.setSignatureUpdatedAt(LocalDateTime.now());
        } else {
            garage.setStampStorageKey(newKey);
            garage.setStampContentType(contentType);
            garage.setStampUpdatedAt(LocalDateTime.now());
        }

        garageRepository.save(garage);

        // A key may be shared by both slots (combined upload): only delete an object nothing references any more.
        for (String previous : new java.util.LinkedHashSet<>(java.util.Arrays.asList(previousSignature, previousStamp))) {
            if (previous != null
                    && !previous.equals(garage.getSignatureStorageKey())
                    && !previous.equals(garage.getStampStorageKey())) {
                delete(previous);
            }
        }

        log.info("[AUTH-ASSET] {} updated. garageId={}, bytes={}", kind.label(), garage.getId(), bytes.length);

        return statusOf(garage);
    }

    /** Bytes for embedding in the invoice PDF; empty when absent or unreadable (the PDF is then refused upstream). */
    @Transactional(readOnly = true)
    public Optional<byte[]> load(Long garageId, Kind kind) {

        Garage garage = find(garageId);
        String key = kind == Kind.STAMP ? garage.getStampStorageKey() : garage.getSignatureStorageKey();

        if (key == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(key.startsWith(R2_PREFIX) ? r2.downloadBytes(key) : mediaStorageService.readBytes(key));
        } catch (RuntimeException e) {
            log.warn("[AUTH-ASSET] {} unreadable. garageId={}", kind.label(), garageId);
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------ internals

    private Status statusOf(Garage garage) {
        boolean combined = garage.getSignatureStorageKey() != null
                && garage.getSignatureStorageKey().equals(garage.getStampStorageKey());
        return new Status(garage.getSignatureStorageKey() != null, garage.getStampStorageKey() != null, combined);
    }

    private Garage find(Long garageId) {
        return garageRepository.findById(garageId)
                .orElseThrow(() -> new ResourceNotFoundException("Garage not found."));
    }

    private void authorizeOwner(GarageUserPrincipal principal, Garage garage) {

        if (principal.getRoles() == null || !principal.getRoles().contains(RoleCode.OWNER.name())) {
            throw new BusinessException("Only the garage owner can manage the invoice signature and stamp.");
        }

        boolean ownsGarage = (principal.getGarageId() != null && principal.getGarageId().equals(garage.getId()))
                || (principal.getId() != null && principal.getId().equals(garage.getOwnerUserId()));

        if (!ownsGarage) {
            // not-found, never confirming another garage exists
            throw new ResourceNotFoundException("Garage not found.");
        }
    }

    static byte[] readAndValidateBytes(byte[] bytes, String label) {

        if (bytes.length > MAX_BYTES) {
            throw new BusinessException("The " + label + " image must be 2 MB or smaller.");
        }

        if (sniffExtension(bytes) == null) {
            throw new BusinessException("The " + label + " must be a PNG or JPEG image.");
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

            if (image == null) {
                throw new BusinessException("The " + label + " is not a valid image.");
            }

            if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
                throw new BusinessException("The " + label + " dimensions must not exceed 4096 px.");
            }
        } catch (IOException e) {
            throw new BusinessException("The " + label + " is not a valid image.");
        }

        return bytes;
    }

    private byte[] readAndValidate(MultipartFile file, Kind kind) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException("The " + kind.label() + " file is required.");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("The " + kind.label() + " image must be 2 MB or smaller.");
        }

        try {
            return readAndValidateBytes(file.getBytes(), kind.label());
        } catch (IOException e) {
            throw new BusinessException("Unable to read the " + kind.label() + " file.");
        }
    }

    private String store(Long garageId, Kind kind, byte[] bytes, String extension, String contentType) {

        if (r2.isAvailable()) {
            // server-generated key: the client file name never reaches the storage path
            String key = R2_PREFIX + garageId + "/" + kind.label() + "-" + System.currentTimeMillis() + extension;
            r2.uploadBytes(key, bytes, contentType);
            return key;
        }

        return mediaStorageService.uploadBytes(bytes, "garage-authorization/" + garageId, extension);
    }

    private void delete(String key) {

        try {
            if (key.startsWith(R2_PREFIX)) {
                if (r2.isAvailable()) {
                    r2.deleteKey(key);
                }
            } else {
                mediaStorageService.delete(key);
            }
        } catch (RuntimeException e) {
            log.warn("[AUTH-ASSET] Could not delete previous asset. key={}", key);
        }
    }

    /** Real file type from magic bytes - never from the client-supplied name or content type. */
    static String sniffExtension(byte[] b) {

        if (b != null && b.length > 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return ".png";
        }

        if (b != null && b.length > 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }

        return null;
    }
}
