package com.interview_platform_backend.interview_platform_backend.bulk.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkInviteCandidatesRequest {

    @NotNull
    private UUID interviewId;

    @NotEmpty(message = "At least one candidate must be provided")
    private List<CandidateInvite> candidates;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CandidateInvite {
        private UUID candidateId;
        private String email;
        private String firstName;
        private String lastName;
        private String customMessage;
    }
}

