package com.garageos.modules.vehicle.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import com.garageos.modules.customer.entity.Customer;
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

    private String variant;

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