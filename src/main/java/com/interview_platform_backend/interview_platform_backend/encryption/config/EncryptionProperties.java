package com.interview_platform_backend.interview_platform_backend.encryption.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.encryption")
@Getter
@Setter
public class EncryptionProperties {

    /**
     * Whether field-level encryption is enabled.
     * When disabled, data is stored in plaintext (useful for development).
     */
    private boolean enabled = true;

    /**
     * AES-256 encryption key (base64-encoded, 32 bytes / 256 bits).
     * MUST be set in production via environment variable.
     */
    private String secretKey;

    /**
     * Algorithm to use for encryption.
     * Default: AES/GCM/NoPadding (authenticated encryption).
     */
    private String algorithm = "AES/GCM/NoPadding";

    /**
     * GCM IV length in bytes (default: 12 bytes / 96 bits as recommended by NIST).
     */
    private int ivLength = 12;

    /**
     * GCM authentication tag length in bits (default: 128 bits).
     */
    private int tagLength = 128;
}
