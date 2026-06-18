package com.interview_platform_backend.interview_platform_backend.user.entity;

import com.interview_platform_backend.interview_platform_backend.encryption.converter.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private String profilePictureUrl;

    private String bio;

    private List<String> skills;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "contact_number")
    private String contactNumber;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "github_url")
    private String githubUrl;

    private String resumeUrl;

    private String designation;

    private String company;

    private Integer experienceYears;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile", nullable = false)
    private User user;
}
