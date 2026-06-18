package com.interview_platform_backend.interview_platform_backend.user.mapper;

import com.interview_platform_backend.interview_platform_backend.user.dto.response.RoleResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleResponse toResponse(Role role) {

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt())
                .build();
    }
}

