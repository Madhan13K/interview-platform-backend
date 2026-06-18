package com.interview_platform_backend.interview_platform_backend.encryption.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

/**
 * Utility to migrate existing plaintext PII data to encrypted form.
 * 
 * Run with profile "encrypt-migrate" to trigger one-time migration:
 *   --spring.profiles.active=dev,encrypt-migrate
 * 
 * This will:
 * 1. Scan all PII columns for unencrypted values (no "ENC:" prefix)
 * 2. Encrypt each value in-place
 * 3. Report the number of migrated records
 */
@Component
@Profile("encrypt-migrate")
public class EncryptionMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EncryptionMigrationRunner.class);

    private final FieldEncryptionService encryptionService;
    private final JdbcTemplate jdbcTemplate;

    public EncryptionMigrationRunner(FieldEncryptionService encryptionService, JdbcTemplate jdbcTemplate) {
        this.encryptionService = encryptionService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (!encryptionService.isEnabled()) {
            log.warn("Encryption is disabled. Skipping migration.");
            return;
        }

        log.info("Starting PII encryption migration...");

        int total = 0;
        total += migrateColumn("users", "phone_number", "id");
        total += migrateColumn("user_profiles", "contact_number", "id");
        total += migrateColumn("user_profiles", "linkedin_url", "id");
        total += migrateColumn("user_profiles", "github_url", "id");
        total += migrateColumn("job_positions", "salary_min", "id");
        total += migrateColumn("job_positions", "salary_max", "id");

        log.info("PII encryption migration complete. Total fields encrypted: {}", total);
    }

    private int migrateColumn(String table, String column, String idColumn) {
        try {
            String query = String.format(
                    "SELECT %s, %s FROM %s WHERE %s IS NOT NULL AND %s != '' AND %s NOT LIKE 'ENC:%%'",
                    idColumn, column, table, column, column, column);

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
            int count = 0;

            for (Map<String, Object> row : rows) {
                Object id = row.get(idColumn);
                String plaintext = (String) row.get(column);

                if (plaintext != null && !plaintext.isEmpty() && !plaintext.startsWith("ENC:")) {
                    String encrypted = encryptionService.encrypt(plaintext);
                    String update = String.format("UPDATE %s SET %s = ? WHERE %s = ?::uuid", table, column, idColumn);
                    jdbcTemplate.update(update, encrypted, id.toString());
                    count++;
                }
            }

            if (count > 0) {
                log.info("  Encrypted {} values in {}.{}", count, table, column);
            }
            return count;
        } catch (Exception e) {
            log.warn("  Could not migrate {}.{}: {}", table, column, e.getMessage());
            return 0;
        }
    }

    /**
     * Utility method to generate a new AES-256 key.
     * Call this to generate a key for production use.
     */
    public static String generateKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
