package com.interview_platform_backend.interview_platform_backend.whiteboard.websocket;

import com.interview_platform_backend.interview_platform_backend.whiteboard.dto.WhiteboardStrokeResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WhiteboardWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public WhiteboardWebSocketHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastStroke(UUID sessionId, WhiteboardStrokeResponse strokeResponse) {
        messagingTemplate.convertAndSend(
                "/topic/whiteboard/" + sessionId,
                strokeResponse
        );
    }
}
