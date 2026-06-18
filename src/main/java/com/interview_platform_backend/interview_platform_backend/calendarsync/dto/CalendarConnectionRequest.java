package com.interview_platform_backend.interview_platform_backend.calendarsync.dto;

import com.interview_platform_backend.interview_platform_backend.calendarsync.entity.CalendarProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarConnectionRequest {

    @NotNull(message = "Provider is required")
    private CalendarProvider provider;

    @NotBlank(message = "Authorization code is required")
    private String authorizationCode;

    private String calendarId;

    private String redirectUri;
}
