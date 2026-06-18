package com.interview_platform_backend.interview_platform_backend.user.dto.request;

import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchRequest {

    private String keyword;

    private UserStatus status;

    private Integer page = 0;

    private Integer size = 10;
}