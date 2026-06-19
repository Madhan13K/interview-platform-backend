//package com.interview_platform_backend.interview_platform_backend.security.auth.integration;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.interview_platform_backend.interview_platform_backend.notification.kafka.NotificationProducer;
//import com.interview_platform_backend.interview_platform_backend.user.entity.PasswordResetToken;
//import com.interview_platform_backend.interview_platform_backend.user.repository.PasswordResetTokenRepository;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * Integration test covering the full forgot-password flow:
// * register -> forgot-password -> reset-password -> login with new password.
// *
// * Uses a dedicated test PostgreSQL database (interview_platform_test).
// * Prerequisites: PostgreSQL running on localhost:5432 with database interview_platform_test.
// */
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
//@ActiveProfiles("integration")
//@org.springframework.context.annotation.Import(TestObjectMapperConfig.class)
//class ForgotPasswordFlowIntegrationTest extends AbstractIntegrationTest {
//
//    @MockitoBean
//    @SuppressWarnings("unused")
//    private KafkaTemplate<String, String> kafkaTemplate;
//
//    @MockitoBean
//    @SuppressWarnings("unused")
//    private NotificationProducer notificationProducer;
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private PasswordResetTokenRepository passwordResetTokenRepository;
//
//    @Test
//    void fullForgotPasswordFlow_register_forgot_reset_loginWithNewPassword() throws Exception {
//        String email = "flowtest_" + System.currentTimeMillis() + "@example.com";
//        String originalPassword = "OriginalPass123!";
//        String newPassword = "NewSecurePass456!";
//
//        // STEP 1: Register a new user
//        String registerPayload = "{\"firstName\":\"Flow\",\"lastName\":\"Test\",\"email\":\"" + email + "\",\"password\":\"" + originalPassword + "\",\"phoneNumber\":\"1234567890\"}";
//
//        mockMvc.perform(post("/api/v1/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(registerPayload))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").isNotEmpty())
//                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
//
//        // STEP 2: Verify login with original password
//        String loginPayload = "{\"email\":\"" + email + "\",\"password\":\"" + originalPassword + "\"}";
//
//        mockMvc.perform(post("/api/v1/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(loginPayload))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").isNotEmpty());
//
//        // STEP 3: Request forgot password
//        String forgotPayload = "{\"email\":\"" + email + "\"}";
//
//        mockMvc.perform(post("/api/v1/auth/forgot-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(forgotPayload))
//                .andExpect(status().isNoContent());
//
//        // STEP 4: Retrieve the reset token from the database
//        // (In production, the user would get this via email)
//        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll();
//        PasswordResetToken resetToken = tokens.stream()
//                .filter(t -> !Boolean.TRUE.equals(t.getUsed()))
//                .filter(t -> t.getUser().getEmail().equals(email))
//                .findFirst()
//                .orElseThrow(() -> new AssertionError("No reset token found for user"));
//
//        assertThat(resetToken.getToken()).isNotBlank();
//        assertThat(resetToken.getExpiryTime()).isNotNull();
//        assertThat(resetToken.getUsed()).isFalse();
//
//        // STEP 5: Reset the password using the token
//        String resetPayload = "{\"token\":\"" + resetToken.getToken() + "\",\"newPassword\":\"" + newPassword + "\"}";
//
//        mockMvc.perform(post("/api/v1/auth/reset-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(resetPayload))
//                .andExpect(status().isNoContent());
//
//        // STEP 6: Verify login with OLD password FAILS
//        mockMvc.perform(post("/api/v1/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(loginPayload))
//                .andExpect(status().is4xxClientError());
//
//        // STEP 7: Verify login with NEW password SUCCEEDS
//        String newLoginPayload = "{\"email\":\"" + email + "\",\"password\":\"" + newPassword + "\"}";
//
//        mockMvc.perform(post("/api/v1/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(newLoginPayload))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").isNotEmpty())
//                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
//
//        // STEP 8: Verify the reset token is now marked as used
//        List<PasswordResetToken> usedTokens = passwordResetTokenRepository.findAll();
//        boolean allUsed = usedTokens.stream()
//                .filter(t -> t.getUser().getEmail().equals(email))
//                .allMatch(t -> Boolean.TRUE.equals(t.getUsed()));
//        assertThat(allUsed).isTrue();
//
//        // STEP 9: Verify reusing the same reset token FAILS
//        mockMvc.perform(post("/api/v1/auth/reset-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(resetPayload))
//                .andExpect(status().is5xxServerError());
//    }
//
//    @Test
//    void forgotPassword_unknownEmail_returnsNoContentSilently() throws Exception {
//        // Should return 204 even for unknown emails (prevents user enumeration)
//        mockMvc.perform(post("/api/v1/auth/forgot-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"email\":\"nonexistent_unknown@example.com\"}"))
//                .andExpect(status().isNoContent());
//    }
//
//    @Test
//    void resetPassword_invalidToken_returnsError() throws Exception {
//        mockMvc.perform(post("/api/v1/auth/reset-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"token\":\"non-existent-token-12345\",\"newPassword\":\"SomePassword123!\"}"))
//                .andExpect(status().is5xxServerError());
//    }
//}
