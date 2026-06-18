package com.interview_platform_backend.interview_platform_backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayMigrationConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .locations("classpath:db/migration")
                .load();
        // Repair syncs stored checksums to current files before migrating.
        // This handles cases where a migration file was updated after being applied.
        flyway.repair();
        flyway.migrate();
        return flyway;
    }
}
