package com.interview_platform_backend.interview_platform_backend.team.dto;

import com.interview_platform_backend.interview_platform_backend.team.entity.TeamMember;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamResponse {
    private UUID id;
    private String name;
    private String description;
    private String department;
    private UUID managerId;
    private String managerName;
    private Boolean isActive;
    private Instant createdAt;
    private List<TeamMemberDto> members;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TeamMemberDto {
        private UUID id;
        private UUID userId;
        private String userName;
        private String email;
        private TeamMember.TeamRole role;
        private Instant joinedAt;
    }
}

