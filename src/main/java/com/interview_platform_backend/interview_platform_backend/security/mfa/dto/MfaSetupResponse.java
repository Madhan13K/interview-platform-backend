package com.interview_platform_backend.interview_platform_backend.security.mfa.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaSetupResponse {
    private String secretKey;
    private String qrCodeUri;
    private List<String> backupCodes;
}
