package com.interview_platform_backend.interview_platform_backend.document.dto;

import com.interview_platform_backend.interview_platform_backend.document.entity.DocumentType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDocumentRequest {
    private String description;
    private DocumentType documentType;
}

