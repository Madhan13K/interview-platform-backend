package com.interview_platform_backend.interview_platform_backend.meeting.dto;

import com.interview_platform_backend.interview_platform_backend.meeting.entity.MeetingProvider;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingLinkResponse {

    private UUID id;
    private UUID interviewId;
    private MeetingProvider provider;
    private String meetingUrl;
    private String hostUrl;
    private String meetingId;
    private String passcode;
    private Instant createdAt;
    private Instant expiresAt;
}

