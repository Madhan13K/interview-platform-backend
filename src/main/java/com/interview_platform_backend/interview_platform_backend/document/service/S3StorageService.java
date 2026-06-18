package com.interview_platform_backend.interview_platform_backend.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * Service responsible for all AWS S3 interactions.
 * Handles file upload, deletion, and presigned URL generation.
 *
 * <p>Configure AWS credentials via environment variables or application properties:</p>
 * <ul>
 *   <li>{@code aws.s3.access-key} - AWS access key</li>
 *   <li>{@code aws.s3.secret-key} - AWS secret key</li>
 *   <li>{@code aws.s3.bucket-name} - Target S3 bucket</li>
 *   <li>{@code aws.s3.region} - AWS region</li>
 *   <li>{@code aws.s3.endpoint} - Custom endpoint for LocalStack/MinIO</li>
 * </ul>
 */
@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    @Value("${aws.s3.bucket-name:interview-platform-documents}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.access-key:}")
    private String accessKey;

    @Value("${aws.s3.secret-key:}")
    private String secretKey;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    @Value("${aws.s3.presigned-url-expiry-minutes:60}")
    private int presignedUrlExpiryMinutes;

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private boolean configured = false;

    @PostConstruct
    public void init() {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            log.warn("AWS S3 credentials not configured. Document upload/download will not work. " +
                    "Set aws.s3.access-key and aws.s3.secret-key to enable S3 storage.");
            return;
        }

        var credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );

        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider);

        var presignerBuilder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider);

        if (endpoint != null && !endpoint.isBlank()) {
            URI endpointUri = URI.create(endpoint);
            builder.endpointOverride(endpointUri)
                    .forcePathStyle(true); // Required for LocalStack/MinIO
            presignerBuilder.endpointOverride(endpointUri);
        }

        this.s3Client = builder.build();
        this.s3Presigner = presignerBuilder.build();
        this.configured = true;

        // Auto-create bucket if it doesn't exist (for LocalStack/local development)
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("S3 bucket '{}' exists", bucketName);
        } catch (NoSuchBucketException e) {
            log.info("S3 bucket '{}' not found, creating it...", bucketName);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("S3 bucket '{}' created successfully", bucketName);
        } catch (Exception e) {
            log.warn("Could not verify bucket existence (may be fine for LocalStack): {}", e.getMessage());
        }

        log.info("S3 client initialized with bucket: {}, region: {}, endpoint: {}",
                bucketName, region, endpoint != null && !endpoint.isBlank() ? endpoint : "AWS default");
    }

    private void ensureConfigured() {
        if (!configured) {
            throw new IllegalStateException("S3 storage is not configured. Set aws.s3.access-key and aws.s3.secret-key.");
        }
    }

    /**
     * Upload a file to S3
     */
    public String uploadFile(MultipartFile file, String s3Key) throws IOException {
        ensureConfigured();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        String s3Url = generateS3Url(s3Key);
        log.info("File uploaded to S3: {}", s3Url);
        return s3Url;
    }

    /**
     * Delete a file from S3
     */
    public void deleteFile(String s3Key) {
        ensureConfigured();
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("File deleted from S3: {}", s3Key);
    }

    /**
     * Generate a presigned URL for downloading a file
     */
    public String generatePresignedDownloadUrl(String s3Key) {
        ensureConfigured();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Generate a presigned URL for uploading a file
     */
    public String generatePresignedUploadUrl(String s3Key, String contentType) {
        ensureConfigured();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Generate a unique S3 key for a file
     */
    public String generateS3Key(String documentType, UUID userId, String originalFileName) {
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }
        return String.format("%s/%s/%s%s",
                documentType.toLowerCase(),
                userId.toString(),
                UUID.randomUUID(),
                extension);
    }

    public String getBucketName() {
        return bucketName;
    }

    public int getPresignedUrlExpiryMinutes() {
        return presignedUrlExpiryMinutes;
    }

    private String generateS3Url(String s3Key) {
        if (endpoint != null && !endpoint.isBlank()) {
            return String.format("%s/%s/%s", endpoint, bucketName, s3Key);
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }
}

