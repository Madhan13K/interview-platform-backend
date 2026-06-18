package com.interview_platform_backend.interview_platform_backend.codeeditor.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingSessionResponse {

    private UUID id;
    private UUID interviewId;
    private String language;
    private String codeContent;
    private UUID lastEditedBy;
    private Instant startedAt;
    private Instant endedAt;
}

