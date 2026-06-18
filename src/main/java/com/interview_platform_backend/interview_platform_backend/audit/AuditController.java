package com.interview_platform_backend.interview_platform_backend.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository.findAll(pageable));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(
                auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId));
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(
            @PathVariable String email, Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository.findByPerformedBy(email, pageable));
    }
}

