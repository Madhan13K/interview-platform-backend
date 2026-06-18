package com.interview_platform_backend.interview_platform_backend.meeting.dto;

import com.interview_platform_backend.interview_platform_backend.meeting.entity.MeetingProvider;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateMeetingRequest {

    @NotNull(message = "Provider is required")
    private MeetingProvider provider;

    private String topic;

    private Integer durationMinutes;
}

