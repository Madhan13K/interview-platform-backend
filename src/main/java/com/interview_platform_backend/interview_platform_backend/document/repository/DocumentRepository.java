package com.interview_platform_backend.interview_platform_backend.document.repository;

import com.interview_platform_backend.interview_platform_backend.document.entity.Document;
import com.interview_platform_backend.interview_platform_backend.document.entity.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUploadedById(UUID userId);

    Page<Document> findByUploadedById(UUID userId, Pageable pageable);

    List<Document> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    List<Document> findByDocumentType(DocumentType documentType);

    Page<Document> findByDocumentType(DocumentType documentType, Pageable pageable);

    List<Document> findByEntityTypeAndEntityIdAndDocumentType(String entityType, UUID entityId, DocumentType documentType);
}

