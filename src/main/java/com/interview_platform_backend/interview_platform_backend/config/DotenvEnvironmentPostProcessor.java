package com.interview_platform_backend.interview_platform_backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads environment variables from a .env file in the project root
 * and adds them as a property source with lower priority than actual
 * OS environment variables (so real env vars always take precedence).
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotenvPath = findDotenvFile();
        if (dotenvPath == null || !Files.exists(dotenvPath)) {
            return;
        }

        try {
            Map<String, Object> envVars = parseDotenv(dotenvPath);
            if (!envVars.isEmpty()) {
                MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, envVars);
                // Add after systemEnvironment so real env vars take precedence
                environment.getPropertySources().addLast(propertySource);
            }
        } catch (IOException e) {
            // Silently ignore if .env cannot be read
        }
    }

    private Path findDotenvFile() {
        // Try current working directory
        Path cwd = Paths.get(System.getProperty("user.dir"), ".env");
        if (Files.exists(cwd)) {
            return cwd;
        }
        // Try project root (parent of target)
        Path parent = Paths.get(System.getProperty("user.dir")).getParent();
        if (parent != null) {
            Path parentEnv = parent.resolve(".env");
            if (Files.exists(parentEnv)) {
                return parentEnv;
            }
        }
        return null;
    }

    private Map<String, Object> parseDotenv(Path path) throws IOException {
        Map<String, Object> vars = new HashMap<>();
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            // Skip comments and empty lines
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eqIndex = trimmed.indexOf('=');
            if (eqIndex > 0) {
                String key = trimmed.substring(0, eqIndex).trim();
                String value = trimmed.substring(eqIndex + 1).trim();
                // Remove surrounding quotes if present
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                vars.put(key, value);
            }
        }
        return vars;
    }
}

