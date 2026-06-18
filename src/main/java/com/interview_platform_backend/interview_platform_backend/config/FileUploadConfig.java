package com.interview_platform_backend.interview_platform_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for file upload and multipart settings.
 * File size limits are configured in application.yml / application-s3.yml:
 * - spring.servlet.multipart.max-file-size=50MB
 * - spring.servlet.multipart.max-request-size=55MB
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${spring.servlet.multipart.max-file-size:50MB}")
    private String maxFileSize;

    @Value("${spring.servlet.multipart.max-request-size:55MB}")
    private String maxRequestSize;
}

