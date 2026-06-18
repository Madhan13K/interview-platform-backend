package com.interview_platform_backend.interview_platform_backend.security.mfa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyRequest {
    @NotBlank(message = "Code is required")
    private String code;
}
