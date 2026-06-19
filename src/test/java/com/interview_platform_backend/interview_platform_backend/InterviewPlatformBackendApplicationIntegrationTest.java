package com.interview_platform_backend.interview_platform_backend;

import com.interview_platform_backend.interview_platform_backend.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration")
class InterviewPlatformBackendApplicationIntegrationTest extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
