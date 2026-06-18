package com.interview_platform_backend.interview_platform_backend.user.mapper;

import com.interview_platform_backend.interview_platform_backend.user.dto.response.UserResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {

        List<String> roles = user.getUserRoles() == null
                ? Collections.emptyList()
                : user.getUserRoles().stream()
                .filter(userRole -> userRole != null && userRole.getRole() != null)
                .map(userRole -> userRole.getRole().getName())
                .filter(roleName -> roleName != null)
                .distinct()
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .roles(roles)
                .build();
    }
}

