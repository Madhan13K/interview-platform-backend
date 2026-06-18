package com.interview_platform_backend.interview_platform_backend.ai.repository;

import com.interview_platform_backend.interview_platform_backend.ai.entity.AiSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {

    Page<AiSuggestion> findByUserId(UUID userId, Pageable pageable);

    List<AiSuggestion> findByUserIdAndType(UUID userId, AiSuggestion.AiSuggestionType type);

    List<AiSuggestion> findByInterviewId(UUID interviewId);
}
