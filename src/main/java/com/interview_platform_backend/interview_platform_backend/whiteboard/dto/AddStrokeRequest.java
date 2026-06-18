package com.interview_platform_backend.interview_platform_backend.whiteboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddStrokeRequest {

    @NotBlank
    private String strokeData;

    private String tool;

    private String color;

    private Double strokeWidth;
}
