package com.interview_platform_backend.interview_platform_backend.pipeline.entity;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.template.entity.InterviewTemplate;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A single stage within a pipeline (e.g., "Phone Screening", "Technical Round 1").
 */
@Entity
@Table(name = "pipeline_stages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineStage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private InterviewPipeline pipeline;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", length = 30)
    private InterviewType interviewType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private InterviewTemplate template;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "is_optional", nullable = false)
    @Builder.Default
    private Boolean isOptional = false;
}

