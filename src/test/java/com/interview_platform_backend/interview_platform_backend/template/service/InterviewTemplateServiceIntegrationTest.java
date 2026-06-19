package com.interview_platform_backend.interview_platform_backend.template.service;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.interview_platform_backend.interview_platform_backend.candidate.dto.InterviewResponse;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewMode;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.Question;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionCategory;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionDifficulty;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionType;
import com.interview_platform_backend.interview_platform_backend.questionbank.repository.QuestionCategoryRepository;
import com.interview_platform_backend.interview_platform_backend.questionbank.repository.QuestionRepository;
import com.interview_platform_backend.interview_platform_backend.template.dto.*;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class InterviewTemplateServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InterviewTemplateService templateService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionCategoryRepository categoryRepository;

    private User testUser;
    private Question testQuestion;
    private String templateTitle;

    @BeforeEach
    void setUp() {
        templateTitle = "Template_" + UUID.randomUUID().toString().substring(0, 8);

        testUser = userRepository.save(User.builder()
                .firstName("Template")
                .lastName("Creator")
                .email("template-" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        QuestionCategory category = categoryRepository.save(QuestionCategory.builder()
                .name("Cat_" + UUID.randomUUID().toString().substring(0, 6))
                .description("Test category")
                .build());

        testQuestion = questionRepository.save(Question.builder()
                .title("What is polymorphism?")
                .description("Explain OOP polymorphism")
                .category(category)
                .difficulty(QuestionDifficulty.MEDIUM)
                .type(QuestionType.THEORETICAL)
                .expectedDurationMinutes(10)
                .isActive(true)
                .createdAt(Instant.now())
                .build());
    }

    private CreateTemplateRequest buildCreateRequest() {
        return CreateTemplateRequest.builder()
                .title(templateTitle)
                .description("A standard technical interview template")
                .type(InterviewType.TECHNICAL)
                .mode(InterviewMode.ONLINE)
                .durationMinutes(60)
                .evaluationCriteria("Problem solving: 1-5, Communication: 1-5, Code quality: 1-5")
                .instructions("Start with introductions, then move to coding problems")
                .tags("java,backend,senior")
                .build();
    }

    private CreateTemplateRequest buildCreateRequestWithQuestions() {
        TemplateQuestionRequest qr = TemplateQuestionRequest.builder()
                .questionId(testQuestion.getId())
                .orderIndex(1)
                .isMandatory(true)
                .timeAllocationMinutes(15)
                .notes("Warm-up question")
                .build();

        CreateTemplateRequest request = buildCreateRequest();
        request.setQuestions(List.of(qr));
        return request;
    }

    @Nested
    @DisplayName("Create Template")
    class CreateTemplate {

        @Test
        @DisplayName("should create template without questions")
        void createTemplate_success() {
            TemplateResponse response = templateService.createTemplate(buildCreateRequest(), testUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getTitle()).isEqualTo(templateTitle);
            assertThat(response.getType()).isEqualTo(InterviewType.TECHNICAL);
            assertThat(response.getMode()).isEqualTo(InterviewMode.ONLINE);
            assertThat(response.getDurationMinutes()).isEqualTo(60);
            assertThat(response.getIsActive()).isTrue();
            assertThat(response.getCreatedById()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("should create template with questions")
        void createTemplate_withQuestions() {
            TemplateResponse response = templateService.createTemplate(buildCreateRequestWithQuestions(), testUser.getId());

            assertThat(response.getQuestions()).hasSize(1);
            assertThat(response.getQuestions().get(0).getQuestionTitle()).isEqualTo("What is polymorphism?");
            assertThat(response.getQuestions().get(0).getOrderIndex()).isEqualTo(1);
            assertThat(response.getQuestions().get(0).getIsMandatory()).isTrue();
            assertThat(response.getQuestions().get(0).getTimeAllocationMinutes()).isEqualTo(15);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException for duplicate title")
        void createTemplate_duplicateTitle() {
            templateService.createTemplate(buildCreateRequest(), testUser.getId());

            assertThatThrownBy(() -> templateService.createTemplate(buildCreateRequest(), testUser.getId()))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("Get Template")
    class GetTemplate {

        @Test
        @DisplayName("should get template by ID with questions")
        void getTemplate_success() {
            TemplateResponse created = templateService.createTemplate(buildCreateRequestWithQuestions(), testUser.getId());
            TemplateResponse found = templateService.getTemplate(created.getId());

            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(created.getId());
            assertThat(found.getQuestions()).hasSize(1);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid ID")
        void getTemplate_notFound() {
            assertThatThrownBy(() -> templateService.getTemplate(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should get all active templates")
        void getAllTemplates() {
            templateService.createTemplate(buildCreateRequest(), testUser.getId());
            List<TemplateResponse> templates = templateService.getAllTemplates();

            assertThat(templates).isNotEmpty();
            assertThat(templates).anyMatch(t -> t.getTitle().equals(templateTitle));
        }

        @Test
        @DisplayName("should get templates paginated")
        void getTemplatesPaginated() {
            templateService.createTemplate(buildCreateRequest(), testUser.getId());
            PaginatedResponse<TemplateResponse> result = templateService.getTemplatesPaginated(0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("should filter templates by type")
        void getTemplatesByType() {
            templateService.createTemplate(buildCreateRequest(), testUser.getId());
            List<TemplateResponse> templates = templateService.getTemplatesByType(InterviewType.TECHNICAL);

            assertThat(templates).isNotEmpty();
            assertThat(templates).allMatch(t -> t.getType() == InterviewType.TECHNICAL);
        }

        @Test
        @DisplayName("should search templates by keyword")
        void searchTemplates() {
            templateService.createTemplate(buildCreateRequest(), testUser.getId());
            List<TemplateResponse> templates = templateService.searchTemplates("backend");

            assertThat(templates).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Update Template")
    class UpdateTemplate {

        @Test
        @DisplayName("should update template fields")
        void updateTemplate_success() {
            TemplateResponse created = templateService.createTemplate(buildCreateRequest(), testUser.getId());

            UpdateTemplateRequest updateRequest = UpdateTemplateRequest.builder()
                    .title("Updated " + templateTitle)
                    .durationMinutes(90)
                    .evaluationCriteria("Updated criteria")
                    .build();

            TemplateResponse updated = templateService.updateTemplate(created.getId(), updateRequest);

            assertThat(updated.getTitle()).isEqualTo("Updated " + templateTitle);
            assertThat(updated.getDurationMinutes()).isEqualTo(90);
            assertThat(updated.getEvaluationCriteria()).isEqualTo("Updated criteria");
        }

        @Test
        @DisplayName("should deactivate template")
        void updateTemplate_deactivate() {
            TemplateResponse created = templateService.createTemplate(buildCreateRequest(), testUser.getId());

            UpdateTemplateRequest updateRequest = UpdateTemplateRequest.builder()
                    .isActive(false)
                    .build();

            TemplateResponse updated = templateService.updateTemplate(created.getId(), updateRequest);
            assertThat(updated.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid ID")
        void updateTemplate_notFound() {
            UpdateTemplateRequest updateRequest = UpdateTemplateRequest.builder()
                    .title("Should fail")
                    .build();

            assertThatThrownBy(() -> templateService.updateTemplate(UUID.randomUUID(), updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Template")
    class DeleteTemplate {

        @Test
        @DisplayName("should delete template")
        void deleteTemplate_success() {
            TemplateResponse created = templateService.createTemplate(buildCreateRequest(), testUser.getId());
            templateService.deleteTemplate(created.getId());

            assertThatThrownBy(() -> templateService.getTemplate(created.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid ID")
        void deleteTemplate_notFound() {
            assertThatThrownBy(() -> templateService.deleteTemplate(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Template Questions Management")
    class TemplateQuestions {

        @Test
        @DisplayName("should add question to template")
        void addQuestion_success() {
            TemplateResponse created = templateService.createTemplate(buildCreateRequest(), testUser.getId());

            TemplateQuestionRequest qr = TemplateQuestionRequest.builder()
                    .questionId(testQuestion.getId())
                    .orderIndex(1)
                    .isMandatory(true)
                    .timeAllocationMinutes(10)
                    .notes("Added later")
                    .build();

            TemplateResponse updated = templateService.addQuestionToTemplate(created.getId(), qr);

            assertThat(updated.getQuestions()).hasSize(1);
            assertThat(updated.getQuestions().get(0).getQuestionId()).isEqualTo(testQuestion.getId());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when question already in template")
        void addQuestion_duplicate() {
            TemplateResponse created = templateService.createTemplate(buildCreateRequestWithQuestions(), testUser.getId());

            TemplateQuestionRequest qr = TemplateQuestionRequest.builder()
                    .questionId(testQuestion.getId())
                    .orderIndex(2)
                    .build();

            assertThatThrownBy(() -> templateService.addQuestionToTemplate(created.getId(), qr))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should remove question from template")
        void removeQuestion_success() {
            TemplateResponse created = templateService.createTemplate(buildCreateRequestWithQuestions(), testUser.getId());
            UUID templateQuestionId = created.getQuestions().get(0).getId();

            TemplateResponse updated = templateService.removeQuestionFromTemplate(created.getId(), templateQuestionId);

            assertThat(updated.getQuestions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Create Interview from Template")
    class CreateFromTemplate {

        @Test
        @DisplayName("should create interview from template")
        void createInterviewFromTemplate_success() {
            TemplateResponse created = templateService.createTemplate(buildCreateRequest(), testUser.getId());

            User candidate = userRepository.save(User.builder()
                    .firstName("Candidate")
                    .lastName("FromTemplate")
                    .email("candidate-tpl-" + UUID.randomUUID() + "@test.com")
                    .password("encoded-password")
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build());

            User interviewer = userRepository.save(User.builder()
                    .firstName("Interviewer")
                    .lastName("FromTemplate")
                    .email("interviewer-tpl-" + UUID.randomUUID() + "@test.com")
                    .password("encoded-password")
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build());

            CreateInterviewFromTemplateRequest request = CreateInterviewFromTemplateRequest.builder()
                    .templateId(created.getId())
                    .candidateId(candidate.getId())
                    .startTime(Instant.now().plus(1, ChronoUnit.DAYS))
                    .timeZone("Asia/Kolkata")
                    .interviewerIds(List.of(interviewer.getId()))
                    .build();

            InterviewResponse interview = templateService.createInterviewFromTemplate(request, testUser.getId());

            assertThat(interview).isNotNull();
            assertThat(interview.getTitle()).isEqualTo(templateTitle);
            assertThat(interview.getType()).isEqualTo(InterviewType.TECHNICAL);
            assertThat(interview.getMode()).isEqualTo(InterviewMode.ONLINE);
            assertThat(interview.getCandidateId()).isEqualTo(candidate.getId());
            // End time = start + 60 minutes (template duration)
            assertThat(interview.getEndTime()).isAfter(interview.getStartTime());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for invalid template ID")
        void createInterviewFromTemplate_templateNotFound() {
            CreateInterviewFromTemplateRequest request = CreateInterviewFromTemplateRequest.builder()
                    .templateId(UUID.randomUUID())
                    .candidateId(testUser.getId())
                    .startTime(Instant.now().plus(1, ChronoUnit.DAYS))
                    .interviewerIds(List.of(testUser.getId()))
                    .build();

            assertThatThrownBy(() -> templateService.createInterviewFromTemplate(request, testUser.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

