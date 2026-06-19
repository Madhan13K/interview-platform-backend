package com.interview_platform_backend.interview_platform_backend;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that need real database and messaging infrastructure.
 * Uses Testcontainers to spin up PostgreSQL and Kafka automatically.
 * Containers are shared across all tests (static) for speed.
 * <p>
 * Usage: extend this class in your integration test:
 * <pre>
 * {@code @SpringBootTest}
 * {@code @ActiveProfiles("integration")}
 * class MyServiceIntegrationTest extends AbstractIntegrationTest { ... }
 * </pre>
 */
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres;
    static final KafkaContainer kafka;

    static {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("interview_platform_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();

        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
        kafka.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");

        // Flyway - disabled, using ddl-auto create-drop
        registry.add("spring.flyway.enabled", () -> "false");

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.listener.auto-startup", () -> "false");

        // Disable services not needed for integration tests
        registry.add("app.kafka.enabled", () -> "false");
        registry.add("app.notifications.enabled", () -> "false");
        registry.add("app.sms.enabled", () -> "false");

        // Redis - disable completely for tests
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.autoconfigure.exclude", () ->
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");

        // Docker compose - disable
        registry.add("spring.docker.compose.enabled", () -> "false");

        // Encryption - disable for test speed
        registry.add("app.encryption.enabled", () -> "false");

        // Code execution - disable
        registry.add("app.code-execution.enabled", () -> "false");
    }
}
