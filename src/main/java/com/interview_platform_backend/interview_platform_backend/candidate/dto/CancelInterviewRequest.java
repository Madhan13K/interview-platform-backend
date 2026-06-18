package com.interview_platform_backend.interview_platform_backend.candidate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelInterviewRequest {

    @NotBlank(message = "Cancel reason is required")
    private String reason;
}

