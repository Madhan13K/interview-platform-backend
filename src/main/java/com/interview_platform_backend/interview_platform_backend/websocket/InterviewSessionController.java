package com.interview_platform_backend.interview_platform_backend.websocket;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

/**
 * WebSocket controller for handling real-time interview session messages.
 * <p>
 * Endpoints:
 * - /app/interview/{interviewId}/join      → Notify participants someone joined
 * - /app/interview/{interviewId}/leave     → Notify participants someone left
 * - /app/interview/{interviewId}/chat      → Broadcast chat message
 * - /app/interview/{interviewId}/code      → Broadcast code editor updates
 * - /app/interview/{interviewId}/signal    → WebRTC signaling (peer-to-peer)
 */
@Controller
public class InterviewSessionController {

    private final SimpMessagingTemplate messagingTemplate;

    public InterviewSessionController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Participant joins an interview session.
     */
    @MessageMapping("/interview/{interviewId}/join")
    @SendTo("/topic/interview/{interviewId}")
    public InterviewSessionMessage joinSession(
            @DestinationVariable UUID interviewId,
            @Payload InterviewSessionMessage message) {
        message.setType(InterviewSessionMessage.MessageType.JOIN);
        message.setInterviewId(interviewId);
        message.setTimestamp(Instant.now());
        return message;
    }

    /**
     * Participant leaves an interview session.
     */
    @MessageMapping("/interview/{interviewId}/leave")
    @SendTo("/topic/interview/{interviewId}")
    public InterviewSessionMessage leaveSession(
            @DestinationVariable UUID interviewId,
            @Payload InterviewSessionMessage message) {
        message.setType(InterviewSessionMessage.MessageType.LEAVE);
        message.setInterviewId(interviewId);
        message.setTimestamp(Instant.now());
        return message;
    }

    /**
     * Chat message within an interview session.
     */
    @MessageMapping("/interview/{interviewId}/chat")
    @SendTo("/topic/interview/{interviewId}")
    public InterviewSessionMessage chatMessage(
            @DestinationVariable UUID interviewId,
            @Payload InterviewSessionMessage message) {
        message.setType(InterviewSessionMessage.MessageType.CHAT);
        message.setInterviewId(interviewId);
        message.setTimestamp(Instant.now());
        return message;
    }

    /**
     * Code editor content sync for collaborative coding.
     */
    @MessageMapping("/interview/{interviewId}/code")
    @SendTo("/topic/interview/{interviewId}/code")
    public InterviewSessionMessage codeUpdate(
            @DestinationVariable UUID interviewId,
            @Payload InterviewSessionMessage message) {
        message.setType(InterviewSessionMessage.MessageType.CODE);
        message.setInterviewId(interviewId);
        message.setTimestamp(Instant.now());
        return message;
    }

    /**
     * WebRTC signaling for video/audio — sent to specific user.
     */
    @MessageMapping("/interview/{interviewId}/signal")
    public void signalingMessage(
            @DestinationVariable UUID interviewId,
            @Payload InterviewSessionMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        message.setType(InterviewSessionMessage.MessageType.SIGNAL);
        message.setInterviewId(interviewId);
        message.setTimestamp(Instant.now());
        // Broadcast to the interview topic for signaling
        messagingTemplate.convertAndSend("/topic/interview/" + interviewId + "/signal", message);
    }

    /**
     * Interview status update (started, ended, etc.)
     */
    @MessageMapping("/interview/{interviewId}/status")
    @SendTo("/topic/interview/{interviewId}")
    public InterviewSessionMessage statusUpdate(
            @DestinationVariable UUID interviewId,
            @Payload InterviewSessionMessage message) {
        message.setType(InterviewSessionMessage.MessageType.STATUS_UPDATE);
        message.setInterviewId(interviewId);
        message.setTimestamp(Instant.now());
        return message;
    }
}

