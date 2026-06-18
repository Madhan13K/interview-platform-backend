package com.interview_platform_backend.interview_platform_backend.user.repository;

import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    Boolean existsByName(String name);
}