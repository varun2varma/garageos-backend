package com.garageos.modules.customer.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.customer.CustomerGarageSource;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Relationship metadata between a customer and a garage (V66). Not a
 * replacement for JobCard/Booking ownership - those stay the source of
 * truth for service history.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer_garage")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerGarage extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    Long customerId;

    @Column(name = "garage_id", nullable = false)
    Long garageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    CustomerGarageSource source;

    @Column(name = "is_default", nullable = false)
    boolean defaultGarage;
}
