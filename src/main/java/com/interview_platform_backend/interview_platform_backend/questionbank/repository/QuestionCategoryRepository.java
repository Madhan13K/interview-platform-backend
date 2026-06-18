package com.interview_platform_backend.interview_platform_backend.questionbank.repository;

import com.interview_platform_backend.interview_platform_backend.questionbank.entity.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuestionCategoryRepository extends JpaRepository<QuestionCategory, UUID> {

    Optional<QuestionCategory> findByName(String name);

    boolean existsByName(String name);
}

