package com.interview_platform_backend.interview_platform_backend.candidate.entity;

import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interview_feedback",
uniqueConstraints = {@UniqueConstraint(name = "uk_interview_feedback_interview_interviewer",
                columnNames = {"interview_id", "interviewer_id"})})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewFeedBack {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Column(nullable = false)
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackRecommendation recommendation;

    @Column(length = 2000)
    private String strengths;

    @Column(length = 2000)
    private String weaknesses;

    @Column(length = 2000)
    private String comments;

    private Instant submittedAt;
}
