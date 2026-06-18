package com.interview_platform_backend.interview_platform_backend.whiteboard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWhiteboardRequest {

    @NotNull
    private UUID interviewId;

    private String title;
}
