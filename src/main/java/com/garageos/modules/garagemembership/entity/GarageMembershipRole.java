package com.garageos.modules.garagemembership.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.modules.identity.entity.Role;
import jakarta.persistence.Entity;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "garage_membership_role",
        indexes = {

                @Index(
                        name = "idx_membership_role_membership",
                        columnList = "membership_id"
                ),

                @Index(
                        name = "idx_membership_role_role",
                        columnList = "role_id"
                )

        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GarageMembershipRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "membership_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_membership_role_membership")
    )
    GarageMembership membership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "role_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_membership_role_role")
    )
    Role role;

}