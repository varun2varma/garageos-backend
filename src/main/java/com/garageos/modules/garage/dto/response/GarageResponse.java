package com.garageos.modules.garage.dto.response;

import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.core.enums.garage.WorkshopType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GarageResponse {

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

    String gstNumber;

    String panNumber;

    BigDecimal latitude;

    BigDecimal longitude;

    GarageStatus status;

}