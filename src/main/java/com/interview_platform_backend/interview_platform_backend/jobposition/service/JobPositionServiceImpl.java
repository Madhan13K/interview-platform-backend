package com.interview_platform_backend.interview_platform_backend.jobposition.service;

import com.interview_platform_backend.interview_platform_backend.audit.AuditAction;
import com.interview_platform_backend.interview_platform_backend.audit.AuditService;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.jobposition.dto.*;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPosition;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPositionStatus;
import com.interview_platform_backend.interview_platform_backend.jobposition.repository.JobPositionRepository;
import com.interview_platform_backend.interview_platform_backend.pipeline.entity.InterviewPipeline;
import com.interview_platform_backend.interview_platform_backend.pipeline.repository.InterviewPipelineRepository;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobPositionServiceImpl implements JobPositionService {

    private final JobPositionRepository jobPositionRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final InterviewPipelineRepository pipelineRepository;
    private final AuditService auditService;

    public JobPositionServiceImpl(JobPositionRepository jobPositionRepository,
                                   InterviewRepository interviewRepository,
                                   UserRepository userRepository,
                                   InterviewPipelineRepository pipelineRepository,
                                   AuditService auditService) {
        this.jobPositionRepository = jobPositionRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
        this.pipelineRepository = pipelineRepository;
        this.auditService = auditService;
    }

    @Override
    @CacheEvict(value = "jobPositions", allEntries = true)
    public JobPositionResponse createJobPosition(CreateJobPositionRequest request, UUID createdBy) {
        User creator = userRepository.findById(createdBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", createdBy));

        JobPosition jobPosition = JobPosition.builder()
                .title(request.getTitle())
                .department(request.getDepartment())
                .location(request.getLocation())
                .employmentType(request.getEmploymentType())
                .experienceLevel(request.getExperienceLevel())
                .status(JobPositionStatus.OPEN)
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .responsibilities(request.getResponsibilities())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryCurrency(request.getSalaryCurrency() != null ? request.getSalaryCurrency() : "USD")
                .numberOfOpenings(request.getNumberOfOpenings() != null ? request.getNumberOfOpenings() : 1)
                .numberHired(0)
                .createdBy(creator)
                .skills(request.getSkills())
                .deadline(request.getDeadline())
                .postedAt(Instant.now())
                .build();

        if (request.getHiringManagerId() != null) {
            User hiringManager = userRepository.findById(request.getHiringManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getHiringManagerId()));
            jobPosition.setHiringManager(hiringManager);
        }

        if (request.getPipelineId() != null) {
            InterviewPipeline pipeline = pipelineRepository.findById(request.getPipelineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pipeline", "id", request.getPipelineId()));
            jobPosition.setPipeline(pipeline);
        }

        JobPosition saved = jobPositionRepository.save(jobPosition);
        auditService.log("JobPosition", saved.getId(), AuditAction.CREATE, "Job position created: " + saved.getTitle());

        return mapToResponse(saved);
    }

    @Override
    @Cacheable(value = "jobPositions", key = "#id")
    @Transactional(readOnly = true)
    public JobPositionResponse getJobPosition(UUID id) {
        JobPosition jp = jobPositionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", id));
        return mapToResponse(jp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPositionResponse> getAllJobPositions() {
        return jobPositionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<JobPositionResponse> getJobPositionsPaginated(int page, int size) {
        Page<JobPosition> pageResult = jobPositionRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return buildPaginatedResponse(pageResult, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPositionResponse> getJobPositionsByStatus(JobPositionStatus status) {
        return jobPositionRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<JobPositionResponse> searchJobPositions(String keyword, int page, int size) {
        Page<JobPosition> pageResult = jobPositionRepository.searchByKeyword(keyword, PageRequest.of(page, size));
        return buildPaginatedResponse(pageResult, page, size);
    }

    @Override
    @CacheEvict(value = "jobPositions", allEntries = true)
    public JobPositionResponse updateJobPosition(UUID id, UpdateJobPositionRequest request) {
        JobPosition jp = jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", id));

        if (request.getTitle() != null) jp.setTitle(request.getTitle());
        if (request.getDepartment() != null) jp.setDepartment(request.getDepartment());
        if (request.getLocation() != null) jp.setLocation(request.getLocation());
        if (request.getEmploymentType() != null) jp.setEmploymentType(request.getEmploymentType());
        if (request.getExperienceLevel() != null) jp.setExperienceLevel(request.getExperienceLevel());
        if (request.getStatus() != null) jp.setStatus(request.getStatus());
        if (request.getDescription() != null) jp.setDescription(request.getDescription());
        if (request.getRequirements() != null) jp.setRequirements(request.getRequirements());
        if (request.getResponsibilities() != null) jp.setResponsibilities(request.getResponsibilities());
        if (request.getSalaryMin() != null) jp.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null) jp.setSalaryMax(request.getSalaryMax());
        if (request.getSalaryCurrency() != null) jp.setSalaryCurrency(request.getSalaryCurrency());
        if (request.getNumberOfOpenings() != null) jp.setNumberOfOpenings(request.getNumberOfOpenings());
        if (request.getSkills() != null) jp.setSkills(request.getSkills());
        if (request.getDeadline() != null) jp.setDeadline(request.getDeadline());

        if (request.getHiringManagerId() != null) {
            User hiringManager = userRepository.findById(request.getHiringManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getHiringManagerId()));
            jp.setHiringManager(hiringManager);
        }

        if (request.getPipelineId() != null) {
            InterviewPipeline pipeline = pipelineRepository.findById(request.getPipelineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pipeline", "id", request.getPipelineId()));
            jp.setPipeline(pipeline);
        }

        JobPosition saved = jobPositionRepository.save(jp);
        auditService.log("JobPosition", saved.getId(), AuditAction.UPDATE, "Job position updated");
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = "jobPositions", allEntries = true)
    public JobPositionResponse updateStatus(UUID id, JobPositionStatus status) {
        JobPosition jp = jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", id));
        jp.setStatus(status);
        if (status == JobPositionStatus.CLOSED || status == JobPositionStatus.FILLED) {
            jp.setClosedAt(Instant.now());
        }
        JobPosition saved = jobPositionRepository.save(jp);
        auditService.log("JobPosition", saved.getId(), AuditAction.STATUS_CHANGE, "Status changed to: " + status);
        return mapToResponse(saved);
    }

    @Override
    @CacheEvict(value = "jobPositions", allEntries = true)
    public void deleteJobPosition(UUID id) {
        JobPosition jp = jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", id));
        jobPositionRepository.delete(jp);
        auditService.log("JobPosition", id, AuditAction.DELETE, "Job position deleted: " + jp.getTitle());
    }

    @Override
    public JobPositionResponse linkInterviewToPosition(UUID positionId, UUID interviewId) {
        JobPosition jp = jobPositionRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", positionId));
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        interview.setJobPosition(jp);
        interviewRepository.save(interview);
        return mapToResponse(jp);
    }

    @Override
    public JobPositionResponse unlinkInterviewFromPosition(UUID interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));
        interview.setJobPosition(null);
        interviewRepository.save(interview);
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPositionResponse> getMyJobPositions(UUID userId) {
        return jobPositionRepository.findByCreatedById(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private JobPositionResponse mapToResponse(JobPosition jp) {
        long totalInterviews = jp.getInterviews() != null ? jp.getInterviews().size() : 0;
        long totalCandidates = jp.getInterviews() != null
                ? jp.getInterviews().stream()
                    .map(i -> i.getCandidate().getId())
                    .collect(Collectors.toSet()).size()
                : 0;

        return JobPositionResponse.builder()
                .id(jp.getId())
                .title(jp.getTitle())
                .department(jp.getDepartment())
                .location(jp.getLocation())
                .employmentType(jp.getEmploymentType())
                .experienceLevel(jp.getExperienceLevel())
                .status(jp.getStatus())
                .description(jp.getDescription())
                .requirements(jp.getRequirements())
                .responsibilities(jp.getResponsibilities())
                .salaryMin(jp.getSalaryMin())
                .salaryMax(jp.getSalaryMax())
                .salaryCurrency(jp.getSalaryCurrency())
                .numberOfOpenings(jp.getNumberOfOpenings())
                .numberHired(jp.getNumberHired())
                .pipelineId(jp.getPipeline() != null ? jp.getPipeline().getId() : null)
                .pipelineName(jp.getPipeline() != null ? jp.getPipeline().getName() : null)
                .createdById(jp.getCreatedBy().getId())
                .createdByName(jp.getCreatedBy().getFirstName() + " " + jp.getCreatedBy().getLastName())
                .hiringManagerId(jp.getHiringManager() != null ? jp.getHiringManager().getId() : null)
                .hiringManagerName(jp.getHiringManager() != null
                        ? jp.getHiringManager().getFirstName() + " " + jp.getHiringManager().getLastName() : null)
                .skills(jp.getSkills())
                .postedAt(jp.getPostedAt())
                .closedAt(jp.getClosedAt())
                .deadline(jp.getDeadline())
                .createdAt(jp.getCreatedAt())
                .updatedAt(jp.getUpdatedAt())
                .totalInterviews(totalInterviews)
                .totalCandidates(totalCandidates)
                .build();
    }

    private PaginatedResponse<JobPositionResponse> buildPaginatedResponse(Page<JobPosition> page, int pageNum, int size) {
        List<JobPositionResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PaginatedResponse.<JobPositionResponse>builder()
                .content(content)
                .page(pageNum)
                .size(size)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}



