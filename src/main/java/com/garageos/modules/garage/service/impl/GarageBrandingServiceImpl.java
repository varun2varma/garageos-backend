package com.garageos.modules.garage.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.garage.dto.response.GarageBrandingResponse;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.garage.service.GarageBrandingService;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.jobcard.repository.JobCardRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class GarageBrandingServiceImpl implements GarageBrandingService {

    /** Logos are small marks, not photos. */
    static final long MAX_LOGO_BYTES = 2L * 1024 * 1024;

    static final int MAX_LOGO_DIMENSION = 4096;

    private final GarageRepository garageRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final JobCardRepository jobCardRepository;
    private final GarageMembershipRepository garageMembershipRepository;
    private final MediaStorageService mediaStorageService;
    private final R2MediaStorageProvider r2;

    /**
     * Keys under this prefix live in the persistent Cloudflare R2 bucket (the
     * same private bucket job-card media uses); anything else is a key in the
     * local-disk MediaStorageService (development / R2 not yet configured).
     * Production runs on Render with no persistent disk, so R2 is what keeps
     * logos across restarts and redeploys.
     */
    static final String R2_PREFIX = "GarageST/garage-logos/";

    @Override
    @Transactional(readOnly = true)
    public GarageBrandingResponse getBranding(GarageUserPrincipal principal, Long garageId) {

        Garage garage = findGarage(garageId);

        authorizeRead(principal, garage);

        return toBranding(garage);
    }

    @Override
    @Transactional(readOnly = true)
    public LogoContent getLogo(GarageUserPrincipal principal, Long garageId) {

        Garage garage = findGarage(garageId);

        authorizeRead(principal, garage);

        LogoContent logo = readLogo(garage);

        if (logo == null) {
            throw new ResourceNotFoundException("This garage has no logo.");
        }

        return logo;
    }

    @Override
    @Transactional
    public GarageBrandingResponse updateLogo(
            GarageUserPrincipal principal,
            Long garageId,
            MultipartFile file) {

        Garage garage = findGarage(garageId);

        authorizeWrite(principal, garage);

        byte[] bytes = readAndValidate(file);

        String extension = sniffExtension(bytes);

        String previousKey = garage.getLogoStorageKey();

        String contentType = ".png".equals(extension) ? "image/png" : "image/jpeg";

        String newKey = storeLogo(garage.getId(), bytes, extension, contentType);

        garage.setLogoStorageKey(newKey);
        garage.setLogoContentType(contentType);
        garage.setLogoUpdatedAt(LocalDateTime.now());

        garageRepository.save(garage);

        if (previousKey != null && !previousKey.equals(newKey)) {
            deleteLogo(previousKey);
        }

        log.info("[BRANDING] Garage logo updated. garageId={}, bytes={}", garage.getId(), bytes.length);

        return toBranding(garage);
    }

    @Override
    @Transactional(readOnly = true)
    public GarageBrandingResponse resolve(Long garageId) {

        return toBranding(findGarage(garageId));
    }

    @Override
    @Transactional(readOnly = true)
    public LogoContent loadLogoOrNull(Long garageId) {

        return readLogo(findGarage(garageId));
    }

    private Garage findGarage(Long garageId) {

        return garageRepository.findById(garageId)
                .orElseThrow(() -> new ResourceNotFoundException("Garage not found."));
    }

    /**
     * A garage's branding is shown to its own staff and to customers who
     * actually have a job card there. Anyone else gets the same not-found
     * used elsewhere so another garage's existence is not confirmed.
     */
    private void authorizeRead(GarageUserPrincipal principal, Garage garage) {

        if (principal.getGarageId() != null
                && principal.getGarageId().equals(garage.getId())) {
            return;
        }

        if (principal.getId() != null
                && garageMembershipRepository.existsByGarage_IdAndUser_Id(
                garage.getId(), principal.getId())) {
            return;
        }

        if (principal.getMobile() != null) {

            Customer customer = customerRepository
                    .findByMobileNumber(principal.getMobile())
                    .orElse(null);

            if (customer != null
                    && jobCardRepository.existsByGarage_IdAndCustomer_Id(
                    garage.getId(), customer.getId())) {
                return;
            }
        }

        throw new ResourceNotFoundException("Garage not found.");
    }

    /** Only an OWNER of THIS garage may change its branding. */
    private void authorizeWrite(GarageUserPrincipal principal, Garage garage) {

        if (principal.getRoles() == null
                || !principal.getRoles().contains(RoleCode.OWNER.name())) {
            throw new BusinessException("Only the garage owner can change the garage logo.");
        }

        boolean ownsGarage =
                (principal.getGarageId() != null
                        && principal.getGarageId().equals(garage.getId()))
                        || (principal.getId() != null
                        && principal.getId().equals(garage.getOwnerUserId()));

        if (!ownsGarage) {
            throw new BusinessException("This garage does not belong to you.");
        }
    }

    private byte[] readAndValidate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Logo file is required.");
        }

        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new BusinessException("Logo must be 2 MB or smaller.");
        }

        byte[] bytes;

        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Unable to read the logo file.");
        }

        if (bytes.length > MAX_LOGO_BYTES) {
            throw new BusinessException("Logo must be 2 MB or smaller.");
        }

        if (sniffExtension(bytes) == null) {
            throw new BusinessException("Logo must be a PNG or JPEG image.");
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

            if (image == null) {
                throw new BusinessException("Logo is not a valid image.");
            }

            if (image.getWidth() > MAX_LOGO_DIMENSION || image.getHeight() > MAX_LOGO_DIMENSION) {
                throw new BusinessException("Logo dimensions must not exceed 4096 px.");
            }

        } catch (IOException e) {
            throw new BusinessException("Logo is not a valid image.");
        }

        return bytes;
    }

    /** Real file type from magic bytes - never from the client-supplied name/content-type. */
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

    private LogoContent readLogo(Garage garage) {

        if (garage.getLogoStorageKey() == null) {
            return null;
        }

        try {
            byte[] bytes = garage.getLogoStorageKey().startsWith(R2_PREFIX)
                    ? r2.downloadBytes(garage.getLogoStorageKey())
                    : mediaStorageService.readBytes(garage.getLogoStorageKey());

            return new LogoContent(
                    bytes,
                    garage.getLogoContentType() != null
                            ? garage.getLogoContentType()
                            : "image/png");

        } catch (RuntimeException e) {
            // A missing/unreadable file must never break the app or a PDF:
            // behave exactly like a garage with no logo.
            log.warn("[BRANDING] Logo unreadable, falling back to default branding. garageId={}", garage.getId());
            return null;
        }
    }

    private String storeLogo(Long garageId, byte[] bytes, String extension, String contentType) {

        if (r2.isAvailable()) {
            String key = R2_PREFIX + garageId + "/logo-" + System.currentTimeMillis() + extension;
            r2.uploadBytes(key, bytes, contentType);
            return key;
        }

        return mediaStorageService.uploadBytes(bytes, "garage-logos/" + garageId, extension);
    }

    private void deleteLogo(String key) {

        try {
            if (key.startsWith(R2_PREFIX)) {
                if (r2.isAvailable()) {
                    r2.deleteKey(key);
                }
            } else {
                mediaStorageService.delete(key);
            }
        } catch (RuntimeException e) {
            // Best-effort: an orphaned old logo object is harmless.
            log.warn("[BRANDING] Could not delete previous logo. key={}", key);
        }
    }

    private GarageBrandingResponse toBranding(Garage garage) {

        String phone = null;
        String email = null;

        if (garage.getOwnerUserId() != null) {
            User owner = userRepository.findById(garage.getOwnerUserId()).orElse(null);
            if (owner != null) {
                phone = owner.getMobile();
                email = owner.getEmail();
            }
        }

        boolean hasLogo = garage.getLogoStorageKey() != null;

        return GarageBrandingResponse.builder()
                .garageId(garage.getId())
                .garageCode(garage.getGarageCode())
                .garageName(garage.getGarageName())
                .address(garage.getAddress())
                .landmark(garage.getLandmark())
                .city(garage.getCity())
                .state(garage.getState())
                .pincode(garage.getPincode())
                .phone(phone)
                .email(email)
                .gstNumber(garage.getGstNumber())
                .hasLogo(hasLogo)
                .logoUpdatedAt(garage.getLogoUpdatedAt())
                .logoUrl(hasLogo ? "/api/v1/garages/" + garage.getId() + "/logo" : null)
                .build();
    }
}
