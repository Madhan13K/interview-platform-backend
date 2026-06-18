package com.interview_platform_backend.interview_platform_backend.security.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO for admin/recruiter to create users with specific roles.
 * Used by protected endpoints — not public self-registration.
 */
@Getter
@Setter
public class AdminCreateUserRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String phoneNumber;

    /**
     * Roles to assign: e.g. ["CANDIDATE", "INTERVIEWER"]
     * At least one role is required.
     */
    @NotEmpty(message = "At least one role must be specified")
    private List<String> roles;
}

