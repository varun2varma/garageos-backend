package com.garageos.modules.vehiclemaster.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicle_variant")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    VehicleModel model;

    @Column(nullable = false, length = 100)
    String variantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TransmissionType transmissionType;

    Integer engineCc;

    Double horsepower;

    Double torqueNm;

    Integer launchYear;

    Integer discontinuedYear;

    Integer serviceIntervalKm;

    Integer serviceIntervalMonths;

    @Column(nullable = false)
    Boolean active = true;
}