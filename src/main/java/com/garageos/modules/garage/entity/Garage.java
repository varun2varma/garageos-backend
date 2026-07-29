package com.garageos.modules.garage.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.core.enums.garage.WorkshopType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "garage",
        indexes = {

                @Index(
                        name = "idx_garage_code",
                        columnList = "garage_code",
                        unique = true
                ),

                @Index(
                        name = "idx_garage_name",
                        columnList = "garage_name"
                )

        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Garage extends BaseEntity {

    @Column(
            nullable = false,
            unique = true,
            updatable = false,
            length = 10
    )
    String garageCode;

    @Column(
            nullable = false,
            length = 150
    )
    String garageName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    WorkshopType workshopType;

    @Column(nullable = false)
    Integer numberOfBays;

    @Column(length = 300)
    String address;

    @Column(length = 100)
    String landmark;

    @Column(length = 100)
    String city;

    @Column(length = 100)
    String state;

    @Column(length = 6)
    String pincode;

    @Column(length = 15)
    String gstNumber;

    @Column(length = 10)
    String panNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    GarageStatus status;

}