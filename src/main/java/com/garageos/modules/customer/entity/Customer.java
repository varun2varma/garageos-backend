package com.garageos.modules.customer.entity;

import com.garageos.core.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "customer")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Customer extends BaseEntity {

    @Column(nullable = false, length = 100)
    String firstName;

    @Column(length = 100)
    String lastName;

    @Column(nullable = false, unique = true, length = 15)
    String mobileNumber;

    @Column(length = 150)
    String email;

    String address;

    @Column(length = 100)
    String city;

    @Column(length = 100)
    String state;

    @Column(length = 10)
    String pincode;

    String remarks;

    public String getFullName() {
        return lastName == null || lastName.isBlank()
                ? firstName
                : firstName + " " + lastName;
    }
}