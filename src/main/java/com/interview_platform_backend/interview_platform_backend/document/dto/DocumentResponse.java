package com.interview_platform_backend.interview_platform_backend.document.dto;

import com.interview_platform_backend.interview_platform_backend.document.entity.DocumentType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {
    private UUID id;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String s3Url;
    private DocumentType documentType;
    private String entityType;
    private UUID entityId;
    private UUID uploadedById;
    private String uploadedByName;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}

