package com.interview_platform_backend.interview_platform_backend.encryption.service;

import com.interview_platform_backend.interview_platform_backend.encryption.config.EncryptionProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for field-level PII encryption.
 * 
 * Provides authenticated encryption with associated data (AEAD).
 * Each encryption operation uses a unique random IV to ensure semantic security.
 * 
 * Storage format: Base64(IV || Ciphertext || AuthTag)
 * 
 * Required for SOC2/GDPR compliance - encrypts PII at rest.
 */
@Service
public class FieldEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(FieldEncryptionService.class);
    private static final String AES_ALGORITHM = "AES";

    private final EncryptionProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    public FieldEncryptionService(EncryptionProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.warn("Field-level encryption is DISABLED. PII will be stored in plaintext.");
            return;
        }

        if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
            // Generate a warning-level log but don't fail startup in dev
            log.warn("No encryption key configured (app.encryption.secret-key). " +
                    "Using a derived key for development ONLY. Set ENCRYPTION_SECRET_KEY in production!");
            // Derive a deterministic dev key (NOT for production)
            byte[] devKey = new byte[32];
            System.arraycopy("dev-only-encryption-key-32bytes!".getBytes(), 0, devKey, 0, 32);
            this.secretKey = new SecretKeySpec(devKey, AES_ALGORITHM);
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(properties.getSecretKey());
            if (keyBytes.length != 32) {
                throw new IllegalStateException(
                        "Encryption key must be exactly 32 bytes (256 bits) when base64-decoded. Got: " + keyBytes.length);
            }
            this.secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
        }

        log.info("Field-level encryption initialized with {} (IV: {} bytes, Tag: {} bits)",
                properties.getAlgorithm(), properties.getIvLength(), properties.getTagLength());
    }

    /**
     * Encrypt a plaintext string value.
     * Returns a base64-encoded string containing IV + ciphertext + auth tag.
     *
     * @param plaintext The value to encrypt
     * @return Base64-encoded encrypted value, or the plaintext if encryption is disabled
     */
    public String encrypt(String plaintext) {
        if (!properties.isEnabled() || plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[properties.getIvLength()];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(properties.getAlgorithm());
            GCMParameterSpec parameterSpec = new GCMParameterSpec(properties.getTagLength(), iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Combine IV + ciphertext (GCM appends auth tag to ciphertext automatically)
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            // Encode as base64 with a prefix to identify encrypted values
            return "ENC:" + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to encrypt field value", e);
        }
    }

    /**
     * Decrypt a previously encrypted value.
     *
     * @param encryptedValue The base64-encoded encrypted value (with "ENC:" prefix)
     * @return The decrypted plaintext
     */
    public String decrypt(String encryptedValue) {
        if (!properties.isEnabled() || encryptedValue == null || encryptedValue.isEmpty()) {
            return encryptedValue;
        }

        // If value doesn't have the encryption prefix, return as-is (backward compatibility)
        if (!encryptedValue.startsWith("ENC:")) {
            return encryptedValue;
        }

        try {
            // Remove prefix and decode
            byte[] decoded = Base64.getDecoder().decode(encryptedValue.substring(4));

            // Extract IV
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[properties.getIvLength()];
            buffer.get(iv);

            // Extract ciphertext (includes auth tag)
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(properties.getAlgorithm());
            GCMParameterSpec parameterSpec = new GCMParameterSpec(properties.getTagLength(), iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to decrypt field value", e);
        }
    }

    /**
     * Check if a value is already encrypted (has the ENC: prefix).
     */
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith("ENC:");
    }

    /**
     * Check if encryption is enabled.
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }
}
