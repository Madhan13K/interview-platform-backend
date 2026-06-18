package com.interview_platform_backend.interview_platform_backend.team.service;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.team.dto.*;
import com.interview_platform_backend.interview_platform_backend.team.entity.*;
import com.interview_platform_backend.interview_platform_backend.team.repository.*;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository memberRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    public TeamResponse createTeam(CreateTeamRequest request) {
        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .department(request.getDepartment())
                .isActive(true)
                .build();

        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getManagerId()));
            team.setManager(manager);
        }

        Team saved = teamRepository.save(team);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(UUID teamId) {
        Team team = teamRepository.findByIdWithMembers(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
        return mapToResponse(team);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        return teamRepository.findByIsActiveTrue().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsByDepartment(String department) {
        return teamRepository.findByDepartment(department).stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getMyTeams(UUID userId) {
        return teamRepository.findByMemberUserId(userId).stream().map(this::mapToResponse).toList();
    }

    public TeamResponse updateTeam(UUID teamId, CreateTeamRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
        if (request.getName() != null) team.setName(request.getName());
        if (request.getDescription() != null) team.setDescription(request.getDescription());
        if (request.getDepartment() != null) team.setDepartment(request.getDepartment());
        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getManagerId()));
            team.setManager(manager);
        }
        return mapToResponse(teamRepository.save(team));
    }

    public void deleteTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
        team.setIsActive(false);
        teamRepository.save(team);
    }

    public TeamResponse addMember(UUID teamId, UUID userId, TeamMember.TeamRole role) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (memberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new DuplicateResourceException("User is already a member of this team");
        }

        TeamMember member = TeamMember.builder()
                .team(team).user(user)
                .role(role != null ? role : TeamMember.TeamRole.MEMBER)
                .build();
        memberRepository.save(member);

        return getTeam(teamId);
    }

    public TeamResponse removeMember(UUID teamId, UUID userId) {
        TeamMember member = memberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("TeamMember", "userId", userId));
        memberRepository.delete(member);
        return getTeam(teamId);
    }

    public TeamResponse updateMemberRole(UUID teamId, UUID userId, TeamMember.TeamRole role) {
        TeamMember member = memberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("TeamMember", "userId", userId));
        member.setRole(role);
        memberRepository.save(member);
        return getTeam(teamId);
    }

    private TeamResponse mapToResponse(Team team) {
        List<TeamResponse.TeamMemberDto> memberDtos = team.getMembers() != null
                ? team.getMembers().stream().map(m -> TeamResponse.TeamMemberDto.builder()
                    .id(m.getId())
                    .userId(m.getUser().getId())
                    .userName(m.getUser().getFirstName() + " " + m.getUser().getLastName())
                    .email(m.getUser().getEmail())
                    .role(m.getRole())
                    .joinedAt(m.getJoinedAt())
                    .build()).toList()
                : List.of();

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .department(team.getDepartment())
                .managerId(team.getManager() != null ? team.getManager().getId() : null)
                .managerName(team.getManager() != null ? team.getManager().getFirstName() + " " + team.getManager().getLastName() : null)
                .isActive(team.getIsActive())
                .createdAt(team.getCreatedAt())
                .members(memberDtos)
                .build();
    }
}

