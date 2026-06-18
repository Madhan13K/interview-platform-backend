package com.interview_platform_backend.interview_platform_backend.tag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateTagRequest {
    @NotBlank private String name;
    private String color;
    private String category;
}

