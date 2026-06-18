package com.interview_platform_backend.interview_platform_backend.security.jwks;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes the public RSA key as a JWKS endpoint so that other microservices
 * can verify JWTs without sharing any secret.
 */
@RestController
public class JwksController {

    private final RsaKeyProperties rsaKeys;

    public JwksController(RsaKeyProperties rsaKeys) {
        this.rsaKeys = rsaKeys;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        RSAKey rsaKey = new RSAKey.Builder(rsaKeys.publicKey())
                .keyID("interview-platform-key-1")
                .build();
        return new JWKSet(rsaKey).toJSONObject();
    }
}

