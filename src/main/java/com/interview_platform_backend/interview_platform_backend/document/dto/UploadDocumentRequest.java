package com.interview_platform_backend.interview_platform_backend.document.dto;

import com.interview_platform_backend.interview_platform_backend.document.entity.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadDocumentRequest {

    @NotNull
    private DocumentType documentType;

    private String entityType;

    private UUID entityId;

    private String description;
}

