package com.interview_platform_backend.interview_platform_backend.websocket;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Message payload for real-time interview session communication.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSessionMessage {

    public enum MessageType {
        JOIN,          // Participant joined the session
        LEAVE,         // Participant left the session
        CHAT,          // Chat message during interview
        CODE,          // Code editor content update
        NOTE,          // Interviewer private note
        SIGNAL,        // WebRTC signaling for video/audio
        STATUS_UPDATE  // Interview status change (started, ended)
    }

    private UUID interviewId;
    private UUID senderId;
    private String senderName;
    private MessageType type;
    private String content;
    private Instant timestamp;
}

