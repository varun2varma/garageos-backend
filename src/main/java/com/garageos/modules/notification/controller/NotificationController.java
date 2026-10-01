package com.garageos.modules.notification.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.notification.dto.NotificationResponse;
import com.garageos.modules.notification.dto.UnreadCountResponse;
import com.garageos.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Notification history for the authenticated user only. */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> list(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ApiResponseUtil.success(
                "Notifications fetched successfully.",
                notificationService.list(user.getId(), page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> unreadCount(
            @AuthenticationPrincipal GarageUserPrincipal user) {

        return ApiResponseUtil.success(
                "Unread count fetched successfully.",
                new UnreadCountResponse(notificationService.unreadCount(user.getId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id) {

        notificationService.markRead(user.getId(), id);
        return ApiResponseUtil.success("Notification marked as read.");
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal GarageUserPrincipal user) {

        notificationService.markAllRead(user.getId());
        return ApiResponseUtil.success("All notifications marked as read.");
    }
}
