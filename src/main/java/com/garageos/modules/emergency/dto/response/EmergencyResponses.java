package com.garageos.modules.emergency.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Outbound shapes of the Emergency Assist API. Nothing here carries a token other than the one named. */
public final class EmergencyResponses {

    private EmergencyResponses() {
    }

    public record VerificationSent(String maskedDestination, long expiresInSeconds, long resendAfterSeconds) {
    }

    public record VerificationConfirmed(String verificationToken, long expiresInSeconds) {
    }

    /**
     * One garage this verified number may ask. {@code available=false} carries a {@code unavailableReason}
     * ({@code GARAGE_INACTIVE}, {@code OUT_OF_COVERAGE}, {@code NO_GARAGE_LOCATION}) so the app can say so; the
     * system never substitutes another garage for an unavailable default one.
     */
    public record GarageOption(Long garageId, String garageName, String city, Double distanceKm,
                               boolean defaultGarage, boolean available, String unavailableReason) {
    }

    public record GarageOptions(boolean defaultGarageMode, List<GarageOption> garages) {
    }

    /** Customer's view of a request. {@code statusToken} is present only on create / idempotent replay. */
    public record RequestView(String requestCode, String status, String assistanceType, String garageName,
                              String garageRemarks, LocalDateTime createdAt, LocalDateTime expiresAt,
                              String statusToken) {
    }

    /** Garage staff's view: the contact and place they need to respond. */
    public record GarageRequestView(Long id, String requestCode, String status, String assistanceType,
                                    String contactName, String contactMobile, String vehicleRegistration,
                                    String vehicleDescription, BigDecimal latitude, BigDecimal longitude,
                                    String address, String notes, String garageRemarks,
                                    LocalDateTime createdAt, LocalDateTime expiresAt, LocalDateTime decidedAt) {
    }
}
