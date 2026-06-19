package com.interview_platform_backend.interview_platform_backend.security;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.LoginRequest;
import com.interview_platform_backend.interview_platform_backend.security.auth.dto.RegisterRequest;
import com.interview_platform_backend.interview_platform_backend.user.dto.request.RefreshTokenRequest;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserStatus;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
@TestMethodOrder(MethodOrderer.MethodName.class)
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String email;
    private String password;

    @BeforeEach
    void setUp() {
        email = "auth-int-" + UUID.randomUUID() + "@example.com";
        password = "Password@123";
    }

    /**
     * Helper: registers a user and immediately activates them (bypasses email verification).
     */
    private String registerAndActivate(RegisterRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Activate the user directly in DB (bypass email verification for tests)
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        return response;
    }

    @Test
    void test01_register_returnsAccessAndRefreshToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Auth");
        request.setLastName("Integration");
        request.setEmail(email);
        request.setPassword(password);
        request.setPhoneNumber("9999999999");

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> json = objectMapper.readValue(response, Map.class);
        assertThat(json.get("accessToken")).isNotNull();
        assertThat(((String) json.get("accessToken"))).isNotBlank();
        assertThat(json.get("refreshToken")).isNotNull();
        assertThat(((String) json.get("refreshToken"))).isNotBlank();
    }

    @Test
    void test02_login_returnsAccessAndRefreshToken() throws Exception {
        RegisterRequest register = new RegisterRequest();
        register.setFirstName("Login");
        register.setLastName("Integration");
        register.setEmail(email);
        register.setPassword(password);
        register.setPhoneNumber("8888888888");

        registerAndActivate(register);

        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> json = objectMapper.readValue(response, Map.class);
        assertThat(json.get("accessToken")).isNotNull();
        assertThat(((String) json.get("accessToken"))).isNotBlank();
        assertThat(json.get("refreshToken")).isNotNull();
        assertThat(((String) json.get("refreshToken"))).isNotBlank();
    }

    @Test
    void test03_refresh_withValidToken_returnsNewTokens() throws Exception {
        RegisterRequest register = new RegisterRequest();
        register.setFirstName("Refresh");
        register.setLastName("Integration");
        register.setEmail(email);
        register.setPassword(password);
        register.setPhoneNumber("7777777777");

        registerAndActivate(register);

        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> loginJson = objectMapper.readValue(loginResponse, Map.class);
        String oldAccessToken = (String) loginJson.get("accessToken");
        String oldRefreshToken = (String) loginJson.get("refreshToken");

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(oldRefreshToken);

        String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> refreshJson = objectMapper.readValue(refreshResponse, Map.class);
        String newAccessToken = (String) refreshJson.get("accessToken");
        String newRefreshToken = (String) refreshJson.get("refreshToken");

        assertThat(newAccessToken).isNotBlank();
        assertThat(newRefreshToken).isNotBlank();
        assertThat(newAccessToken).isNotEqualTo(oldAccessToken);
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);
    }

    @Test
    void test04_refresh_withInvalidToken_returns4xx() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("invalid.refresh.token");

        int status = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(400, 401, 403, 500);
    }

    @Test
    void test05_refresh_reuseOldRefreshToken_shouldFailAfterRotation() throws Exception {
        RegisterRequest register = new RegisterRequest();
        register.setFirstName("Rotate");
        register.setLastName("Integration");
        register.setEmail(email);
        register.setPassword(password);
        register.setPhoneNumber("6666666666");

        registerAndActivate(register);

        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> loginJson = objectMapper.readValue(loginResponse, Map.class);
        String firstRefreshToken = (String) loginJson.get("refreshToken");

        RefreshTokenRequest firstRefreshReq = new RefreshTokenRequest();
        firstRefreshReq.setRefreshToken(firstRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRefreshReq)))
                .andExpect(status().isOk());

        int reuseStatus = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRefreshReq)))
                .andReturn()
                .getResponse()
                .getStatus();

        // Old token should be revoked after rotation.
        assertThat(reuseStatus).isIn(400, 401, 403, 500);
    }
}