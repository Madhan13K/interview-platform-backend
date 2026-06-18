package com.interview_platform_backend.interview_platform_backend.user.dto.response;
import lombok.*;

import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private Boolean success;

    private String message;

    private T data;

    private Instant timestamp;
}