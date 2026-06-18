package com.interview_platform_backend.interview_platform_backend.codeeditor.dto;

import lombok.*;

import java.util.UUID;

/**
 * WebSocket message for real-time collaborative code editing.
 * Sent over /topic/interview/{interviewId}/code
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeChangeMessage {

    public enum ChangeType {
        FULL_SYNC,      // Full code content sync (on join)
        INSERT,         // Text inserted at position
        DELETE,         // Text deleted at range
        REPLACE,        // Text replaced at range
        CURSOR_MOVE,    // Cursor position update
        LANGUAGE_CHANGE // Programming language changed
    }

    private UUID interviewId;
    private UUID senderId;
    private String senderName;
    private ChangeType changeType;

    // For code changes
    private String content;
    private String language;

    // For incremental edits (OT-ready)
    private Integer startLine;
    private Integer startColumn;
    private Integer endLine;
    private Integer endColumn;
    private String text;

    // Cursor position (for showing other user's cursor)
    private Integer cursorLine;
    private Integer cursorColumn;

    private Long timestamp;
}

