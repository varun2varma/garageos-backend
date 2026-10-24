package com.garageos.modules.emergency.dto.request;

import com.garageos.core.enums.emergency.EmergencyAssistanceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Inbound bodies of the Emergency Assist API. */
public final class EmergencyRequests {

    private EmergencyRequests() {
    }

    @Getter
    @Setter
    public static class SendVerification {
        @NotBlank(message = "Mobile number is required.")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number.")
        private String mobile;
    }

    @Getter
    @Setter
    public static class ConfirmVerification {
        @NotBlank(message = "Mobile number is required.")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number.")
        private String mobile;

        @NotBlank(message = "OTP is required.")
        @Pattern(regexp = "^\\d{4,8}$", message = "Invalid OTP.")
        private String otp;
    }

    /** Where help is needed; used to compute the garages this verified number is eligible to ask. */
    @Getter
    @Setter
    public static class SearchGarages {
        @NotBlank(message = "Verification is required.")
        private String verificationToken;

        @NotNull(message = "Latitude is required.")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.")
        private BigDecimal latitude;

        @NotNull(message = "Longitude is required.")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.")
        private BigDecimal longitude;
    }

    @Getter
    @Setter
    public static class Create {
        @NotBlank(message = "Verification is required.")
        private String verificationToken;

        @NotBlank(message = "Name is required.")
        @Size(min = 2, max = 100, message = "Name must be 2 to 100 characters.")
        private String contactName;

        @Email(message = "Invalid email.")
        @Size(max = 150)
        private String contactEmail;

        @Size(max = 30)
        private String vehicleRegistration;

        @Size(max = 200)
        private String vehicleDescription;

        @NotNull(message = "Type of assistance is required.")
        private EmergencyAssistanceType assistanceType;

        @NotNull(message = "Location is required.")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.")
        private BigDecimal latitude;

        @NotNull(message = "Location is required.")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.")
        private BigDecimal longitude;

        @Size(max = 500)
        private String address;

        @Size(max = 1000)
        private String notes;

        /**
         * Optional hint. It is only honoured when it is one of the garages the SERVER computed as eligible for this
         * verified number and location; it never widens that set.
         */
        private Long garageId;
    }

    @Getter
    @Setter
    public static class Decision {
        @Size(max = 1000)
        private String remarks;
    }
}
