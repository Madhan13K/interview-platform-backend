package com.interview_platform_backend.interview_platform_backend.exportimport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportRequest {

    @NotBlank(message = "Entity type is required")
    private String entityType;

    @NotNull(message = "File document ID is required")
    private UUID fileDocumentId;
}
