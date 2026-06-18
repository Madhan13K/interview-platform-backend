package com.interview_platform_backend.interview_platform_backend.template.service;

import com.interview_platform_backend.interview_platform_backend.candidate.dto.CreateInterviewRequest;
import com.interview_platform_backend.interview_platform_backend.candidate.dto.InterviewResponse;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.candidate.service.InterviewService;
import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.Question;
import com.interview_platform_backend.interview_platform_backend.questionbank.repository.QuestionRepository;
import com.interview_platform_backend.interview_platform_backend.template.dto.*;
import com.interview_platform_backend.interview_platform_backend.template.entity.InterviewTemplate;
import com.interview_platform_backend.interview_platform_backend.template.entity.TemplateQuestion;
import com.interview_platform_backend.interview_platform_backend.template.repository.InterviewTemplateRepository;
import com.interview_platform_backend.interview_platform_backend.template.repository.TemplateQuestionRepository;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InterviewTemplateService {

    private final InterviewTemplateRepository templateRepository;
    private final TemplateQuestionRepository templateQuestionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final InterviewService interviewService;

    public InterviewTemplateService(InterviewTemplateRepository templateRepository,
                                     TemplateQuestionRepository templateQuestionRepository,
                                     QuestionRepository questionRepository,
                                     UserRepository userRepository,
                                     InterviewService interviewService) {
        this.templateRepository = templateRepository;
        this.templateQuestionRepository = templateQuestionRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.interviewService = interviewService;
    }

    @CacheEvict(value = "templates", allEntries = true)
    public TemplateResponse createTemplate(CreateTemplateRequest request, UUID createdByUserId) {
        if (templateRepository.existsByTitle(request.getTitle())) {
            throw new DuplicateResourceException("Template", "title", request.getTitle());
        }

        User createdBy = null;
        if (createdByUserId != null) {
            createdBy = userRepository.findById(createdByUserId).orElse(null);
        }

        InterviewTemplate template = InterviewTemplate.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .mode(request.getMode())
                .durationMinutes(request.getDurationMinutes())
                .evaluationCriteria(request.getEvaluationCriteria())
                .instructions(request.getInstructions())
                .tags(request.getTags())
                .createdBy(createdBy)
                .templateQuestions(new ArrayList<>())
                .build();

        InterviewTemplate saved = templateRepository.save(template);

        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            addQuestionsToTemplate(saved, request.getQuestions());
        }

        return toResponse(templateRepository.findByIdWithQuestions(saved.getId()).orElse(saved));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "templates", key = "#templateId")
    public TemplateResponse getTemplate(UUID templateId) {
        InterviewTemplate template = templateRepository.findByIdWithQuestions(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", templateId));
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplates() {
        return templateRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<TemplateResponse> getTemplatesPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<InterviewTemplate> templatePage = templateRepository.findByIsActiveTrue(pageable);

        List<TemplateResponse> content = templatePage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PaginatedResponse.<TemplateResponse>builder()
                .content(content)
                .page(templatePage.getNumber())
                .size(templatePage.getSize())
                .totalElements(templatePage.getTotalElements())
                .totalPages(templatePage.getTotalPages())
                .last(templatePage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> getTemplatesByType(InterviewType type) {
        return templateRepository.findByTypeAndIsActiveTrue(type).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> searchTemplates(String keyword) {
        return templateRepository.searchByKeyword(keyword).stream()
                .map(this::toResponse)
                .toList();
    }

    @CacheEvict(value = "templates", allEntries = true)
    public TemplateResponse updateTemplate(UUID templateId, UpdateTemplateRequest request) {
        InterviewTemplate template = templateRepository.findByIdWithQuestions(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", templateId));

        if (request.getTitle() != null) {
            if (!request.getTitle().equals(template.getTitle()) && templateRepository.existsByTitle(request.getTitle())) {
                throw new DuplicateResourceException("Template", "title", request.getTitle());
            }
            template.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getType() != null) template.setType(request.getType());
        if (request.getMode() != null) template.setMode(request.getMode());
        if (request.getDurationMinutes() != null) template.setDurationMinutes(request.getDurationMinutes());
        if (request.getEvaluationCriteria() != null) template.setEvaluationCriteria(request.getEvaluationCriteria());
        if (request.getInstructions() != null) template.setInstructions(request.getInstructions());
        if (request.getTags() != null) template.setTags(request.getTags());
        if (request.getIsActive() != null) template.setIsActive(request.getIsActive());

        if (request.getQuestions() != null) {
            template.getTemplateQuestions().clear();
            templateRepository.save(template);
            addQuestionsToTemplate(template, request.getQuestions());
        }

        InterviewTemplate saved = templateRepository.save(template);
        return toResponse(templateRepository.findByIdWithQuestions(saved.getId()).orElse(saved));
    }

    @CacheEvict(value = "templates", allEntries = true)
    public void deleteTemplate(UUID templateId) {
        InterviewTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", templateId));
        templateRepository.delete(template);
    }

    @CacheEvict(value = "templates", allEntries = true)
    public TemplateResponse addQuestionToTemplate(UUID templateId, TemplateQuestionRequest request) {
        InterviewTemplate template = templateRepository.findByIdWithQuestions(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", templateId));

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", request.getQuestionId()));

        boolean alreadyExists = template.getTemplateQuestions().stream()
                .anyMatch(tq -> tq.getQuestion().getId().equals(request.getQuestionId()));
        if (alreadyExists) {
            throw new DuplicateResourceException("Question already exists in this template");
        }

        TemplateQuestion templateQuestion = TemplateQuestion.builder()
                .template(template)
                .question(question)
                .orderIndex(request.getOrderIndex())
                .isMandatory(request.getIsMandatory() != null ? request.getIsMandatory() : true)
                .timeAllocationMinutes(request.getTimeAllocationMinutes())
                .notes(request.getNotes())
                .build();

        template.getTemplateQuestions().add(templateQuestion);
        templateRepository.save(template);

        return toResponse(templateRepository.findByIdWithQuestions(templateId).orElse(template));
    }

    @CacheEvict(value = "templates", allEntries = true)
    public TemplateResponse removeQuestionFromTemplate(UUID templateId, UUID templateQuestionId) {
        InterviewTemplate template = templateRepository.findByIdWithQuestions(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", templateId));

        TemplateQuestion tq = templateQuestionRepository.findById(templateQuestionId)
                .orElseThrow(() -> new ResourceNotFoundException("TemplateQuestion", "id", templateQuestionId));

        template.getTemplateQuestions().remove(tq);
        templateQuestionRepository.delete(tq);

        return toResponse(templateRepository.findByIdWithQuestions(templateId).orElse(template));
    }

    /**
     * Creates an interview from a template, using the template's type, mode, duration,
     * and description as defaults.
     */
    public InterviewResponse createInterviewFromTemplate(CreateInterviewFromTemplateRequest request, UUID scheduledByUserId) {
        InterviewTemplate template = templateRepository.findByIdWithQuestions(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", request.getTemplateId()));

        Instant endTime = request.getStartTime().plus(template.getDurationMinutes(), ChronoUnit.MINUTES);

        String description = template.getDescription() != null ? template.getDescription() : "";
        if (template.getInstructions() != null && !template.getInstructions().isBlank()) {
            description += "\n\n--- Instructions ---\n" + template.getInstructions();
        }
        if (template.getEvaluationCriteria() != null && !template.getEvaluationCriteria().isBlank()) {
            description += "\n\n--- Evaluation Criteria ---\n" + template.getEvaluationCriteria();
        }

        CreateInterviewRequest interviewRequest = CreateInterviewRequest.builder()
                .title(template.getTitle())
                .description(description)
                .candidateId(request.getCandidateId())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .timeZone(request.getTimeZone())
                .type(template.getType())
                .mode(template.getMode())
                .meetingLink(request.getMeetingLink())
                .location(request.getLocation())
                .interviewerIds(request.getInterviewerIds())
                .build();

        return interviewService.createInterview(interviewRequest, scheduledByUserId);
    }

    // ---- Helper methods ----

    private void addQuestionsToTemplate(InterviewTemplate template, List<TemplateQuestionRequest> questions) {
        for (TemplateQuestionRequest qr : questions) {
            Question question = questionRepository.findById(qr.getQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question", "id", qr.getQuestionId()));

            TemplateQuestion tq = TemplateQuestion.builder()
                    .template(template)
                    .question(question)
                    .orderIndex(qr.getOrderIndex())
                    .isMandatory(qr.getIsMandatory() != null ? qr.getIsMandatory() : true)
                    .timeAllocationMinutes(qr.getTimeAllocationMinutes())
                    .notes(qr.getNotes())
                    .build();

            template.getTemplateQuestions().add(tq);
        }
        templateRepository.save(template);
    }

    private TemplateResponse toResponse(InterviewTemplate template) {
        List<TemplateQuestionResponse> questions = List.of();
        if (template.getTemplateQuestions() != null) {
            questions = template.getTemplateQuestions().stream()
                    .map(this::toQuestionResponse)
                    .toList();
        }

        return TemplateResponse.builder()
                .id(template.getId())
                .title(template.getTitle())
                .description(template.getDescription())
                .type(template.getType())
                .mode(template.getMode())
                .durationMinutes(template.getDurationMinutes())
                .evaluationCriteria(template.getEvaluationCriteria())
                .instructions(template.getInstructions())
                .tags(template.getTags())
                .isActive(template.getIsActive())
                .createdById(template.getCreatedBy() != null ? template.getCreatedBy().getId() : null)
                .createdByName(template.getCreatedBy() != null
                        ? template.getCreatedBy().getFirstName() + " " + template.getCreatedBy().getLastName()
                        : null)
                .questions(questions)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private TemplateQuestionResponse toQuestionResponse(TemplateQuestion tq) {
        Question q = tq.getQuestion();
        return TemplateQuestionResponse.builder()
                .id(tq.getId())
                .questionId(q.getId())
                .questionTitle(q.getTitle())
                .questionDescription(q.getDescription())
                .difficulty(q.getDifficulty())
                .questionType(q.getType())
                .categoryName(q.getCategory() != null ? q.getCategory().getName() : null)
                .orderIndex(tq.getOrderIndex())
                .isMandatory(tq.getIsMandatory())
                .timeAllocationMinutes(tq.getTimeAllocationMinutes())
                .notes(tq.getNotes())
                .build();
    }
}

