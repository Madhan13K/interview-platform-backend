package com.interview_platform_backend.interview_platform_backend.video.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoRecordingResponse {

    private UUID id;
    private UUID interviewId;
    private UUID recordedByUserId;
    private String recordedByName;
    private String fileName;
    private Long fileSizeBytes;
    private Integer durationSeconds;
    private String mimeType;
    private String status;
    private String thumbnailUrl;
    private String downloadUrl;
    private Instant startedAt;
    private Instant endedAt;
    private Instant createdAt;
}
