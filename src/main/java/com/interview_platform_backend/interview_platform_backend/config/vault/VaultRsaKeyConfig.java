package com.interview_platform_backend.interview_platform_backend.config.vault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads RSA keys from Vault or environment variables instead of classpath files.
 * 
 * In production, keys are stored in HashiCorp Vault at:
 *   secret/interview-platform/rsa-keys
 *     - public-key: PEM-encoded RSA public key
 *     - private-key: PEM-encoded RSA private key
 * 
 * When Vault is not available (dev), falls back to classpath keys.
 */
@Configuration
@ConditionalOnProperty(name = "app.security.rsa.from-vault", havingValue = "true")
public class VaultRsaKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(VaultRsaKeyConfig.class);

    @Value("${rsa.public-key-pem:}")
    private String publicKeyPem;

    @Value("${rsa.private-key-pem:}")
    private String privateKeyPem;

    @Bean
    public RSAPublicKey rsaPublicKey() throws Exception {
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw new IllegalStateException("RSA public key not found in Vault. " +
                    "Ensure 'rsa.public-key-pem' is set in Vault at secret/interview-platform");
        }

        String cleaned = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(cleaned);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        log.info("RSA public key loaded from Vault");
        return (RSAPublicKey) kf.generatePublic(keySpec);
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey() throws Exception {
        if (privateKeyPem == null || privateKeyPem.isBlank()) {
            throw new IllegalStateException("RSA private key not found in Vault. " +
                    "Ensure 'rsa.private-key-pem' is set in Vault at secret/interview-platform");
        }

        String cleaned = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(cleaned);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        log.info("RSA private key loaded from Vault");
        return (RSAPrivateKey) kf.generatePrivate(keySpec);
    }
}
