package com.interview_platform_backend.interview_platform_backend.user.mapper;

import com.interview_platform_backend.interview_platform_backend.user.dto.response.UserProfileResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserProfile;
import org.springframework.stereotype.Component;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;

import java.util.List;

@Component
public class UserProfileMapper {

    public UserProfileResponse toResponse(UserProfile profile, User user, List<String> roles) {

        return UserProfileResponse.builder()
                .id(profile.getId())
                .bio(profile.getBio())
                .designation(profile.getDesignation())
                .company(profile.getCompany())
                .experienceYears(profile.getExperienceYears())
                .linkedinUrl(profile.getLinkedinUrl())
                .githubUrl(profile.getGithubUrl())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .resumeUrl(profile.getResumeUrl())
                .contactNumber(profile.getContactNumber())
                .skills(profile.getSkills())
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .roles(roles)
                .build();
    }
}

