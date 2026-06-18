package com.interview_platform_backend.interview_platform_backend.notification.slack.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SlackMessage {
    private String channel;
    private String text;
    private List<SlackBlock> blocks;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SlackBlock {
        private String type;
        private SlackText text;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SlackText {
        private String type;
        private String text;
    }
}
