package com.interview_platform_backend.interview_platform_backend.exportimport.entity;

import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "export_import_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(nullable = false, length = 50)
    private String entityType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String filters;

    @Column(length = 500)
    private String fileName;

    @Column(length = 1000)
    private String s3Key;

    private Integer totalRecords;

    private Integer processedRecords;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant startedAt;

    private Instant completedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum JobType {
        EXPORT, IMPORT
    }

    public enum JobFormat {
        CSV, EXCEL, JSON
    }

    public enum JobStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
