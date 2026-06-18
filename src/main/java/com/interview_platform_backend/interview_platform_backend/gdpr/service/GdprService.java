package com.interview_platform_backend.interview_platform_backend.gdpr.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewFeedBack;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewFeedbackRepository;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.document.entity.Document;
import com.interview_platform_backend.interview_platform_backend.document.repository.DocumentRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.gdpr.dto.*;
import com.interview_platform_backend.interview_platform_backend.gdpr.entity.DataErasureRequest;
import com.interview_platform_backend.interview_platform_backend.gdpr.entity.UserConsent;
import com.interview_platform_backend.interview_platform_backend.gdpr.repository.DataErasureRequestRepository;
import com.interview_platform_backend.interview_platform_backend.gdpr.repository.UserConsentRepository;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GdprService {

    private final UserConsentRepository userConsentRepository;
    private final DataErasureRequestRepository dataErasureRequestRepository;
    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewFeedbackRepository interviewFeedbackRepository;
    private final DocumentRepository documentRepository;

    public GdprService(UserConsentRepository userConsentRepository,
                       DataErasureRequestRepository dataErasureRequestRepository,
                       UserRepository userRepository,
                       InterviewRepository interviewRepository,
                       InterviewFeedbackRepository interviewFeedbackRepository,
                       DocumentRepository documentRepository) {
        this.userConsentRepository = userConsentRepository;
        this.dataErasureRequestRepository = dataErasureRequestRepository;
        this.userRepository = userRepository;
        this.interviewRepository = interviewRepository;
        this.interviewFeedbackRepository = interviewFeedbackRepository;
        this.documentRepository = documentRepository;
    }

    public ConsentResponse recordConsent(UUID userId, ConsentRequest request, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserConsent consent = userConsentRepository.findByUserIdAndConsentType(userId, request.getConsentType())
                .orElse(UserConsent.builder()
                        .user(user)
                        .consentType(request.getConsentType())
                        .build());

        consent.setGranted(request.getGranted());
        consent.setIpAddress(ipAddress);
        consent.setGrantedAt(Instant.now());

        if (!request.getGranted()) {
            consent.setRevokedAt(Instant.now());
        } else {
            consent.setRevokedAt(null);
        }

        UserConsent saved = userConsentRepository.save(consent);
        return mapToConsentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConsentResponse> getConsents(UUID userId) {
        return userConsentRepository.findByUserId(userId).stream()
                .map(this::mapToConsentResponse)
                .collect(Collectors.toList());
    }

    public ConsentResponse revokeConsent(UUID userId, String consentType) {
        UserConsent consent = userConsentRepository.findByUserIdAndConsentType(userId, consentType)
                .orElseThrow(() -> new ResourceNotFoundException("Consent", "type", consentType));

        consent.setGranted(false);
        consent.setRevokedAt(Instant.now());

        UserConsent saved = userConsentRepository.save(consent);
        return mapToConsentResponse(saved);
    }

    @Transactional(readOnly = true)
    public DataExportResponse exportUserData(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Compile user profile data
        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("id", user.getId().toString());
        userData.put("firstName", user.getFirstName());
        userData.put("lastName", user.getLastName());
        userData.put("email", user.getEmail());
        userData.put("phoneNumber", user.getPhoneNumber());
        userData.put("status", user.getStatus() != null ? user.getStatus().name() : null);
        userData.put("createdAt", user.getCreatedAt());
        userData.put("lastLoginAt", user.getLastLoginAt());

        // Compile interviews
        List<Interview> interviews = interviewRepository.findByCandidateId(userId);
        List<Map<String, Object>> interviewData = interviews.stream()
                .map(interview -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", interview.getId().toString());
                    map.put("title", interview.getTitle());
                    map.put("description", interview.getDescription());
                    map.put("startTime", interview.getStartTime());
                    map.put("endTime", interview.getEndTime());
                    map.put("status", interview.getStatus().name());
                    map.put("type", interview.getType().name());
                    map.put("mode", interview.getMode().name());
                    map.put("createdAt", interview.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());

        // Compile feedback
        List<InterviewFeedBack> feedbackList = interviewFeedbackRepository.findByInterviewer(user);
        List<Map<String, Object>> feedbackData = feedbackList.stream()
                .map(fb -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", fb.getId().toString());
                    map.put("interviewId", fb.getInterview().getId().toString());
                    map.put("rating", fb.getRating());
                    map.put("comments", fb.getComments());
                    map.put("submittedAt", fb.getSubmittedAt());
                    return map;
                })
                .collect(Collectors.toList());

        // Compile documents metadata
        List<Document> documents = documentRepository.findByUploadedById(userId);
        List<Map<String, Object>> documentData = documents.stream()
                .map(doc -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", doc.getId().toString());
                    map.put("fileName", doc.getOriginalFileName());
                    map.put("contentType", doc.getContentType());
                    map.put("fileSize", doc.getFileSize());
                    map.put("documentType", doc.getDocumentType().name());
                    map.put("createdAt", doc.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());

        return DataExportResponse.builder()
                .userData(userData)
                .interviews(interviewData)
                .feedback(feedbackData)
                .documents(documentData)
                .exportedAt(Instant.now())
                .build();
    }

    public ErasureRequestResponse requestErasure(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Check if there's already a pending request
        List<DataErasureRequest> existingRequests = dataErasureRequestRepository.findByUserId(userId);
        boolean hasPending = existingRequests.stream()
                .anyMatch(r -> r.getStatus() == DataErasureRequest.ErasureStatus.PENDING
                        || r.getStatus() == DataErasureRequest.ErasureStatus.PROCESSING);
        if (hasPending) {
            throw new BadRequestException("A data erasure request is already pending for this user");
        }

        DataErasureRequest request = DataErasureRequest.builder()
                .user(user)
                .status(DataErasureRequest.ErasureStatus.PENDING)
                .requestedAt(Instant.now())
                .build();

        DataErasureRequest saved = dataErasureRequestRepository.save(request);
        return mapToErasureResponse(saved);
    }

    public ErasureRequestResponse processErasure(UUID requestId, UUID adminUserId) {
        DataErasureRequest request = dataErasureRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("DataErasureRequest", "id", requestId));

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        if (request.getStatus() != DataErasureRequest.ErasureStatus.PENDING) {
            throw new BadRequestException("Erasure request is not in PENDING status");
        }

        // Anonymize user data
        User user = request.getUser();
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setEmail(UUID.randomUUID().toString() + "@deleted.invalid");
        user.setPhoneNumber(null);
        user.setPassword("DELETED");

        if (user.getProfile() != null) {
            user.getProfile().setBio(null);
            user.getProfile().setProfilePictureUrl(null);
            user.getProfile().setLinkedinUrl(null);
            user.getProfile().setGithubUrl(null);
            user.getProfile().setContactNumber(null);
        }

        userRepository.save(user);

        // Mark request as completed
        request.setStatus(DataErasureRequest.ErasureStatus.COMPLETED);
        request.setCompletedAt(Instant.now());
        request.setProcessedBy(admin);

        DataErasureRequest saved = dataErasureRequestRepository.save(request);
        return mapToErasureResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ErasureRequestResponse> getErasureRequests(DataErasureRequest.ErasureStatus status) {
        List<DataErasureRequest> requests;
        if (status != null) {
            requests = dataErasureRequestRepository.findByStatus(status);
        } else {
            requests = dataErasureRequestRepository.findAll();
        }
        return requests.stream()
                .map(this::mapToErasureResponse)
                .collect(Collectors.toList());
    }

    private ConsentResponse mapToConsentResponse(UserConsent consent) {
        return ConsentResponse.builder()
                .id(consent.getId())
                .consentType(consent.getConsentType())
                .granted(consent.getGranted())
                .grantedAt(consent.getGrantedAt())
                .revokedAt(consent.getRevokedAt())
                .build();
    }

    private ErasureRequestResponse mapToErasureResponse(DataErasureRequest request) {
        return ErasureRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUser().getId())
                .status(request.getStatus().name())
                .requestedAt(request.getRequestedAt())
                .completedAt(request.getCompletedAt())
                .build();
    }
}
