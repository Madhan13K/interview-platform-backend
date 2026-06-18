package com.interview_platform_backend.interview_platform_backend.gdpr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRequest {

    @NotBlank(message = "Consent type is required")
    private String consentType;

    @NotNull(message = "Granted status is required")
    private Boolean granted;
}
