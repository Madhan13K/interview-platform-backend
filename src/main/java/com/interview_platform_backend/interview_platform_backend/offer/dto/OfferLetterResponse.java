package com.interview_platform_backend.interview_platform_backend.offer.dto;

import com.interview_platform_backend.interview_platform_backend.offer.entity.ESignatureProvider;
import com.interview_platform_backend.interview_platform_backend.offer.entity.ESignatureStatus;
import com.interview_platform_backend.interview_platform_backend.offer.entity.OfferStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferLetterResponse {

    private UUID id;
    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;
    private UUID jobPositionId;
    private String jobPositionTitle;
    private String department;
    private UUID createdById;
    private String createdByName;
    private OfferStatus status;
    private String offerContent;
    private BigDecimal salaryOffered;
    private String salaryCurrency;
    private BigDecimal bonusAmount;
    private LocalDate startDate;
    private Instant expiresAt;
    private Instant sentAt;
    private Instant viewedAt;
    private Instant respondedAt;
    private String candidateResponse;
    private ESignatureProvider esignatureProvider;
    private String esignatureEnvelopeId;
    private ESignatureStatus esignatureStatus;
    private Instant esignatureSignedAt;
    private String esignatureDocumentUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OfferApprovalResponse> approvals;
}
