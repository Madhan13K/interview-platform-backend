package com.interview_platform_backend.interview_platform_backend.user.dto.response;

import java.time.Instant;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private Integer status;

    private String error;

    private String errorCode;

    private String message;

    private String path;

    private Instant timestamp;
}