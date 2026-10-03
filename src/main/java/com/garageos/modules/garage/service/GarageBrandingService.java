package com.garageos.modules.garage.service;

import com.garageos.modules.garage.dto.response.GarageBrandingResponse;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import org.springframework.web.multipart.MultipartFile;

/**
 * Single place that resolves garage branding (name, contact, GSTIN, logo)
 * by garageId. Everything garage-branded - Job Card, Estimate, Invoice,
 * the customer's job-card view and invoice PDFs - goes through here so the
 * lookup is never duplicated and always keyed on the garage the document
 * belongs to (JobCard -> garageId -> Garage), never on the viewer.
 */
public interface GarageBrandingService {

    /** Authorized read: the caller's own garage, or a customer with a job card at that garage. */
    GarageBrandingResponse getBranding(GarageUserPrincipal principal, Long garageId);

    /** Authorized read of the logo bytes; throws not-found when the garage has no logo. */
    LogoContent getLogo(GarageUserPrincipal principal, Long garageId);

    /** Owner-only add/replace of the garage's logo. */
    GarageBrandingResponse updateLogo(GarageUserPrincipal principal, Long garageId, MultipartFile file);

    /** Internal resolution for server-side documents (callers authorize first). */
    GarageBrandingResponse resolve(Long garageId);

    /** Internal: logo bytes for server-side documents, or null when absent/unreadable. */
    LogoContent loadLogoOrNull(Long garageId);

    record LogoContent(byte[] bytes, String contentType) {
    }
}
