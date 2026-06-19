package com.interview_platform_backend.interview_platform_backend.tenant.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.tenant.dto.*;
import com.interview_platform_backend.interview_platform_backend.tenant.entity.Organization;
import com.interview_platform_backend.interview_platform_backend.tenant.repository.OrganizationMemberRepository;
import com.interview_platform_backend.interview_platform_backend.tenant.repository.OrganizationRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class OrganizationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "org-test-" + UUID.randomUUID() + "@example.com";
        testUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email(testEmail)
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        testUser = userRepository.save(testUser);

        // Set up security context so SecurityHelper.getCurrentUserId() works
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(testEmail, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private CreateOrganizationRequest buildCreateRequest() {
        return CreateOrganizationRequest.builder()
                .name("Test Organization")
                .slug("test-org-" + UUID.randomUUID().toString().substring(0, 8))
                .domain("test.com")
                .logoUrl("https://example.com/logo.png")
                .plan("FREE")
                .build();
    }

    private CreateOrganizationRequest buildCreateRequestWithSlug(String slug) {
        return CreateOrganizationRequest.builder()
                .name("Test Organization")
                .slug(slug)
                .domain("test.com")
                .logoUrl("https://example.com/logo.png")
                .plan("FREE")
                .build();
    }

    @Nested
    @DisplayName("Create Organization")
    class CreateOrganization {

        @Test
        @DisplayName("should create organization successfully")
        void createOrganization_success() {
            CreateOrganizationRequest request = buildCreateRequest();
            OrganizationResponse response = organizationService.createOrganization(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getName()).isEqualTo("Test Organization");
            assertThat(response.getSlug()).isEqualTo(request.getSlug());
            assertThat(response.getDomain()).isEqualTo("test.com");
            assertThat(response.getLogoUrl()).isEqualTo("https://example.com/logo.png");
            assertThat(response.getPlan()).isEqualTo("FREE");
            assertThat(response.getMaxUsers()).isEqualTo(5);
            assertThat(response.getIsActive()).isTrue();
            assertThat(response.getMemberCount()).isEqualTo(1L); // Creator is auto-added as OWNER
        }

        @Test
        @DisplayName("should create organization with STARTER plan")
        void createOrganization_starterPlan() {
            CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                    .name("Starter Org")
                    .slug("starter-org-" + UUID.randomUUID().toString().substring(0, 8))
                    .plan("STARTER")
                    .build();

            OrganizationResponse response = organizationService.createOrganization(request);

            assertThat(response.getPlan()).isEqualTo("STARTER");
            assertThat(response.getMaxUsers()).isEqualTo(25);
        }

        @Test
        @DisplayName("should create organization with PROFESSIONAL plan")
        void createOrganization_professionalPlan() {
            CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                    .name("Pro Org")
                    .slug("pro-org-" + UUID.randomUUID().toString().substring(0, 8))
                    .plan("PROFESSIONAL")
                    .build();

            OrganizationResponse response = organizationService.createOrganization(request);

            assertThat(response.getPlan()).isEqualTo("PROFESSIONAL");
            assertThat(response.getMaxUsers()).isEqualTo(100);
        }

        @Test
        @DisplayName("should create organization with ENTERPRISE plan")
        void createOrganization_enterprisePlan() {
            CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                    .name("Enterprise Org")
                    .slug("enterprise-org-" + UUID.randomUUID().toString().substring(0, 8))
                    .plan("ENTERPRISE")
                    .build();

            OrganizationResponse response = organizationService.createOrganization(request);

            assertThat(response.getPlan()).isEqualTo("ENTERPRISE");
            assertThat(response.getMaxUsers()).isEqualTo(1000);
        }

        @Test
        @DisplayName("should default to FREE plan when plan is null")
        void createOrganization_defaultPlan() {
            CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                    .name("Default Plan Org")
                    .slug("default-org-" + UUID.randomUUID().toString().substring(0, 8))
                    .build();

            OrganizationResponse response = organizationService.createOrganization(request);

            assertThat(response.getPlan()).isEqualTo("FREE");
            assertThat(response.getMaxUsers()).isEqualTo(5);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for duplicate slug")
        void createOrganization_duplicateSlug() {
            String slug = "duplicate-slug-" + UUID.randomUUID().toString().substring(0, 8);
            organizationService.createOrganization(buildCreateRequestWithSlug(slug));

            assertThatThrownBy(() -> organizationService.createOrganization(buildCreateRequestWithSlug(slug)))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("slug");
        }

        @Test
        @DisplayName("should throw BadRequestException for invalid plan")
        void createOrganization_invalidPlan() {
            CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                    .name("Invalid Plan Org")
                    .slug("invalid-plan-" + UUID.randomUUID().toString().substring(0, 8))
                    .plan("INVALID_PLAN")
                    .build();

            assertThatThrownBy(() -> organizationService.createOrganization(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid plan");
        }

        @Test
        @DisplayName("should add creator as OWNER member")
        void createOrganization_addsCreatorAsOwner() {
            CreateOrganizationRequest request = buildCreateRequest();
            OrganizationResponse response = organizationService.createOrganization(request);

            List<OrganizationMemberResponse> members = organizationService.getMembers(response.getId());
            assertThat(members).hasSize(1);
            assertThat(members.get(0).getUserId()).isEqualTo(testUser.getId());
            assertThat(members.get(0).getRole()).isEqualTo("OWNER");
        }
    }

    @Nested
    @DisplayName("Get Organization")
    class GetOrganization {

        @Test
        @DisplayName("should get organization by ID successfully")
        void getOrganization_success() {
            OrganizationResponse created = organizationService.createOrganization(buildCreateRequest());
            OrganizationResponse fetched = organizationService.getOrganization(created.getId());

            assertThat(fetched).isNotNull();
            assertThat(fetched.getId()).isEqualTo(created.getId());
            assertThat(fetched.getName()).isEqualTo(created.getName());
            assertThat(fetched.getSlug()).isEqualTo(created.getSlug());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent ID")
        void getOrganization_notFound() {
            UUID randomId = UUID.randomUUID();
            assertThatThrownBy(() -> organizationService.getOrganization(randomId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Organization");
        }
    }

    @Nested
    @DisplayName("Update Organization")
    class UpdateOrganization {

        @Test
        @DisplayName("should update organization name")
        void updateOrganization_name() {
            OrganizationResponse created = organizationService.createOrganization(buildCreateRequest());

            UpdateOrganizationRequest updateRequest = UpdateOrganizationRequest.builder()
                    .name("Updated Name")
                    .build();

            OrganizationResponse updated = organizationService.updateOrganization(created.getId(), updateRequest);

            assertThat(updated.getName()).isEqualTo("Updated Name");
            assertThat(updated.getSlug()).isEqualTo(created.getSlug()); // unchanged
        }

        @Test
        @DisplayName("should update organization domain and logoUrl")
        void updateOrganization_domainAndLogo() {
            OrganizationResponse created = organizationService.createOrganization(buildCreateRequest());

            UpdateOrganizationRequest updateRequest = UpdateOrganizationRequest.builder()
                    .domain("newdomain.com")
                    .logoUrl("https://new.com/logo.png")
                    .build();

            OrganizationResponse updated = organizationService.updateOrganization(created.getId(), updateRequest);

            assertThat(updated.getDomain()).isEqualTo("newdomain.com");
            assertThat(updated.getLogoUrl()).isEqualTo("https://new.com/logo.png");
        }

        @Test
        @DisplayName("should update organization plan")
        void updateOrganization_plan() {
            OrganizationResponse created = organizationService.createOrganization(buildCreateRequest());

            UpdateOrganizationRequest updateRequest = UpdateOrganizationRequest.builder()
                    .plan("PROFESSIONAL")
                    .build();

            OrganizationResponse updated = organizationService.updateOrganization(created.getId(), updateRequest);

            assertThat(updated.getPlan()).isEqualTo("PROFESSIONAL");
            assertThat(updated.getMaxUsers()).isEqualTo(100);
        }

        @Test
        @DisplayName("should update organization isActive status")
        void updateOrganization_deactivate() {
            OrganizationResponse created = organizationService.createOrganization(buildCreateRequest());

            UpdateOrganizationRequest updateRequest = UpdateOrganizationRequest.builder()
                    .isActive(false)
                    .build();

            OrganizationResponse updated = organizationService.updateOrganization(created.getId(), updateRequest);

            assertThat(updated.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("should update organization maxUsers")
        void updateOrganization_maxUsers() {
            OrganizationResponse created = organizationService.createOrganization(buildCreateRequest());

            UpdateOrganizationRequest updateRequest = UpdateOrganizationRequest.builder()
                    .maxUsers(50)
                    .build();

            OrganizationResponse updated = organizationService.updateOrganization(created.getId(), updateRequest);

            assertThat(updated.getMaxUsers()).isEqualTo(50);
        }

        @Test
        @DisplayName("should update organization slug to a new unique slug")
        void updateOrganization_slug() {
            OrganizationResponse created = organizationService.createOrganization(buildCreateRequest());
            String newSlug = "new-slug-" + UUID.randomUUID().toString().substring(0, 8);

            UpdateOrganizationRequest updateRequest = UpdateOrganizationRequest.builder()
                    .slug(newSlug)
                    .build();

            OrganizationResponse updated = organizationService.updateOrganization(created.getId(), updateRequest);

            assertThat(updated.getSlug()).isEqualTo(newSlug);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when updating to existing slug")
        void updateOrganization_duplicateSlug() {
            OrganizationResponse org1 = organizationService.createOrganization(buildCreateRequest());
            OrganizationResponse org2 = organizationService.createOrganization(buildCreateRequest());

            UpdateOrganizationRequest updateRequest = UpdateOrganizationRequest.builder()
                    .slug(org1.getSlug())
                    .build();

            assertThatThrownBy(() -> organizationService.updateOrganization(org2.getId(), updateRequest))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent organization")
        void updateOrganization_notFound() {
            UUID randomId = UUID.randomUUID();
            UpdateOrganizationRequest updateRequest = UpdateOrganizationRequest.builder()
                    .name("New Name")
                    .build();

            assertThatThrownBy(() -> organizationService.updateOrganization(randomId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException for invalid plan on update")
        void updateOrganization_invalidPlan() {
            OrganizationResponse created = organizationService.createOrganization(buildCreateRequest());

            UpdateOrganizationRequest updateRequest = UpdateOrganizationRequest.builder()
                    .plan("BOGUS")
                    .build();

            assertThatThrownBy(() -> organizationService.updateOrganization(created.getId(), updateRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid plan");
        }
    }

    @Nested
    @DisplayName("Delete Organization")
    class DeleteOrganization {

        @Test
        @DisplayName("should delete organization successfully")
        void deleteOrganization_success() {
            OrganizationResponse created = organizationService.createOrganization(buildCreateRequest());
            organizationService.deleteOrganization(created.getId());

            assertThat(organizationRepository.findById(created.getId())).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent organization")
        void deleteOrganization_notFound() {
            UUID randomId = UUID.randomUUID();
            assertThatThrownBy(() -> organizationService.deleteOrganization(randomId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Organizations By User")
    class GetOrganizationsByUser {

        @Test
        @DisplayName("should return organizations for current user")
        void getOrganizationsByUser_success() {
            organizationService.createOrganization(buildCreateRequest());
            organizationService.createOrganization(buildCreateRequest());

            List<OrganizationResponse> organizations = organizationService.getOrganizationsByUser();

            assertThat(organizations).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should return empty list when user has no organizations")
        void getOrganizationsByUser_empty() {
            // Create a different user with no orgs
            String otherEmail = "other-" + UUID.randomUUID() + "@example.com";
            User otherUser = User.builder()
                    .firstName("Other")
                    .lastName("User")
                    .email(otherEmail)
                    .password("encoded-password")
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();
            userRepository.save(otherUser);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(otherEmail, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            List<OrganizationResponse> organizations = organizationService.getOrganizationsByUser();

            assertThat(organizations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Member Management")
    class MemberManagement {

        private OrganizationResponse createdOrg;
        private User secondUser;

        @BeforeEach
        void setUpMembers() {
            createdOrg = organizationService.createOrganization(buildCreateRequest());

            String secondEmail = "member-" + UUID.randomUUID() + "@example.com";
            secondUser = User.builder()
                    .firstName("Second")
                    .lastName("Member")
                    .email(secondEmail)
                    .password("encoded-password")
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();
            secondUser = userRepository.save(secondUser);
        }

        @Test
        @DisplayName("should add member successfully")
        void addMember_success() {
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(secondUser.getId())
                    .role("MEMBER")
                    .build();

            OrganizationMemberResponse response = organizationService.addMember(createdOrg.getId(), request);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(secondUser.getId());
            assertThat(response.getRole()).isEqualTo("MEMBER");
            assertThat(response.getUserEmail()).isEqualTo(secondUser.getEmail());
            assertThat(response.getUserName()).isEqualTo("Second Member");
        }

        @Test
        @DisplayName("should add member with ADMIN role")
        void addMember_adminRole() {
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(secondUser.getId())
                    .role("ADMIN")
                    .build();

            OrganizationMemberResponse response = organizationService.addMember(createdOrg.getId(), request);

            assertThat(response.getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("should default to MEMBER role when role is null")
        void addMember_defaultRole() {
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(secondUser.getId())
                    .build();

            OrganizationMemberResponse response = organizationService.addMember(createdOrg.getId(), request);

            assertThat(response.getRole()).isEqualTo("MEMBER");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for duplicate member")
        void addMember_duplicate() {
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(secondUser.getId())
                    .role("MEMBER")
                    .build();

            organizationService.addMember(createdOrg.getId(), request);

            assertThatThrownBy(() -> organizationService.addMember(createdOrg.getId(), request))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent organization")
        void addMember_orgNotFound() {
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(secondUser.getId())
                    .role("MEMBER")
                    .build();

            UUID randomOrgId = UUID.randomUUID();
            assertThatThrownBy(() -> organizationService.addMember(randomOrgId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent user")
        void addMember_userNotFound() {
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(UUID.randomUUID())
                    .role("MEMBER")
                    .build();

            assertThatThrownBy(() -> organizationService.addMember(createdOrg.getId(), request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException for invalid role")
        void addMember_invalidRole() {
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(secondUser.getId())
                    .role("INVALID_ROLE")
                    .build();

            assertThatThrownBy(() -> organizationService.addMember(createdOrg.getId(), request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid role");
        }

        @Test
        @DisplayName("should throw BadRequestException when max members reached")
        void addMember_maxMembersReached() {
            // Create org with maxUsers=1 by updating it
            UpdateOrganizationRequest updateRequest = UpdateOrganizationRequest.builder()
                    .maxUsers(1)
                    .build();
            organizationService.updateOrganization(createdOrg.getId(), updateRequest);

            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(secondUser.getId())
                    .role("MEMBER")
                    .build();

            assertThatThrownBy(() -> organizationService.addMember(createdOrg.getId(), request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("maximum member limit");
        }

        @Test
        @DisplayName("should remove member successfully")
        void removeMember_success() {
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(secondUser.getId())
                    .role("MEMBER")
                    .build();
            organizationService.addMember(createdOrg.getId(), request);

            organizationService.removeMember(createdOrg.getId(), secondUser.getId());

            List<OrganizationMemberResponse> members = organizationService.getMembers(createdOrg.getId());
            assertThat(members).noneMatch(m -> m.getUserId().equals(secondUser.getId()));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when removing non-existent member")
        void removeMember_notFound() {
            UUID randomUserId = UUID.randomUUID();
            assertThatThrownBy(() -> organizationService.removeMember(createdOrg.getId(), randomUserId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException when removing the last owner")
        void removeMember_lastOwner() {
            assertThatThrownBy(() -> organizationService.removeMember(createdOrg.getId(), testUser.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("last owner");
        }

        @Test
        @DisplayName("should get all members of organization")
        void getMembers_success() {
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(secondUser.getId())
                    .role("MEMBER")
                    .build();
            organizationService.addMember(createdOrg.getId(), request);

            List<OrganizationMemberResponse> members = organizationService.getMembers(createdOrg.getId());

            assertThat(members).hasSize(2);
            assertThat(members).anyMatch(m -> m.getUserId().equals(testUser.getId()) && m.getRole().equals("OWNER"));
            assertThat(members).anyMatch(m -> m.getUserId().equals(secondUser.getId()) && m.getRole().equals("MEMBER"));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when getting members of non-existent org")
        void getMembers_orgNotFound() {
            UUID randomOrgId = UUID.randomUUID();
            assertThatThrownBy(() -> organizationService.getMembers(randomOrgId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should update member role successfully")
        void updateMemberRole_success() {
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(secondUser.getId())
                    .role("MEMBER")
                    .build();
            organizationService.addMember(createdOrg.getId(), request);

            OrganizationMemberResponse updated = organizationService.updateMemberRole(
                    createdOrg.getId(), secondUser.getId(), "ADMIN");

            assertThat(updated.getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when updating role of non-existent member")
        void updateMemberRole_memberNotFound() {
            UUID randomUserId = UUID.randomUUID();
            assertThatThrownBy(() -> organizationService.updateMemberRole(
                    createdOrg.getId(), randomUserId, "ADMIN"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw BadRequestException for invalid role on update")
        void updateMemberRole_invalidRole() {
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(secondUser.getId())
                    .role("MEMBER")
                    .build();
            organizationService.addMember(createdOrg.getId(), request);

            assertThatThrownBy(() -> organizationService.updateMemberRole(
                    createdOrg.getId(), secondUser.getId(), "INVALID"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid role");
        }

        @Test
        @DisplayName("should throw BadRequestException when changing role of last owner")
        void updateMemberRole_lastOwner() {
            assertThatThrownBy(() -> organizationService.updateMemberRole(
                    createdOrg.getId(), testUser.getId(), "MEMBER"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("last owner");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when updating role in non-existent org")
        void updateMemberRole_orgNotFound() {
            UUID randomOrgId = UUID.randomUUID();
            assertThatThrownBy(() -> organizationService.updateMemberRole(
                    randomOrgId, testUser.getId(), "ADMIN"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
