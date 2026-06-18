package com.interview_platform_backend.interview_platform_backend.jobposition.entity;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.encryption.converter.EncryptedBigDecimalConverter;
import com.interview_platform_backend.interview_platform_backend.pipeline.entity.InterviewPipeline;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 200)
    private String department;

    @Column(length = 300)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 50)
    @Builder.Default
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 50)
    @Builder.Default
    private ExperienceLevel experienceLevel = ExperienceLevel.MID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private JobPositionStatus status = JobPositionStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "salary_min", columnDefinition = "TEXT")
    private BigDecimal salaryMin;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "salary_max", columnDefinition = "TEXT")
    private BigDecimal salaryMax;

    @Column(name = "salary_currency", length = 10)
    @Builder.Default
    private String salaryCurrency = "USD";

    @Column(name = "number_of_openings", nullable = false)
    @Builder.Default
    private Integer numberOfOpenings = 1;

    @Column(name = "number_hired", nullable = false)
    @Builder.Default
    private Integer numberHired = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id")
    private InterviewPipeline pipeline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hiring_manager_id")
    private User hiringManager;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    private Instant deadline;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "jobPosition", fetch = FetchType.LAZY)
    private List<Interview> interviews = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

