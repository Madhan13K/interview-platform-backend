package com.interview_platform_backend.interview_platform_backend.codeeditor.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.codeeditor.dto.CodingSessionResponse;
import com.interview_platform_backend.interview_platform_backend.codeeditor.entity.CodingSession;
import com.interview_platform_backend.interview_platform_backend.codeeditor.repository.CodingSessionRepository;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CodingSessionService {

    private final CodingSessionRepository codingSessionRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public CodingSessionService(CodingSessionRepository codingSessionRepository,
                                InterviewRepository interviewRepository,
                                UserRepository userRepository) {
        this.codingSessionRepository = codingSessionRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
    }

    public CodingSessionResponse startSession(UUID interviewId, String language) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        // Return existing active session if one exists
        return codingSessionRepository.findByInterviewIdAndEndedAtIsNull(interviewId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    CodingSession session = CodingSession.builder()
                            .interview(interview)
                            .language(language != null ? language : "java")
                            .codeContent("")
                            .build();
                    return toResponse(codingSessionRepository.save(session));
                });
    }

    public CodingSessionResponse getActiveSession(UUID interviewId) {
        CodingSession session = codingSessionRepository.findByInterviewIdAndEndedAtIsNull(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("CodingSession", "interviewId", interviewId));
        return toResponse(session);
    }

    public CodingSessionResponse saveCode(UUID interviewId, String code, String language, UUID userId) {
        CodingSession session = codingSessionRepository.findByInterviewIdAndEndedAtIsNull(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("CodingSession", "interviewId", interviewId));

        session.setCodeContent(code);
        if (language != null) session.setLanguage(language);
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            session.setLastEditedBy(user);
        }

        return toResponse(codingSessionRepository.save(session));
    }

    public void endSession(UUID interviewId) {
        codingSessionRepository.findByInterviewIdAndEndedAtIsNull(interviewId)
                .ifPresent(session -> {
                    session.setEndedAt(Instant.now());
                    codingSessionRepository.save(session);
                });
    }

    @Transactional(readOnly = true)
    public List<CodingSessionResponse> getSessionHistory(UUID interviewId) {
        return codingSessionRepository.findByInterviewId(interviewId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CodingSessionResponse toResponse(CodingSession session) {
        return CodingSessionResponse.builder()
                .id(session.getId())
                .interviewId(session.getInterview().getId())
                .language(session.getLanguage())
                .codeContent(session.getCodeContent())
                .lastEditedBy(session.getLastEditedBy() != null ? session.getLastEditedBy().getId() : null)
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .build();
    }
}

