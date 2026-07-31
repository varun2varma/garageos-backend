package com.garageos.modules.inspectionmaster.repository.projection;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;

public interface InspectionMasterLookup {

    Long getId();

    String getMake();

    String getModel();

    String getVariant();

    FuelType getFuelType();

    TransmissionType getTransmissionType();

    Integer getMinYear();

    Integer getMaxYear();

    Integer getMinOdometer();

    Integer getMaxOdometer();

}