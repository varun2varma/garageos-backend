package com.garageos.modules.vehiclemaster.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.modules.vehiclemaster.enums.BodyType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicle_model")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleModel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    VehicleBrand brand;

    @Column(nullable = false, length = 100)
    String name;

    @Enumerated(EnumType.STRING)
    BodyType bodyType;

    Integer seatingCapacity;

    @Column(nullable = false)
    Boolean active = true;

    @OneToMany(mappedBy = "model", fetch = FetchType.LAZY)
    List<VehicleVariant> variants = new ArrayList<>();
}