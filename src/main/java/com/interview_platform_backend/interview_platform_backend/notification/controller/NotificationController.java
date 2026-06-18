package com.interview_platform_backend.interview_platform_backend.notification.controller;

import com.interview_platform_backend.interview_platform_backend.notification.dto.NotificationCountResponse;
import com.interview_platform_backend.interview_platform_backend.notification.dto.NotificationResponse;
import com.interview_platform_backend.interview_platform_backend.notification.service.InAppNotificationService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "In-app notification management")
public class NotificationController {

    private final InAppNotificationService notificationService;
    private final SecurityHelper securityHelper;

    public NotificationController(InAppNotificationService notificationService, SecurityHelper securityHelper) {
        this.notificationService = notificationService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Get my notifications (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated notifications")
    @GetMapping
    public ResponseEntity<PaginatedResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(notificationService.getNotifications(userId, page, size));
    }

    @Operation(summary = "Get my unread notifications")
    @ApiResponse(responseCode = "200", description = "Unread notifications")
    @GetMapping("/unread")
    public ResponseEntity<PaginatedResponse<NotificationResponse>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId, page, size));
    }

    @Operation(summary = "Get unread notification count (for badge)")
    @ApiResponse(responseCode = "200", description = "Unread count")
    @GetMapping("/count")
    public ResponseEntity<NotificationCountResponse> getUnreadCount() {
        UUID userId = securityHelper.getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(new NotificationCountResponse(count));
    }

    @Operation(summary = "Mark a notification as read")
    @ApiResponse(responseCode = "204", description = "Marked as read")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark all notifications as read")
    @ApiResponse(responseCode = "204", description = "All marked as read")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        UUID userId = securityHelper.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}

