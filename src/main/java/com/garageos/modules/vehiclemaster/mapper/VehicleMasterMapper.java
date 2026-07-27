package com.garageos.modules.vehiclemaster.mapper;

import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleBrandRequest;
import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleModelRequest;
import com.garageos.modules.vehiclemaster.dto.request.CreateVehicleVariantRequest;
import com.garageos.modules.vehiclemaster.dto.response.VehicleBrandResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleModelResponse;
import com.garageos.modules.vehiclemaster.dto.response.VehicleVariantResponse;
import com.garageos.modules.vehiclemaster.entity.VehicleBrand;
import com.garageos.modules.vehiclemaster.entity.VehicleModel;
import com.garageos.modules.vehiclemaster.entity.VehicleVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface VehicleMasterMapper {

    // ==========================
    // Brand
    // ==========================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "models", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    VehicleBrand toEntity(CreateVehicleBrandRequest request);

    VehicleBrandResponse toBrandResponse(VehicleBrand entity);

    List<VehicleBrandResponse> toBrandResponseList(List<VehicleBrand> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "models", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    void updateBrand(
            CreateVehicleBrandRequest request,
            @MappingTarget VehicleBrand entity
    );

    // ==========================
    // Model
    // ==========================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    VehicleModel toEntity(CreateVehicleModelRequest request);

    @Mapping(source = "brand.id", target = "brandId")
    @Mapping(source = "brand.name", target = "brandName")
    VehicleModelResponse toModelResponse(VehicleModel entity);

    List<VehicleModelResponse> toModelResponseList(List<VehicleModel> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    void updateModel(
            CreateVehicleModelRequest request,
            @MappingTarget VehicleModel entity
    );

    // ==========================
    // Variant
    // ==========================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "model", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    VehicleVariant toEntity(CreateVehicleVariantRequest request);

    @Mapping(source = "model.id", target = "modelId")
    @Mapping(source = "model.name", target = "modelName")
    VehicleVariantResponse toVariantResponse(VehicleVariant entity);

    List<VehicleVariantResponse> toVariantResponseList(List<VehicleVariant> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "model", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    void updateVariant(
            CreateVehicleVariantRequest request,
            @MappingTarget VehicleVariant entity
    );

}