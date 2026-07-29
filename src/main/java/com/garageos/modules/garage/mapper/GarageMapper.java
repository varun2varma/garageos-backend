package com.garageos.modules.garage.mapper;

import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.response.GarageResponse;
import com.garageos.modules.garage.entity.Garage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GarageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "garageCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    Garage toEntity(CreateGarageRequest request);

    GarageResponse toResponse(Garage garage);

}