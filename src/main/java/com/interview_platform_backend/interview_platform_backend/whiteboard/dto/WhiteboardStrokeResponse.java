package com.interview_platform_backend.interview_platform_backend.whiteboard.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhiteboardStrokeResponse {

    private UUID id;
    private UUID sessionId;
    private UUID userId;
    private String userName;
    private String strokeData;
    private String tool;
    private String color;
    private Double strokeWidth;
    private Integer sequenceNumber;
    private Instant createdAt;
}
