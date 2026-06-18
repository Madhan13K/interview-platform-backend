package com.interview_platform_backend.interview_platform_backend.template.repository;

import com.interview_platform_backend.interview_platform_backend.template.entity.TemplateQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateQuestionRepository extends JpaRepository<TemplateQuestion, UUID> {

    List<TemplateQuestion> findByTemplateIdOrderByOrderIndex(UUID templateId);

    void deleteByTemplateId(UUID templateId);
}

