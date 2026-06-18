package com.interview_platform_backend.interview_platform_backend.video.repository;

import com.interview_platform_backend.interview_platform_backend.video.entity.VideoRecording;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRecordingRepository extends JpaRepository<VideoRecording, UUID> {

    List<VideoRecording> findByInterviewId(UUID interviewId);

    Page<VideoRecording> findByRecordedById(UUID recordedById, Pageable pageable);

    List<VideoRecording> findByStatus(VideoRecording.RecordingStatus status);
}
