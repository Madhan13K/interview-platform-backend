package com.interview_platform_backend.interview_platform_backend.jobboard.service;

import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.jobboard.dto.*;
import com.interview_platform_backend.interview_platform_backend.jobboard.entity.ApplicationSource;
import com.interview_platform_backend.interview_platform_backend.jobboard.entity.ApplicationStatus;
import com.interview_platform_backend.interview_platform_backend.jobboard.entity.JobApplication;
import com.interview_platform_backend.interview_platform_backend.jobboard.repository.JobApplicationRepository;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.EmploymentType;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.ExperienceLevel;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPosition;
import com.interview_platform_backend.interview_platform_backend.jobposition.entity.JobPositionStatus;
import com.interview_platform_backend.interview_platform_backend.jobposition.repository.JobPositionRepository;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class JobBoardService {

    private final JobPositionRepository jobPositionRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;

    public JobBoardService(JobPositionRepository jobPositionRepository,
                           JobApplicationRepository jobApplicationRepository,
                           UserRepository userRepository) {
        this.jobPositionRepository = jobPositionRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
    }

    // ==================== Public Job Listings ====================

    @Transactional(readOnly = true)
    public PaginatedResponse<JobListingResponse> getPublicJobListings(int page, int size) {
        Page<JobPosition> pageResult = jobPositionRepository.findByStatus(
                JobPositionStatus.OPEN,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "postedAt")));

        List<JobListingResponse> content = pageResult.getContent().stream()
                .map(this::mapToJobListing)
                .toList();

        return PaginatedResponse.<JobListingResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public JobListingResponse getJobListingDetail(UUID id) {
        JobPosition jp = jobPositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", id));

        if (jp.getStatus() != JobPositionStatus.OPEN) {
            throw new ResourceNotFoundException("JobPosition", "id", id);
        }

        return mapToJobListing(jp);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<JobListingResponse> searchPublicJobs(String keyword,
                                                                   String department,
                                                                   String location,
                                                                   EmploymentType type,
                                                                   ExperienceLevel level,
                                                                   int page,
                                                                   int size) {
        Page<JobPosition> pageResult;

        if (keyword != null && !keyword.isBlank()) {
            pageResult = jobPositionRepository.searchByKeyword(keyword, PageRequest.of(page, size));
        } else {
            pageResult = jobPositionRepository.findByStatus(
                    JobPositionStatus.OPEN,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "postedAt")));
        }

        List<JobListingResponse> content = pageResult.getContent().stream()
                .filter(jp -> jp.getStatus() == JobPositionStatus.OPEN)
                .filter(jp -> department == null || department.isBlank()
                        || (jp.getDepartment() != null && jp.getDepartment().equalsIgnoreCase(department)))
                .filter(jp -> location == null || location.isBlank()
                        || (jp.getLocation() != null && jp.getLocation().toLowerCase().contains(location.toLowerCase())))
                .filter(jp -> type == null || jp.getEmploymentType() == type)
                .filter(jp -> level == null || jp.getExperienceLevel() == level)
                .map(this::mapToJobListing)
                .toList();

        return PaginatedResponse.<JobListingResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements((long) content.size())
                .totalPages(1)
                .last(true)
                .build();
    }

    // ==================== Applications ====================

    public JobApplicationResponse submitApplication(JobApplicationRequest request, String userEmail) {
        User candidate = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        JobPosition jobPosition = jobPositionRepository.findById(request.getJobPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", request.getJobPositionId()));

        if (jobPosition.getStatus() != JobPositionStatus.OPEN) {
            throw new IllegalStateException("This job position is not open for applications");
        }

        if (jobApplicationRepository.existsByJobPositionIdAndCandidateId(jobPosition.getId(), candidate.getId())) {
            throw new IllegalStateException("You have already applied for this position");
        }

        JobApplication application = JobApplication.builder()
                .jobPosition(jobPosition)
                .candidate(candidate)
                .status(ApplicationStatus.SUBMITTED)
                .coverLetter(request.getCoverLetter())
                .resumeUrl(request.getResumeUrl())
                .source(request.getSource() != null ? request.getSource() : ApplicationSource.PORTAL)
                .referralCode(request.getReferralCode())
                .build();

        JobApplication saved = jobApplicationRepository.save(application);
        return mapToApplicationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getMyApplications(String userEmail) {
        User candidate = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return jobApplicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidate.getId()).stream()
                .map(this::mapToApplicationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse getApplicationDetail(UUID applicationId, String userEmail) {
        User candidate = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", applicationId));

        if (!application.getCandidate().getId().equals(candidate.getId())) {
            throw new IllegalStateException("You do not have access to this application");
        }

        return mapToApplicationResponse(application);
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getApplicationsForPosition(UUID positionId) {
        jobPositionRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosition", "id", positionId));

        return jobApplicationRepository.findByJobPositionId(positionId).stream()
                .map(this::mapToApplicationResponse)
                .toList();
    }

    public JobApplicationResponse updateApplicationStatus(UUID applicationId,
                                                          ApplicationStatusUpdate statusUpdate,
                                                          String reviewerEmail) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", applicationId));

        application.setStatus(statusUpdate.getStatus());
        application.setStatusUpdatedAt(Instant.now());
        application.setReviewedAt(Instant.now());

        if (statusUpdate.getNotes() != null) {
            application.setNotes(statusUpdate.getNotes());
        }

        JobApplication saved = jobApplicationRepository.save(application);
        return mapToApplicationResponse(saved);
    }

    public JobApplicationResponse withdrawApplication(UUID applicationId, String userEmail) {
        User candidate = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", applicationId));

        if (!application.getCandidate().getId().equals(candidate.getId())) {
            throw new IllegalStateException("You do not have access to this application");
        }

        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new IllegalStateException("Application is already withdrawn");
        }

        if (application.getStatus() == ApplicationStatus.HIRED || application.getStatus() == ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Cannot withdraw an application that has been " + application.getStatus().name().toLowerCase());
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setStatusUpdatedAt(Instant.now());

        JobApplication saved = jobApplicationRepository.save(application);
        return mapToApplicationResponse(saved);
    }

    // ==================== Mappers ====================

    private JobListingResponse mapToJobListing(JobPosition jp) {
        return JobListingResponse.builder()
                .id(jp.getId())
                .title(jp.getTitle())
                .department(jp.getDepartment())
                .location(jp.getLocation())
                .employmentType(jp.getEmploymentType())
                .experienceLevel(jp.getExperienceLevel())
                .description(jp.getDescription())
                .requirements(jp.getRequirements())
                .skills(jp.getSkills())
                .postedAt(jp.getPostedAt())
                .deadline(jp.getDeadline())
                .numberOfOpenings(jp.getNumberOfOpenings())
                .build();
    }

    private JobApplicationResponse mapToApplicationResponse(JobApplication app) {
        return JobApplicationResponse.builder()
                .id(app.getId())
                .jobPositionId(app.getJobPosition().getId())
                .jobTitle(app.getJobPosition().getTitle())
                .department(app.getJobPosition().getDepartment())
                .location(app.getJobPosition().getLocation())
                .candidateId(app.getCandidate().getId())
                .candidateName(app.getCandidate().getFirstName() + " " + app.getCandidate().getLastName())
                .candidateEmail(app.getCandidate().getEmail())
                .status(app.getStatus())
                .coverLetter(app.getCoverLetter())
                .resumeUrl(app.getResumeUrl())
                .source(app.getSource())
                .referralCode(app.getReferralCode())
                .notes(app.getNotes())
                .appliedAt(app.getAppliedAt())
                .reviewedAt(app.getReviewedAt())
                .statusUpdatedAt(app.getStatusUpdatedAt())
                .build();
    }
}
