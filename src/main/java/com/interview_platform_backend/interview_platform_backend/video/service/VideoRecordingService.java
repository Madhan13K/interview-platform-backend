package com.interview_platform_backend.interview_platform_backend.video.service;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.Interview;
import com.interview_platform_backend.interview_platform_backend.candidate.repository.InterviewRepository;
import com.interview_platform_backend.interview_platform_backend.exception.BadRequestException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.user.dto.response.PaginatedResponse;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import com.interview_platform_backend.interview_platform_backend.video.dto.StartRecordingRequest;
import com.interview_platform_backend.interview_platform_backend.video.dto.VideoRecordingResponse;
import com.interview_platform_backend.interview_platform_backend.video.entity.VideoRecording;
import com.interview_platform_backend.interview_platform_backend.video.repository.VideoRecordingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class VideoRecordingService {

    private final VideoRecordingRepository videoRecordingRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name:interview-platform-recordings}")
    private String bucketName;

    public VideoRecordingService(VideoRecordingRepository videoRecordingRepository,
                                 InterviewRepository interviewRepository,
                                 UserRepository userRepository,
                                 S3Presigner s3Presigner) {
        this.videoRecordingRepository = videoRecordingRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
        this.s3Presigner = s3Presigner;
    }

    public VideoRecordingResponse startRecording(StartRecordingRequest request, UUID userId) {
        Interview interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", request.getInterviewId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String s3Key = String.format("recordings/%s/%s/%s.webm",
                interview.getId(), userId, UUID.randomUUID());

        String fileName = String.format("recording_%s_%s.webm",
                interview.getId().toString().substring(0, 8),
                Instant.now().toEpochMilli());

        VideoRecording recording = VideoRecording.builder()
                .organizationId(userId)
                .interview(interview)
                .recordedBy(user)
                .fileName(fileName)
                .s3Key(s3Key)
                .s3Bucket(bucketName)
                .mimeType("video/webm")
                .status(VideoRecording.RecordingStatus.PROCESSING)
                .startedAt(Instant.now())
                .build();

        VideoRecording saved = videoRecordingRepository.save(recording);
        return toResponse(saved, null);
    }

    public VideoRecordingResponse completeRecording(UUID recordingId, Long fileSizeBytes, Integer durationSeconds) {
        VideoRecording recording = videoRecordingRepository.findById(recordingId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoRecording", "id", recordingId));

        if (recording.getStatus() != VideoRecording.RecordingStatus.PROCESSING) {
            throw new BadRequestException("Recording is not in PROCESSING status");
        }

        recording.setStatus(VideoRecording.RecordingStatus.READY);
        recording.setFileSizeBytes(fileSizeBytes);
        recording.setDurationSeconds(durationSeconds);
        recording.setEndedAt(Instant.now());

        VideoRecording saved = videoRecordingRepository.save(recording);
        return toResponse(saved, generatePresignedUrl(saved.getS3Key(), saved.getS3Bucket()));
    }

    public VideoRecordingResponse failRecording(UUID recordingId, String reason) {
        VideoRecording recording = videoRecordingRepository.findById(recordingId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoRecording", "id", recordingId));

        if (recording.getStatus() != VideoRecording.RecordingStatus.PROCESSING) {
            throw new BadRequestException("Recording is not in PROCESSING status");
        }

        recording.setStatus(VideoRecording.RecordingStatus.FAILED);
        recording.setEndedAt(Instant.now());

        VideoRecording saved = videoRecordingRepository.save(recording);
        return toResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public List<VideoRecordingResponse> getRecordingsByInterview(UUID interviewId) {
        if (!interviewRepository.existsById(interviewId)) {
            throw new ResourceNotFoundException("Interview", "id", interviewId);
        }

        return videoRecordingRepository.findByInterviewId(interviewId).stream()
                .map(recording -> toResponse(recording, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VideoRecordingResponse getRecording(UUID recordingId) {
        VideoRecording recording = videoRecordingRepository.findById(recordingId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoRecording", "id", recordingId));

        String downloadUrl = null;
        if (recording.getStatus() == VideoRecording.RecordingStatus.READY) {
            downloadUrl = generatePresignedUrl(recording.getS3Key(), recording.getS3Bucket());
        }

        return toResponse(recording, downloadUrl);
    }

    public VideoRecordingResponse deleteRecording(UUID recordingId) {
        VideoRecording recording = videoRecordingRepository.findById(recordingId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoRecording", "id", recordingId));

        recording.setStatus(VideoRecording.RecordingStatus.DELETED);

        VideoRecording saved = videoRecordingRepository.save(recording);
        return toResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<VideoRecordingResponse> getMyRecordings(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VideoRecording> recordingPage = videoRecordingRepository.findByRecordedById(userId, pageRequest);

        List<VideoRecordingResponse> content = recordingPage.getContent().stream()
                .map(recording -> toResponse(recording, null))
                .collect(Collectors.toList());

        return PaginatedResponse.<VideoRecordingResponse>builder()
                .content(content)
                .page(recordingPage.getNumber())
                .size(recordingPage.getSize())
                .totalElements(recordingPage.getTotalElements())
                .totalPages(recordingPage.getTotalPages())
                .last(recordingPage.isLast())
                .build();
    }

    private String generatePresignedUrl(String s3Key, String bucket) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    private VideoRecordingResponse toResponse(VideoRecording recording, String downloadUrl) {
        User recordedBy = recording.getRecordedBy();
        String recordedByName = recordedBy != null
                ? recordedBy.getFirstName() + " " + recordedBy.getLastName()
                : null;

        return VideoRecordingResponse.builder()
                .id(recording.getId())
                .interviewId(recording.getInterview().getId())
                .recordedByUserId(recordedBy != null ? recordedBy.getId() : null)
                .recordedByName(recordedByName)
                .fileName(recording.getFileName())
                .fileSizeBytes(recording.getFileSizeBytes())
                .durationSeconds(recording.getDurationSeconds())
                .mimeType(recording.getMimeType())
                .status(recording.getStatus().name())
                .thumbnailUrl(recording.getThumbnailUrl())
                .downloadUrl(downloadUrl)
                .startedAt(recording.getStartedAt())
                .endedAt(recording.getEndedAt())
                .createdAt(recording.getCreatedAt())
                .build();
    }
}
