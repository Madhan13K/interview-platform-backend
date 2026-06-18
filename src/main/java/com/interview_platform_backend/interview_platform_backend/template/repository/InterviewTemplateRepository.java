package com.interview_platform_backend.interview_platform_backend.template.repository;

import com.interview_platform_backend.interview_platform_backend.candidate.entity.InterviewType;
import com.interview_platform_backend.interview_platform_backend.template.entity.InterviewTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewTemplateRepository extends JpaRepository<InterviewTemplate, UUID> {

    boolean existsByTitle(String title);

    @Query("SELECT t FROM InterviewTemplate t LEFT JOIN FETCH t.templateQuestions tq LEFT JOIN FETCH tq.question WHERE t.id = :id")
    Optional<InterviewTemplate> findByIdWithQuestions(@Param("id") UUID id);

    List<InterviewTemplate> findByIsActiveTrue();

    List<InterviewTemplate> findByType(InterviewType type);

    List<InterviewTemplate> findByTypeAndIsActiveTrue(InterviewType type);

    @Query("SELECT t FROM InterviewTemplate t WHERE t.isActive = true AND " +
            "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<InterviewTemplate> searchByKeyword(@Param("keyword") String keyword);

    Page<InterviewTemplate> findByIsActiveTrue(Pageable pageable);
}

