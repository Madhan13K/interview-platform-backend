package com.interview_platform_backend.interview_platform_backend.user.dto.response;

import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;

import java.util.List;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private UUID id;

    private String firstName;

    private String lastName;

    private String email;

    private UserStatus status;

    private String phoneNumber;

    private List<String> roles;
}