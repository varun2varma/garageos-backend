package com.garageos.modules.notification.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.notification.DevicePlatform;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * An FCM registration token for one app installation. Not an identity: a
 * token is reassigned to whichever user most recently registered it.
 */
@Entity
@Table(name = "device_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DevicePlatform platform;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
}
