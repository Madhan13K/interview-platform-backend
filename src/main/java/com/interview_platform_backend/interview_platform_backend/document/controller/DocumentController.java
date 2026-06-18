package com.interview_platform_backend.interview_platform_backend.document.controller;

import com.interview_platform_backend.interview_platform_backend.document.dto.DocumentResponse;
import com.interview_platform_backend.interview_platform_backend.document.dto.PresignedUrlResponse;
import com.interview_platform_backend.interview_platform_backend.document.entity.DocumentType;
import com.interview_platform_backend.interview_platform_backend.document.service.DocumentService;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "File/Document management with AWS S3 storage")
public class DocumentController {

    private final DocumentService documentService;
    private final SecurityHelper securityHelper;

    public DocumentController(DocumentService documentService, SecurityHelper securityHelper) {
        this.documentService = documentService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Upload a document to S3")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or request")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "entityId", required = false) UUID entityId,
            @RequestParam(value = "description", required = false) String description) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(documentService.uploadDocument(file, documentType, entityType, entityId, description, currentUserId));
    }

    @Operation(summary = "Get document metadata by ID")
    @ApiResponse(responseCode = "200", description = "Document found")
    @GetMapping("/{documentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.getDocument(documentId));
    }

    @Operation(summary = "Get documents by entity (e.g., interview, candidate)")
    @ApiResponse(responseCode = "200", description = "Documents list")
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(documentService.getDocumentsByEntity(entityType, entityId));
    }

    @Operation(summary = "Get my uploaded documents")
    @ApiResponse(responseCode = "200", description = "User's documents")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments() {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(documentService.getDocumentsByUser(currentUserId));
    }

    @Operation(summary = "Get my uploaded documents (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated user's documents")
    @GetMapping("/my/paginated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<DocumentResponse>> getMyDocumentsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(documentService.getDocumentsByUserPaginated(currentUserId, page, size));
    }

    @Operation(summary = "Get documents by type")
    @ApiResponse(responseCode = "200", description = "Documents by type")
    @GetMapping("/type/{documentType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByType(@PathVariable DocumentType documentType) {
        return ResponseEntity.ok(documentService.getDocumentsByType(documentType));
    }

    @Operation(summary = "Get presigned download URL for a document")
    @ApiResponse(responseCode = "200", description = "Presigned URL generated")
    @GetMapping("/{documentId}/download-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.getPresignedDownloadUrl(documentId));
    }

    @Operation(summary = "Get presigned upload URL (for client-side upload)")
    @ApiResponse(responseCode = "200", description = "Presigned upload URL generated")
    @PostMapping("/presigned-upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresignedUrlResponse> getPresignedUploadUrl(
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam DocumentType documentType) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(documentService.getPresignedUploadUrl(fileName, contentType, documentType, currentUserId));
    }

    @Operation(summary = "Delete a document")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Document deleted"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @DeleteMapping("/{documentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID documentId) {
        UUID currentUserId = securityHelper.getCurrentUserId();
        documentService.deleteDocument(documentId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update document metadata")
    @ApiResponse(responseCode = "200", description = "Document metadata updated")
    @PatchMapping("/{documentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> updateDocumentMetadata(
            @PathVariable UUID documentId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) DocumentType documentType) {
        return ResponseEntity.ok(documentService.updateDocumentMetadata(documentId, description, documentType));
    }
}

