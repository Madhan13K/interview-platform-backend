package com.interview_platform_backend.interview_platform_backend.notification.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCountResponse {

    private Long unreadCount;
}

