package com.garageos.modules.vehicle.mapper;

import com.garageos.modules.vehicle.dto.request.CreateVehicleRequest;
import com.garageos.modules.vehicle.dto.response.VehicleResponse;
import com.garageos.modules.vehicle.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Vehicle toEntity(CreateVehicleRequest request);

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.fullName", target = "customerName")
    @Mapping(source = "customer.mobileNumber", target = "customerMobileNumber")
    VehicleResponse toResponse(Vehicle vehicle);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateVehicleRequest request,
                      @MappingTarget Vehicle vehicle);
}