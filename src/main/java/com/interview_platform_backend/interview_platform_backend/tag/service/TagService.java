package com.interview_platform_backend.interview_platform_backend.tag.service;

import com.interview_platform_backend.interview_platform_backend.exception.DuplicateResourceException;
import com.interview_platform_backend.interview_platform_backend.exception.ResourceNotFoundException;
import com.interview_platform_backend.interview_platform_backend.tag.dto.*;
import com.interview_platform_backend.interview_platform_backend.tag.entity.*;
import com.interview_platform_backend.interview_platform_backend.tag.repository.*;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final EntityTagRepository entityTagRepository;
    private final UserRepository userRepository;

    public TagService(TagRepository tagRepository, EntityTagRepository entityTagRepository, UserRepository userRepository) {
        this.tagRepository = tagRepository;
        this.entityTagRepository = entityTagRepository;
        this.userRepository = userRepository;
    }

    @CacheEvict(value = "tags", allEntries = true)
    public TagResponse createTag(CreateTagRequest request, UUID createdBy) {
        if (tagRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Tag with name '" + request.getName() + "' already exists");
        }
        User user = userRepository.findById(createdBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", createdBy));

        Tag tag = Tag.builder()
                .name(request.getName())
                .color(request.getColor() != null ? request.getColor() : "#6c757d")
                .category(request.getCategory())
                .createdBy(user)
                .build();

        return mapToResponse(tagRepository.save(tag));
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getTagsByCategory(String category) {
        return tagRepository.findByCategory(category).stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> searchTags(String query) {
        return tagRepository.findByNameContainingIgnoreCase(query).stream().map(this::mapToResponse).toList();
    }

    @CacheEvict(value = "tags", allEntries = true)
    public void deleteTag(UUID tagId) {
        if (!tagRepository.existsById(tagId)) throw new ResourceNotFoundException("Tag", "id", tagId);
        tagRepository.deleteById(tagId);
    }

    // ==================== Entity Tagging ====================

    public void tagEntity(UUID tagId, String entityType, UUID entityId, UUID taggedBy) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));

        if (entityTagRepository.existsByTagIdAndEntityTypeAndEntityId(tagId, entityType, entityId)) {
            throw new DuplicateResourceException("Entity is already tagged with this tag");
        }

        User user = userRepository.findById(taggedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", taggedBy));

        EntityTag entityTag = EntityTag.builder()
                .tag(tag).entityType(entityType).entityId(entityId).taggedBy(user).build();
        entityTagRepository.save(entityTag);
    }

    public void untagEntity(UUID tagId, String entityType, UUID entityId) {
        entityTagRepository.deleteByTagIdAndEntityTypeAndEntityId(tagId, entityType, entityId);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getTagsForEntity(String entityType, UUID entityId) {
        return entityTagRepository.findWithTagByEntity(entityType, entityId).stream()
                .map(et -> mapToResponse(et.getTag())).toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> getEntitiesByTag(UUID tagId, String entityType) {
        return entityTagRepository.findEntityIdsByTagAndType(tagId, entityType);
    }

    private TagResponse mapToResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .category(tag.getCategory())
                .createdById(tag.getCreatedBy() != null ? tag.getCreatedBy().getId() : null)
                .createdAt(tag.getCreatedAt())
                .build();
    }
}

