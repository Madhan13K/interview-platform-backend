package com.interview_platform_backend.interview_platform_backend.tag.dto;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TagResponse {
    private UUID id;
    private String name;
    private String color;
    private String category;
    private UUID createdById;
    private Instant createdAt;
}

