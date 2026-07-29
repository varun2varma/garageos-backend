package com.garageos.modules.identity.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.identity.AuthenticationProvider;
import com.garageos.core.enums.identity.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "garage_id")
    private Long garageId;

    @Column(
            name = "employee_code",
            length = 30
    )
    private String employeeCode;

    @Column(
            nullable = false,
            unique = true,
            length = 100
    )
    private String username;

    @Column(
            name = "password_hash",
            nullable = false,
            length = 255
    )
    private String passwordHash;

    @Column(
            name = "first_name",
            nullable = false,
            length = 100
    )
    private String firstName;

    @Column(
            name = "last_name",
            length = 100
    )
    private String lastName;

    @Column(length = 20)
    private String mobile;

    @Column(length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "authentication_provider",
            nullable = false
    )
    @Builder.Default
    private AuthenticationProvider authenticationProvider =
            AuthenticationProvider.LOCAL;

    @Column(
            name = "first_login",
            nullable = false
    )
    @Builder.Default
    private Boolean firstLogin = true;

    @Column(
            name = "failed_attempts",
            nullable = false
    )
    @Builder.Default
    private Integer failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<UserRole> userRoles = new ArrayList<>();

}