package com.garageos.modules.garage.dto.response;

import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.core.enums.garage.WorkshopType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
/**
 * What any authenticated user (a customer picking a garage, a prospective
 * employee joining one, a driver on a trip) may see about a garage. Business
 * identifiers - PAN and GSTIN - are deliberately NOT here; they are only in
 * {@link GarageResponse}, returned to the garage's own staff/owner.
 */
public class GarageSummaryResponse {

    Long id;

    String garageCode;

    String garageName;

    WorkshopType workshopType;

    Integer numberOfBays;

    String address;

    String landmark;

    String city;

    String state;

    String pincode;

    BigDecimal latitude;

    BigDecimal longitude;

    GarageStatus status;

    /** True when this garage has uploaded its own logo (V63). */
    Boolean hasLogo;

    LocalDateTime logoUpdatedAt;

}