package com.interview_platform_backend.interview_platform_backend.jobboard.dto;

import com.interview_platform_backend.interview_platform_backend.jobboard.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusUpdate {

    @NotNull(message = "Status is required")
    private ApplicationStatus status;

    private String notes;
}
