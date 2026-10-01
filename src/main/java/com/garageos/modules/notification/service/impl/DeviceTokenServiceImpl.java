package com.garageos.modules.notification.service.impl;

import com.garageos.core.enums.notification.DevicePlatform;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.notification.dto.DeviceTokenResponse;
import com.garageos.modules.notification.dto.RegisterDeviceRequest;
import com.garageos.modules.notification.entity.DeviceToken;
import com.garageos.modules.notification.repository.DeviceTokenRepository;
import com.garageos.modules.notification.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository repository;

    @Override
    @Transactional
    public DeviceTokenResponse register(Long userId, RegisterDeviceRequest request) {

        String tokenValue = request.token() == null ? "" : request.token().trim();
        if (tokenValue.isEmpty()) {
            throw new BusinessException("Device token is required.");
        }

        DevicePlatform platform = parsePlatform(request.platform());

        DeviceToken token = repository.findByToken(tokenValue).orElse(null);
        boolean reassigned = false;

        if (token == null) {
            token = DeviceToken.builder().token(tokenValue).build();
        } else if (!userId.equals(token.getUserId())) {
            reassigned = true;
        }

        token.setUserId(userId);
        token.setPlatform(platform);
        token.setAppVersion(request.appVersion());
        token.setActive(true);
        token.setLastSeenAt(LocalDateTime.now());

        token = repository.save(token);

        log.info("[NOTIF_DEVICE] registered deviceTokenId={} userId={} reassigned={}",
                token.getId(), userId, reassigned);

        return toResponse(token);
    }

    @Override
    @Transactional
    public void unregister(Long userId, String tokenValue) {

        if (tokenValue == null || tokenValue.isBlank()) {
            return;
        }

        repository.findByToken(tokenValue.trim())
                .filter(t -> userId.equals(t.getUserId()))
                .ifPresent(this::deactivate);
    }

    @Override
    @Transactional
    public void unregisterById(Long userId, Long id) {

        repository.findById(id)
                .filter(t -> userId.equals(t.getUserId()))
                .ifPresent(this::deactivate);
    }

    private void deactivate(DeviceToken token) {
        token.setActive(false);
        repository.save(token);
        log.info("[NOTIF_DEVICE] deactivated deviceTokenId={} userId={}", token.getId(), token.getUserId());
    }

    private static DevicePlatform parsePlatform(String value) {
        if (value == null || value.isBlank()) {
            return DevicePlatform.ANDROID;
        }
        try {
            return DevicePlatform.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unsupported device platform.");
        }
    }

    private static DeviceTokenResponse toResponse(DeviceToken t) {
        return new DeviceTokenResponse(t.getId(), t.getPlatform().name(), Boolean.TRUE.equals(t.getActive()));
    }
}
