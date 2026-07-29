package com.garageos.modules.identity.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.identity.RoleCode;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            unique = true,
            length = 50
    )
    private RoleCode code;

    @Column(
            name = "display_name",
            nullable = false,
            length = 100
    )
    private String displayName;

    @Column(length = 500)
    private String description;

    @Column(
            name = "system_role",
            nullable = false
    )
    @Builder.Default
    private Boolean systemRole = true;

    @OneToMany(
            mappedBy = "role",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<UserRole> userRoles =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "role",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<RolePermission> rolePermissions =
            new ArrayList<>();

}