package com.interview_platform_backend.interview_platform_backend.team.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateTeamRequest {
    @NotBlank private String name;
    private String description;
    private String department;
    private UUID managerId;
}

