package com.interview_platform_backend.interview_platform_backend.tenant.service;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.tenant.dto.*;
import com.interview_platform_backend.interview_platform_backend.tenant.entity.Organization;
import com.interview_platform_backend.interview_platform_backend.tenant.entity.Organization.OrganizationPlan;
import com.interview_platform_backend.interview_platform_backend.tenant.entity.OrganizationMember;
import com.interview_platform_backend.interview_platform_backend.tenant.entity.OrganizationMember.MemberRole;
import com.interview_platform_backend.interview_platform_backend.tenant.repository.OrganizationMemberRepository;
import com.interview_platform_backend.interview_platform_backend.tenant.repository.OrganizationRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SecurityHelper securityHelper;

    public OrganizationService(OrganizationRepository organizationRepository,
                               OrganizationMemberRepository memberRepository,
                               UserRepository userRepository,
                               SecurityHelper securityHelper) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.securityHelper = securityHelper;
    }

    // ==================== Organization CRUD ====================

    @CacheEvict(value = "organizations", allEntries = true)
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        if (organizationRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Organization", "slug", request.getSlug());
        }

        OrganizationPlan plan = OrganizationPlan.FREE;
        if (request.getPlan() != null) {
            try {
                plan = OrganizationPlan.valueOf(request.getPlan().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid plan: " + request.getPlan());
            }
        }

        Integer maxUsers = getMaxUsersForPlan(plan);

        Organization organization = Organization.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .domain(request.getDomain())
                .logoUrl(request.getLogoUrl())
                .plan(plan)
                .maxUsers(maxUsers)
                .build();

        Organization saved = organizationRepository.save(organization);

        // Add creator as OWNER
        UUID currentUserId = securityHelper.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        OrganizationMember ownerMember = OrganizationMember.builder()
                .organization(saved)
                .user(currentUser)
                .role(MemberRole.OWNER)
                .build();
        memberRepository.save(ownerMember);

        return toOrganizationResponse(saved);
    }

    @Cacheable(value = "organizations", key = "#organizationId")
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));
        return toOrganizationResponse(organization);
    }

    @CacheEvict(value = "organizations", allEntries = true)
    public OrganizationResponse updateOrganization(UUID organizationId, UpdateOrganizationRequest request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        if (request.getName() != null) {
            organization.setName(request.getName());
        }
        if (request.getSlug() != null) {
            if (!request.getSlug().equals(organization.getSlug()) && organizationRepository.existsBySlug(request.getSlug())) {
                throw new DuplicateResourceException("Organization", "slug", request.getSlug());
            }
            organization.setSlug(request.getSlug());
        }
        if (request.getDomain() != null) {
            organization.setDomain(request.getDomain());
        }
        if (request.getLogoUrl() != null) {
            organization.setLogoUrl(request.getLogoUrl());
        }
        if (request.getPlan() != null) {
            try {
                OrganizationPlan plan = OrganizationPlan.valueOf(request.getPlan().toUpperCase());
                organization.setPlan(plan);
                organization.setMaxUsers(getMaxUsersForPlan(plan));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid plan: " + request.getPlan());
            }
        }
        if (request.getMaxUsers() != null) {
            organization.setMaxUsers(request.getMaxUsers());
        }
        if (request.getIsActive() != null) {
            organization.setIsActive(request.getIsActive());
        }

        Organization saved = organizationRepository.save(organization);
        return toOrganizationResponse(saved);
    }

    @CacheEvict(value = "organizations", allEntries = true)
    public void deleteOrganization(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));
        organizationRepository.delete(organization);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getOrganizationsByUser() {
        UUID currentUserId = securityHelper.getCurrentUserId();
        List<OrganizationMember> memberships = memberRepository.findByUserId(currentUserId);
        return memberships.stream()
                .map(member -> toOrganizationResponse(member.getOrganization()))
                .toList();
    }

    // ==================== Member Management ====================

    @CacheEvict(value = "organizations", allEntries = true)
    public OrganizationMemberResponse addMember(UUID organizationId, AddMemberRequest request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        if (memberRepository.existsByOrganizationIdAndUserId(organizationId, request.getUserId())) {
            throw new DuplicateResourceException("OrganizationMember", "userId", request.getUserId());
        }

        long currentMemberCount = memberRepository.countByOrganizationId(organizationId);
        if (currentMemberCount >= organization.getMaxUsers()) {
            throw new BadRequestException("Organization has reached maximum member limit of " + organization.getMaxUsers());
        }

        MemberRole role = MemberRole.MEMBER;
        if (request.getRole() != null) {
            try {
                role = MemberRole.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid role: " + request.getRole());
            }
        }

        OrganizationMember member = OrganizationMember.builder()
                .organization(organization)
                .user(user)
                .role(role)
                .build();

        OrganizationMember saved = memberRepository.save(member);
        return toMemberResponse(saved);
    }

    @CacheEvict(value = "organizations", allEntries = true)
    public void removeMember(UUID organizationId, UUID userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        List<OrganizationMember> members = memberRepository.findByOrganizationId(organizationId);
        OrganizationMember member = members.stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationMember", "userId", userId));

        if (member.getRole() == MemberRole.OWNER) {
            long ownerCount = members.stream()
                    .filter(m -> m.getRole() == MemberRole.OWNER)
                    .count();
            if (ownerCount <= 1) {
                throw new BadRequestException("Cannot remove the last owner of the organization");
            }
        }

        memberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> getMembers(UUID organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", "id", organizationId);
        }

        List<OrganizationMember> members = memberRepository.findByOrganizationId(organizationId);
        return members.stream()
                .map(this::toMemberResponse)
                .toList();
    }

    public OrganizationMemberResponse updateMemberRole(UUID organizationId, UUID userId, String newRole) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", "id", organizationId);
        }

        List<OrganizationMember> members = memberRepository.findByOrganizationId(organizationId);
        OrganizationMember member = members.stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationMember", "userId", userId));

        MemberRole role;
        try {
            role = MemberRole.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + newRole);
        }

        // Prevent removing the last owner
        if (member.getRole() == MemberRole.OWNER && role != MemberRole.OWNER) {
            long ownerCount = members.stream()
                    .filter(m -> m.getRole() == MemberRole.OWNER)
                    .count();
            if (ownerCount <= 1) {
                throw new BadRequestException("Cannot change role of the last owner");
            }
        }

        member.setRole(role);
        OrganizationMember saved = memberRepository.save(member);
        return toMemberResponse(saved);
    }

    // ==================== Helpers ====================

    private OrganizationResponse toOrganizationResponse(Organization organization) {
        long memberCount = memberRepository.countByOrganizationId(organization.getId());
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .slug(organization.getSlug())
                .domain(organization.getDomain())
                .logoUrl(organization.getLogoUrl())
                .plan(organization.getPlan().name())
                .maxUsers(organization.getMaxUsers())
                .isActive(organization.getIsActive())
                .memberCount(memberCount)
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }

    private OrganizationMemberResponse toMemberResponse(OrganizationMember member) {
        User user = member.getUser();
        return OrganizationMemberResponse.builder()
                .id(member.getId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userName(user.getFirstName() + " " + user.getLastName())
                .role(member.getRole().name())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private Integer getMaxUsersForPlan(OrganizationPlan plan) {
        return switch (plan) {
            case FREE -> 5;
            case STARTER -> 25;
            case PROFESSIONAL -> 100;
            case ENTERPRISE -> 1000;
        };
    }
}
