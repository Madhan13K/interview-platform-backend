package com.interview_platform_backend.interview_platform_backend.notification.service;

import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.notification.dto.NotificationResponse;
import com.interview_platform_backend.interview_platform_backend.notification.entity.Notification;
import com.interview_platform_backend.interview_platform_backend.notification.repository.NotificationRepository;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InAppNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public InAppNotificationService(NotificationRepository notificationRepository,
                                    UserRepository userRepository,
                                    SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Create and push a notification to a user (in-app + WebSocket push).
     */
    public void notify(UUID userId, String type, String title, String message,
                       UUID referenceId, String referenceType) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Push via WebSocket to user-specific queue
        NotificationResponse response = toResponse(saved);
        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications",
                response
        );
    }

    /**
     * Notify multiple users.
     */
    public void notifyMultiple(List<UUID> userIds, String type, String title, String message,
                               UUID referenceId, String referenceType) {
        for (UUID userId : userIds) {
            notify(userId, type, title, message, referenceId, referenceType);
        }
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<NotificationResponse> getNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationResponse> content = notifPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PaginatedResponse.<NotificationResponse>builder()
                .content(content)
                .page(notifPage.getNumber())
                .size(notifPage.getSize())
                .totalElements(notifPage.getTotalElements())
                .totalPages(notifPage.getTotalPages())
                .last(notifPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<NotificationResponse> getUnreadNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifPage = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);

        List<NotificationResponse> content = notifPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PaginatedResponse.<NotificationResponse>builder()
                .content(content)
                .page(notifPage.getNumber())
                .size(notifPage.getSize())
                .totalElements(notifPage.getTotalElements())
                .totalPages(notifPage.getTotalPages())
                .last(notifPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notification.setIsRead(true);
        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
    }

    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}

