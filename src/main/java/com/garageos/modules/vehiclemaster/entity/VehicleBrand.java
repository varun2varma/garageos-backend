package com.garageos.modules.vehiclemaster.entity;

import com.garageos.core.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
@Table(name = "vehicle_brand")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleBrand extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    String name;

    @Column(length = 100)
    String country;

    @Column(length = 300)
    String logoUrl;

    @Column(nullable = false)
    Boolean active = true;

    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY)
    List<VehicleModel> models = new ArrayList<>();
}