package com.interview_platform_backend.interview_platform_backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configures Flyway to use a separate DDL-privileged user for migrations.
 * 
 * In production:
 * - Flyway (DDL user): Has CREATE, ALTER, DROP privileges for schema management
 * - Application (DML user): Has only SELECT, INSERT, UPDATE, DELETE
 * 
 * This separation follows the principle of least privilege (SOC2/security best practice).
 * If the application is compromised, the attacker cannot drop tables or alter schema.
 * 
 * Configuration:
 *   app.database.ddl.url      - JDBC URL for DDL operations (can be same DB, different user)
 *   app.database.ddl.username - DDL user (e.g., ddl_admin)
 *   app.database.ddl.password - DDL user password
 * 
 * Enable with: app.database.separate-users=true
 */
@Configuration
@ConditionalOnProperty(name = "app.database.separate-users", havingValue = "true")
public class FlywayDdlUserConfig {

    @Value("${app.database.ddl.url:${spring.datasource.url}}")
    private String ddlUrl;

    @Value("${app.database.ddl.username:${spring.datasource.username}}")
    private String ddlUsername;

    @Value("${app.database.ddl.password:${spring.datasource.password}}")
    private String ddlPassword;

    /**
     * Provides a Flyway bean configured with DDL user credentials.
     * This overrides Spring Boot's auto-configured Flyway which would use
     * the application datasource (DML-only user).
     * 
     * The migration runs automatically on startup via Flyway's default behavior.
     */
    @Bean(initMethod = "migrate")
    @Primary
    public Flyway flyway() {
        return Flyway.configure()
                .dataSource(ddlUrl, ddlUsername, ddlPassword)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .outOfOrder(false)
                .cleanDisabled(true) // NEVER allow clean in production
                .load();
    }
}
