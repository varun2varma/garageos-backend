package com.garageos.modules.customer.dto.response.portal;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerVehicleResponse {

    private Long id;

    private String registrationNumber;

    private String brand;

    private String model;

    private String variant;

    private Integer manufacturingYear;

    private FuelType fuelType;

    private TransmissionType transmission;

    private String color;

}