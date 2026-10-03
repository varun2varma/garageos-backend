package com.garageos.modules.garage.mapper;

import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.response.GarageResponse;
import com.garageos.modules.garage.dto.response.GarageSummaryResponse;
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
    @Mapping(target = "nextEmployeeSequence", ignore = true)
    // Never client-suppliable - GarageServiceImpl.createGarage sets this
    // from the authenticated caller's own userId, not the request body.
    @Mapping(target = "ownerUserId", ignore = true)
    // Logo fields are managed only through GarageBrandingService.
    @Mapping(target = "logoStorageKey", ignore = true)
    @Mapping(target = "logoContentType", ignore = true)
    @Mapping(target = "logoUpdatedAt", ignore = true)
    Garage toEntity(CreateGarageRequest request);

    @Mapping(target = "hasLogo", expression = "java(garage.getLogoStorageKey() != null)")
    GarageResponse toResponse(Garage garage);

    @Mapping(target = "hasLogo", expression = "java(garage.getLogoStorageKey() != null)")
    GarageSummaryResponse toSummary(Garage garage);

}