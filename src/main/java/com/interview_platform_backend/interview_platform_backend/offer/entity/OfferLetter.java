package com.interview_platform_backend.interview_platform_backend.offer.entity;

import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPosition;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "offer_letters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_position_id", nullable = false)
    private JobPosition jobPosition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OfferStatus status = OfferStatus.DRAFT;

    @Column(name = "offer_content", columnDefinition = "TEXT")
    private String offerContent;

    @Column(name = "salary_offered", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaryOffered;

    @Column(name = "salary_currency", length = 10, nullable = false)
    @Builder.Default
    private String salaryCurrency = "USD";

    @Column(name = "bonus_amount", precision = 12, scale = 2)
    private BigDecimal bonusAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "viewed_at")
    private Instant viewedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "candidate_response", columnDefinition = "TEXT")
    private String candidateResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "esignature_provider", length = 20)
    @Builder.Default
    private ESignatureProvider esignatureProvider = ESignatureProvider.NONE;

    @Column(name = "esignature_envelope_id")
    private String esignatureEnvelopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "esignature_status", length = 20)
    private ESignatureStatus esignatureStatus;

    @Column(name = "esignature_signed_at")
    private Instant esignatureSignedAt;

    @Column(name = "esignature_document_url")
    private String esignatureDocumentUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "offerLetter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfferApproval> approvals = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
