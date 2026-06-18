package com.interview_platform_backend.interview_platform_backend.video.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartRecordingRequest {

    @NotNull
    private UUID interviewId;
}
