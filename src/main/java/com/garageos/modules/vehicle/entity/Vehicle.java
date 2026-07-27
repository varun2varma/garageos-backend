package com.garageos.modules.vehicle.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.vehiclemaster.entity.VehicleBrand;
import com.garageos.modules.vehiclemaster.entity.VehicleModel;
import com.garageos.modules.vehiclemaster.entity.VehicleVariant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vehicle")
public class Vehicle extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String variant;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "variant_id", nullable = false)
//    VehicleVariant variant;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "brand_id", nullable = false)
//    private VehicleBrand brand;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "model_id", nullable = false)
//    private VehicleModel model;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "variant_id", nullable = false)
//    private VehicleVariant variant;

    @Enumerated(EnumType.STRING)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    private TransmissionType transmission;

    private Integer manufacturingYear;

    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}