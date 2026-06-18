package com.interview_platform_backend.interview_platform_backend.security.oauth2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
public class OAuth2Controller {
    @GetMapping("/providers")
    public ResponseEntity<Map<String, String>> providers() {
        return ResponseEntity.ok(
                Map.of(
                        "google", "/oauth2/authorization/google",
                        "github", "/oauth2/authorization/github",
                        "microsoft", "/oauth2/authorization/microsoft"
                )
        );
    }
}
