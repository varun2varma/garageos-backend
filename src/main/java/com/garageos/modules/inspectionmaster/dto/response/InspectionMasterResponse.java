package com.garageos.modules.inspectionmaster.dto.response;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionMasterResponse {

    private Long id;

    private String make;

    private String model;

    private String variant;

    private FuelType fuelType;

    private TransmissionType transmissionType;

    private Integer minYear;

    private Integer maxYear;

    private Integer minOdometer;

    private Integer maxOdometer;

    private Boolean active;

    private List<InspectionMasterItemResponse> items;

}