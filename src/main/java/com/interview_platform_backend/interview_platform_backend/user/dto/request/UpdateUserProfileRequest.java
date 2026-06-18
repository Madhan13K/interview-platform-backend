package com.interview_platform_backend.interview_platform_backend.user.dto.request;

import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateUserProfileRequest {

    private String bio;

    private String designation;

    private String company;

    private Integer experienceYears;

    private String linkedinUrl;

    private String githubUrl;

    private String resumeUrl;

    private List<Role> roleList;
}