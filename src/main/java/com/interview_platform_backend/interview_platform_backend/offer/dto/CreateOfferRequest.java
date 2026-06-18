package com.interview_platform_backend.interview_platform_backend.offer.dto;

import com.interview_platform_backend.interview_platform_backend.offer.entity.ESignatureProvider;
import jakarta.validation.constraints.NotNull;
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
public class CreateOfferRequest {

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    @NotNull(message = "Job Position ID is required")
    private UUID jobPositionId;

    private String offerContent;

    @NotNull(message = "Salary offered is required")
    private BigDecimal salaryOffered;

    @Builder.Default
    private String salaryCurrency = "USD";

    private BigDecimal bonusAmount;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private Instant expiresAt;

    private List<UUID> approverIds;

    @Builder.Default
    private ESignatureProvider esignatureProvider = ESignatureProvider.NONE;
}
