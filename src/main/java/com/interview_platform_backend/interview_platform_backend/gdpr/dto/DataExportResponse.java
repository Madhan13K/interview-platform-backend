package com.interview_platform_backend.interview_platform_backend.gdpr.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataExportResponse {

    private Map<String, Object> userData;
    private List<Map<String, Object>> interviews;
    private List<Map<String, Object>> feedback;
    private List<Map<String, Object>> documents;
    private Instant exportedAt;
}
