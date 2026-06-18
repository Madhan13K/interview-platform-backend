package com.interview_platform_backend.interview_platform_backend.bulk.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkInviteResult {
    private String email;
    private String candidateName;
    private boolean sent;
    private String message;
}

