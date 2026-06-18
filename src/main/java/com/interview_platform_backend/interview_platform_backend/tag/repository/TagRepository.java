package com.interview_platform_backend.interview_platform_backend.tag.repository;

import com.interview_platform_backend.interview_platform_backend.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);
    List<Tag> findByCategory(String category);
    List<Tag> findByNameContainingIgnoreCase(String name);
    boolean existsByName(String name);
}

