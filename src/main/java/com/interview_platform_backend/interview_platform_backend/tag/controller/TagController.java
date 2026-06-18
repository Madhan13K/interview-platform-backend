package com.interview_platform_backend.interview_platform_backend.tag.controller;

import com.interview_platform_backend.interview_platform_backend.security.util.SecurityHelper;
import com.interview_platform_backend.interview_platform_backend.tag.dto.*;
import com.interview_platform_backend.interview_platform_backend.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tags")
@Tag(name = "Tags & Labels", description = "Tag interviews, candidates, questions for better filtering")
public class TagController {

    private final TagService tagService;
    private final SecurityHelper securityHelper;

    public TagController(TagService tagService, SecurityHelper securityHelper) {
        this.tagService = tagService;
        this.securityHelper = securityHelper;
    }

    @Operation(summary = "Create a tag")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TagResponse> createTag(@RequestBody @Valid CreateTagRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(tagService.createTag(request, userId));
    }

    @Operation(summary = "Get all tags")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TagResponse>> getAllTags() {
        return ResponseEntity.ok(tagService.getAllTags());
    }

    @Operation(summary = "Get tags by category")
    @GetMapping("/category/{category}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TagResponse>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(tagService.getTagsByCategory(category));
    }

    @Operation(summary = "Search tags by name")
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TagResponse>> searchTags(@RequestParam String query) {
        return ResponseEntity.ok(tagService.searchTags(query));
    }

    @Operation(summary = "Delete a tag")
    @DeleteMapping("/{tagId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTag(@PathVariable UUID tagId) {
        tagService.deleteTag(tagId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Entity Tagging ====================

    @Operation(summary = "Tag an entity (interview, candidate, question, etc.)")
    @PostMapping("/{tagId}/entities/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> tagEntity(
            @PathVariable UUID tagId,
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        UUID userId = securityHelper.getCurrentUserId();
        tagService.tagEntity(tagId, entityType, entityId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove a tag from an entity")
    @DeleteMapping("/{tagId}/entities/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> untagEntity(
            @PathVariable UUID tagId,
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        tagService.untagEntity(tagId, entityType, entityId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all tags for an entity")
    @GetMapping("/entities/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TagResponse>> getTagsForEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(tagService.getTagsForEntity(entityType, entityId));
    }

    @Operation(summary = "Get all entity IDs that have a specific tag")
    @GetMapping("/{tagId}/entities/{entityType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UUID>> getEntitiesByTag(
            @PathVariable UUID tagId,
            @PathVariable String entityType) {
        return ResponseEntity.ok(tagService.getEntitiesByTag(tagId, entityType));
    }
}

