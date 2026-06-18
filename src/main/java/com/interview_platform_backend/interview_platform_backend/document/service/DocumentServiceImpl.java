package com.interview_platform_backend.interview_platform_backend.document.service;

import com.interview_platform_backend.interview_platform_backend.audit.AuditAction;
import com.interview_platform_backend.interview_platform_backend.audit.AuditService;
import com.interview_platform_backend.interview_platform_backend.document.dto.DocumentResponse;
import com.interview_platform_backend.interview_platform_backend.document.dto.PresignedUrlResponse;
import com.interview_platform_backend.interview_platform_backend.document.entity.Document;
import com.interview_platform_backend.interview_platform_backend.document.entity.DocumentType;
import com.interview_platform_backend.interview_platform_backend.document.repository.DocumentRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ForbiddenException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "application/zip",
            "application/json"
    );

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;
    private final AuditService auditService;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                                UserRepository userRepository,
                                S3StorageService s3StorageService,
                                AuditService auditService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.s3StorageService = s3StorageService;
        this.auditService = auditService;
    }

    @Override
    public DocumentResponse uploadDocument(MultipartFile file, DocumentType documentType,
                                            String entityType, UUID entityId, String description, UUID uploadedBy) {
        if (file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum limit of 50MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("File type not allowed. Supported types: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, TXT, CSV, PNG, JPEG, GIF, WEBP, ZIP, JSON");
        }

        User user = userRepository.findById(uploadedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", uploadedBy));

        String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        String s3Key = s3StorageService.generateS3Key(documentType.name(), uploadedBy, originalFileName);

        String s3Url;
        try {
            s3Url = s3StorageService.uploadFile(file, s3Key);
        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", e.getMessage());
            throw new BadRequestException("Failed to upload file: " + e.getMessage());
        }

        Document document = Document.builder()
                .fileName(s3Key.substring(s3Key.lastIndexOf('/') + 1))
                .originalFileName(originalFileName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .s3Bucket(s3StorageService.getBucketName())
                .s3Key(s3Key)
                .s3Url(s3Url)
                .documentType(documentType)
                .entityType(entityType)
                .entityId(entityId)
                .uploadedBy(user)
                .description(description)
                .createdAt(Instant.now())
                .build();

        Document saved = documentRepository.save(document);
        auditService.log("Document", saved.getId(), AuditAction.DOCUMENT_UPLOAD,
                "File uploaded: " + originalFileName);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        return mapToResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByEntity(String entityType, UUID entityId) {
        return documentRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByUser(UUID userId) {
        return documentRepository.findByUploadedById(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<DocumentResponse> getDocumentsByUserPaginated(UUID userId, int page, int size) {
        Page<Document> docPage = documentRepository.findByUploadedById(userId, PageRequest.of(page, size));
        List<DocumentResponse> content = docPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PaginatedResponse.<DocumentResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(docPage.getTotalElements())
                .totalPages(docPage.getTotalPages())
                .last(docPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByType(DocumentType documentType) {
        return documentRepository.findByDocumentType(documentType).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PresignedUrlResponse getPresignedDownloadUrl(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        String downloadUrl = s3StorageService.generatePresignedDownloadUrl(document.getS3Key());
        return PresignedUrlResponse.builder()
                .downloadUrl(downloadUrl)
                .expiresInSeconds(s3StorageService.getPresignedUrlExpiryMinutes() * 60L)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PresignedUrlResponse getPresignedUploadUrl(String fileName, String contentType,
                                                       DocumentType documentType, UUID userId) {
        String s3Key = s3StorageService.generateS3Key(documentType.name(), userId, fileName);
        String uploadUrl = s3StorageService.generatePresignedUploadUrl(s3Key, contentType);
        return PresignedUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .expiresInSeconds(s3StorageService.getPresignedUrlExpiryMinutes() * 60L)
                .build();
    }

    @Override
    public void deleteDocument(UUID documentId, UUID requestedBy) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        if (!document.getUploadedBy().getId().equals(requestedBy)) {
            throw new ForbiddenException("You can only delete your own documents");
        }

        s3StorageService.deleteFile(document.getS3Key());
        documentRepository.delete(document);

        auditService.log("Document", documentId, AuditAction.DOCUMENT_DELETE,
                "File deleted: " + document.getOriginalFileName());
    }

    @Override
    public DocumentResponse updateDocumentMetadata(UUID documentId, String description, DocumentType documentType) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        if (description != null) {
            document.setDescription(description);
        }
        if (documentType != null) {
            document.setDocumentType(documentType);
        }
        document.setUpdatedAt(Instant.now());

        Document updated = documentRepository.save(document);
        return mapToResponse(updated);
    }

    private DocumentResponse mapToResponse(Document document) {
        User uploader = document.getUploadedBy();
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .s3Url(document.getS3Url())
                .documentType(document.getDocumentType())
                .entityType(document.getEntityType())
                .entityId(document.getEntityId())
                .uploadedById(uploader.getId())
                .uploadedByName(uploader.getFirstName() + " " + uploader.getLastName())
                .description(document.getDescription())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}



