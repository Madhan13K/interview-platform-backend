package com.interview_platform_backend.interview_platform_backend.dei.entity;

import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "demographic_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemographicProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Ethnicity ethnicity;

    @Column(name = "veteran_status")
    private Boolean veteranStatus;

    @Column(name = "disability_status")
    private Boolean disabilityStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_range", length = 30)
    private AgeRange ageRange;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;

    @Column(name = "consent_given_at")
    private Instant consentGivenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
