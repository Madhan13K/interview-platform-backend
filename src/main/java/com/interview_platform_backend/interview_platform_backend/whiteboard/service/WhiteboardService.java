package com.interview_platform_backend.interview_platform_backend.whiteboard.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.whiteboard.dto.*;
import com.interview_platform_backend.interview_platform_backend.whiteboard.entity.WhiteboardSession;
import com.interview_platform_backend.interview_platform_backend.whiteboard.entity.WhiteboardStroke;
import com.interview_platform_backend.interview_platform_backend.whiteboard.repository.WhiteboardSessionRepository;
import com.interview_platform_backend.interview_platform_backend.whiteboard.repository.WhiteboardStrokeRepository;
import com.interview_platform_backend.interview_platform_backend.whiteboard.websocket.WhiteboardWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WhiteboardService {

    private final WhiteboardSessionRepository sessionRepository;
    private final WhiteboardStrokeRepository strokeRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final WhiteboardWebSocketHandler webSocketHandler;

    public WhiteboardService(WhiteboardSessionRepository sessionRepository,
                             WhiteboardStrokeRepository strokeRepository,
                             InterviewRepository interviewRepository,
                             UserRepository userRepository,
                             WhiteboardWebSocketHandler webSocketHandler) {
        this.sessionRepository = sessionRepository;
        this.strokeRepository = strokeRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
        this.webSocketHandler = webSocketHandler;
    }

    public WhiteboardSessionResponse createSession(CreateWhiteboardRequest request, UUID userId) {
        Interview interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", request.getInterviewId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        WhiteboardSession session = WhiteboardSession.builder()
                .interview(interview)
                .createdBy(user)
                .title(request.getTitle() != null ? request.getTitle() : "Whiteboard Session")
                .isActive(true)
                .build();

        WhiteboardSession saved = sessionRepository.save(session);
        return toSessionResponse(saved, 0L);
    }

    @Transactional(readOnly = true)
    public WhiteboardSessionResponse getSession(UUID sessionId) {
        WhiteboardSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("WhiteboardSession", "id", sessionId));

        long strokeCount = strokeRepository.countBySessionId(sessionId);
        return toSessionResponse(session, strokeCount);
    }

    @Transactional(readOnly = true)
    public List<WhiteboardSessionResponse> getSessionsByInterview(UUID interviewId) {
        List<WhiteboardSession> sessions = sessionRepository.findByInterviewId(interviewId);
        return sessions.stream()
                .map(session -> {
                    long strokeCount = strokeRepository.countBySessionId(session.getId());
                    return toSessionResponse(session, strokeCount);
                })
                .toList();
    }

    public WhiteboardStrokeResponse addStroke(UUID sessionId, AddStrokeRequest request, UUID userId) {
        WhiteboardSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("WhiteboardSession", "id", sessionId));

        if (!Boolean.TRUE.equals(session.getIsActive())) {
            throw new BadRequestException("Cannot add stroke to a closed whiteboard session");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        long currentCount = strokeRepository.countBySessionId(sessionId);
        int nextSequence = (int) (currentCount + 1);

        WhiteboardStroke.StrokeTool tool = WhiteboardStroke.StrokeTool.PEN;
        if (request.getTool() != null) {
            try {
                tool = WhiteboardStroke.StrokeTool.valueOf(request.getTool().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid tool: " + request.getTool());
            }
        }

        WhiteboardStroke stroke = WhiteboardStroke.builder()
                .session(session)
                .user(user)
                .strokeData(request.getStrokeData())
                .tool(tool)
                .color(request.getColor())
                .strokeWidth(request.getStrokeWidth())
                .sequenceNumber(nextSequence)
                .build();

        WhiteboardStroke saved = strokeRepository.save(stroke);
        WhiteboardStrokeResponse response = toStrokeResponse(saved);

        webSocketHandler.broadcastStroke(sessionId, response);

        return response;
    }

    @Transactional(readOnly = true)
    public List<WhiteboardStrokeResponse> getStrokes(UUID sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("WhiteboardSession", "id", sessionId);
        }

        return strokeRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId).stream()
                .map(this::toStrokeResponse)
                .toList();
    }

    public WhiteboardSessionResponse saveSnapshot(UUID sessionId, String snapshotData) {
        WhiteboardSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("WhiteboardSession", "id", sessionId));

        session.setSnapshotData(snapshotData);
        WhiteboardSession saved = sessionRepository.save(session);
        long strokeCount = strokeRepository.countBySessionId(sessionId);
        return toSessionResponse(saved, strokeCount);
    }

    public WhiteboardSessionResponse closeSession(UUID sessionId) {
        WhiteboardSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("WhiteboardSession", "id", sessionId));

        session.setIsActive(false);
        WhiteboardSession saved = sessionRepository.save(session);
        long strokeCount = strokeRepository.countBySessionId(sessionId);
        return toSessionResponse(saved, strokeCount);
    }

    public void deleteSession(UUID sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("WhiteboardSession", "id", sessionId);
        }

        strokeRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    private WhiteboardSessionResponse toSessionResponse(WhiteboardSession session, Long strokeCount) {
        User createdBy = session.getCreatedBy();
        return WhiteboardSessionResponse.builder()
                .id(session.getId())
                .interviewId(session.getInterview() != null ? session.getInterview().getId() : null)
                .createdById(createdBy != null ? createdBy.getId() : null)
                .createdByName(createdBy != null ? createdBy.getFirstName() + " " + createdBy.getLastName() : null)
                .title(session.getTitle())
                .snapshotData(session.getSnapshotData())
                .thumbnailUrl(session.getThumbnailUrl())
                .isActive(session.getIsActive())
                .strokeCount(strokeCount)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private WhiteboardStrokeResponse toStrokeResponse(WhiteboardStroke stroke) {
        User user = stroke.getUser();
        return WhiteboardStrokeResponse.builder()
                .id(stroke.getId())
                .sessionId(stroke.getSession().getId())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? user.getFirstName() + " " + user.getLastName() : null)
                .strokeData(stroke.getStrokeData())
                .tool(stroke.getTool().name())
                .color(stroke.getColor())
                .strokeWidth(stroke.getStrokeWidth())
                .sequenceNumber(stroke.getSequenceNumber())
                .createdAt(stroke.getCreatedAt())
                .build();
    }
}
