package com.garageos.modules.identity.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.identity.PermissionAction;
import com.garageos.core.enums.identity.PermissionCode;
import com.garageos.core.enums.identity.PermissionModule;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionModule module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 100)
    private PermissionCode code;

    @Column(
            name = "display_name",
            nullable = false,
            length = 150
    )
    private String displayName;

    @Column(length = 500)
    private String description;

    @OneToMany(
            mappedBy = "permission",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<RolePermission> rolePermissions =
            new ArrayList<>();

}