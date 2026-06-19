package com.interview_platform_backend.interview_platform_backend.sourcetracking.entity;

import com.interview_platform_backend.interview_platform_backend.jobboard.entity.JobApplication;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceType source;

    @Column(name = "source_campaign")
    private String sourceCampaign;

    @Column(name = "cost_per_click", precision = 10, scale = 2)
    private BigDecimal costPerClick;

    @Column(name = "total_spend", precision = 12, scale = 2)
    private BigDecimal totalSpend;

    @Column(name = "attributed_at")
    private Instant attributedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (attributedAt == null) {
            attributedAt = Instant.now();
        }
    }
}
