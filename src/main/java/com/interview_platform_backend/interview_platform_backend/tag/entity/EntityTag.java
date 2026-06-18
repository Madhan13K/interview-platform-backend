package com.interview_platform_backend.interview_platform_backend.tag.entity;

import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entity_tags", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tag_id", "entity_type", "entity_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // INTERVIEW, USER, QUESTION, JOB_POSITION

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tagged_by")
    private User taggedBy;

    @Column(name = "tagged_at", nullable = false)
    private Instant taggedAt;

    @PrePersist
    protected void onCreate() { taggedAt = Instant.now(); }
}

