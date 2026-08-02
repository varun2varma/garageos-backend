package com.garageos.modules.garagemembership.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "garage_membership",
        indexes = {

                @Index(
                        name = "idx_garage_membership_garage",
                        columnList = "garage_id"
                ),

                @Index(
                        name = "idx_garage_membership_user",
                        columnList = "user_id"
                ),

                @Index(
                        name = "idx_garage_membership_status",
                        columnList = "status"
                )

        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GarageMembership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "garage_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_garage_membership_garage")
    )
    Garage garage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_garage_membership_user")
    )
    User user;

    @Column(length = 30)
    String employeeCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    GarageMembershipStatus status;

    LocalDateTime joinedAt;

    LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "approved_by",
            foreignKey = @ForeignKey(name = "fk_garage_membership_approved_by")
    )
    User approvedBy;

    @Column(length = 500)
    String remarks;

    @OneToMany(
            mappedBy = "membership",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<GarageMembershipRole> roles = new ArrayList<>();

}