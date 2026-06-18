package com.interview_platform_backend.interview_platform_backend.workflow.engine;

import com.interview_platform_backend.interview_platform_backend.workflow.entity.TriggerEvent;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowContext {

    private TriggerEvent triggerEvent;
    private String entityType;
    private UUID entityId;
    private UUID interviewId;
    private UUID candidateId;
    private UUID candidatePipelineId;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetadataValue(String key, Class<T> type) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
}
