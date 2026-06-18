package com.interview_platform_backend.interview_platform_backend.candidate.dto;

import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewInterviewerResponse {
    private UUID interviewerId;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean primaryInterviewer;

}
