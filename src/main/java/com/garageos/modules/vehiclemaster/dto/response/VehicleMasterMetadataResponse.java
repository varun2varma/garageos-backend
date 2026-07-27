package com.garageos.modules.vehiclemaster.dto.response;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.vehiclemaster.enums.BodyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMasterMetadataResponse {

    private List<FuelType> fuelTypes;

    private List<TransmissionType> transmissionTypes;

    private List<BodyType> bodyTypes;
}