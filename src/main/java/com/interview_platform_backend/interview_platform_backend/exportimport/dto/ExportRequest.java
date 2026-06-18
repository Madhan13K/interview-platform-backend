package com.interview_platform_backend.interview_platform_backend.exportimport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportRequest {

    @NotBlank(message = "Entity type is required")
    private String entityType;

    @NotBlank(message = "Format is required (CSV, EXCEL, JSON)")
    private String format;

    private Map<String, String> filters;
}
