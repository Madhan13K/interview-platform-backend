package com.interview_platform_backend.interview_platform_backend.tenant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    private String domain;

    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrganizationPlan plan = OrganizationPlan.FREE;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxUsers = 5;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum OrganizationPlan {
        FREE, STARTER, PROFESSIONAL, ENTERPRISE
    }
}
