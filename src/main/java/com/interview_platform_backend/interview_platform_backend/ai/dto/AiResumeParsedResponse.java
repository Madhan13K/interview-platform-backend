package com.interview_platform_backend.interview_platform_backend.ai.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiResumeParsedResponse {

    private String candidateName;
    private String email;
    private String phone;
    private List<String> skills;
    private List<Map<String, String>> experience;
    private List<Map<String, String>> education;
    private String summary;
}
