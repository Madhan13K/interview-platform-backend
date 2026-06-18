package com.interview_platform_backend.interview_platform_backend.accountlockout.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpBlockRequest {

    @NotBlank(message = "IP address is required")
    private String ipAddress;

    @NotBlank(message = "Reason is required")
    private String reason;

    /**
     * Block duration in minutes. Null or 0 = permanent.
     */
    private Integer durationMinutes;
}
