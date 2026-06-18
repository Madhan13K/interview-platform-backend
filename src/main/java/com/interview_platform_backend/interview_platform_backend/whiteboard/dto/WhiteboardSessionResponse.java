package com.interview_platform_backend.interview_platform_backend.whiteboard.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhiteboardSessionResponse {

    private UUID id;
    private UUID interviewId;
    private UUID createdById;
    private String createdByName;
    private String title;
    private String snapshotData;
    private String thumbnailUrl;
    private Boolean isActive;
    private Long strokeCount;
    private Instant createdAt;
    private Instant updatedAt;
}
