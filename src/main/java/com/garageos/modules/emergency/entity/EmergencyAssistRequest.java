package com.garageos.modules.emergency.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.emergency.EmergencyAssistanceType;
import com.garageos.core.enums.emergency.EmergencyRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One roadside-assistance request. {@code contactMobile} was proven by OTP before the row exists. The customer's
 * status token is not stored: it is derived (HMAC) from the code and the number. Plain ids for garage/customer, like
 * booking.
 */
@Entity
@Table(name = "emergency_assist_request")
@Getter
@Setter
@NoArgsConstructor
public class EmergencyAssistRequest extends BaseEntity {

    @Column(name = "request_code", nullable = false, length = 20, updatable = false)
    private String requestCode;

    @Column(name = "idempotency_key", nullable = false, length = 80, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmergencyRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "assistance_type", nullable = false, length = 30)
    private EmergencyAssistanceType assistanceType;

    @Column(name = "garage_id", nullable = false)
    private Long garageId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "contact_mobile", nullable = false, length = 15)
    private String contactMobile;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "vehicle_registration", length = 30)
    private String vehicleRegistration;

    @Column(name = "vehicle_description", length = 200)
    private String vehicleDescription;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 500)
    private String address;

    @Column(length = 1000)
    private String notes;

    @Column(name = "garage_remarks", length = 1000)
    private String garageRemarks;

    @Column(name = "decided_by_user_id")
    private Long decidedByUserId;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
