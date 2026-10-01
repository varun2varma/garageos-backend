package com.garageos.modules.notification.service;

import com.garageos.modules.notification.dto.DeviceTokenResponse;
import com.garageos.modules.notification.dto.RegisterDeviceRequest;

public interface DeviceTokenService {

    /** Upserts the token for the given user, reassigning it if another user held it. */
    DeviceTokenResponse register(Long userId, RegisterDeviceRequest request);

    /** Deactivates the token only if it is currently owned by the user. */
    void unregister(Long userId, String token);

    void unregisterById(Long userId, Long id);
}
