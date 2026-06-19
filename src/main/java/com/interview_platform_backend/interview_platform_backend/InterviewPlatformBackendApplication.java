package com.interview_platform_backend.interview_platform_backend;

import com.interview_platform_backend.interview_platform_backend.security.jwks.RsaKeyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(RsaKeyProperties.class)
@EntityScan(basePackages = "com.interview_platform_backend.interview_platform_backend")
@EnableJpaRepositories(basePackages = "com.interview_platform_backend.interview_platform_backend")
@EnableScheduling
@EnableRetry
public class InterviewPlatformBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(InterviewPlatformBackendApplication.class, args);
	}

}
