package com.interview_platform_backend.interview_platform_backend.sourcetracking.dto;

import com.interview_platform_backend.interview_platform_backend.sourcetracking.entity.SourceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackSourceRequest {

    @NotNull(message = "Application ID is required")
    private UUID applicationId;

    @NotNull(message = "Source is required")
    private SourceType source;

    private String sourceCampaign;
    private BigDecimal costPerClick;
    private BigDecimal totalSpend;
}
