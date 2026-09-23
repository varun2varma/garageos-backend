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

import java.math.BigDecimal;

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
            length = 10
    )
    String garageCode;

    /**
     * The user who registered this garage. The column has existed since
     * V24 but had no entity field, so nothing could ever populate it -
     * it was permanently null. Mapped here so garage ownership is
     * expressed on the aggregate itself, alongside the ACTIVE OWNER
     * GarageMembership that authorization actually reads.
     */
    @Column(name = "owner_user_id")
    Long ownerUserId;

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

    /**
     * Optional real-world coordinates for this garage, used as the route
     * origin for pickup/delivery ETA calculations (the manager
     * pre-acceptance map, V46). Null for a garage that hasn't set a map
     * location yet - callers must degrade to address-only display rather
     * than assuming these are always present.
     */
    @Column(precision = 10, scale = 7)
    BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    BigDecimal longitude;

    @Column(length = 15)
    String gstNumber;

    @Column(length = 10)
    String panNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    GarageStatus status;

    /**
     * Next value to assign when generating this garage's next employee
     * code (G%03d-EMPnnn). Incremented under a pessimistic row lock in
     * GarageMembershipServiceImpl.generateEmployeeCode() so concurrent
     * approvals for the same garage can never receive the same code.
     */
    @Column(
            name = "next_employee_sequence",
            nullable = false
    )
    Integer nextEmployeeSequence;

}