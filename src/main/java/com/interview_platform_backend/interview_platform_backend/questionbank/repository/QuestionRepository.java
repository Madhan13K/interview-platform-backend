package com.interview_platform_backend.interview_platform_backend.questionbank.repository;

import com.interview_platform_backend.interview_platform_backend.questionbank.entity.Question;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionDifficulty;
import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findByCategoryIdAndIsActiveTrue(UUID categoryId);

    List<Question> findByDifficultyAndIsActiveTrue(QuestionDifficulty difficulty);

    List<Question> findByTypeAndIsActiveTrue(QuestionType type);

    Page<Question> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.isActive = true " +
            "AND (:categoryId IS NULL OR q.category.id = :categoryId) " +
            "AND (:difficulty IS NULL OR q.difficulty = :difficulty) " +
            "AND (:type IS NULL OR q.type = :type) " +
            "AND (:keyword IS NULL OR LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(q.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Question> searchQuestions(
            @Param("categoryId") UUID categoryId,
            @Param("difficulty") QuestionDifficulty difficulty,
            @Param("type") QuestionType type,
            @Param("keyword") String keyword,
            Pageable pageable);
}

