package com.interview_platform_backend.interview_platform_backend.document.service;

import com.interview_platform_backend.interview_platform_backend.document.dto.DocumentResponse;
import com.interview_platform_backend.interview_platform_backend.document.dto.PresignedUrlResponse;
import com.interview_platform_backend.interview_platform_backend.document.entity.DocumentType;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    DocumentResponse uploadDocument(MultipartFile file, DocumentType documentType,
                                     String entityType, UUID entityId, String description, UUID uploadedBy);

    DocumentResponse getDocument(UUID documentId);

    List<DocumentResponse> getDocumentsByEntity(String entityType, UUID entityId);

    List<DocumentResponse> getDocumentsByUser(UUID userId);

    PaginatedResponse<DocumentResponse> getDocumentsByUserPaginated(UUID userId, int page, int size);

    List<DocumentResponse> getDocumentsByType(DocumentType documentType);

    PresignedUrlResponse getPresignedDownloadUrl(UUID documentId);

    PresignedUrlResponse getPresignedUploadUrl(String fileName, String contentType, DocumentType documentType, UUID userId);

    void deleteDocument(UUID documentId, UUID requestedBy);

    DocumentResponse updateDocumentMetadata(UUID documentId, String description, DocumentType documentType);
}

