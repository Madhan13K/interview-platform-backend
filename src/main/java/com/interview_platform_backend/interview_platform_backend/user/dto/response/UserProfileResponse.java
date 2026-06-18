package com.interview_platform_backend.interview_platform_backend.user.dto.response;

import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String bio;
    private String designation;
    private String company;
    private Integer experienceYears;
    private String linkedinUrl;
    private String githubUrl;
    private String profilePictureUrl;
    private String resumeUrl;
    private String contactNumber;
    private List<String> skills;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private UserStatus status;
    private List<String> roles;
}

