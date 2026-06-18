package com.interview_platform_backend.interview_platform_backend.tenant.repository;

import com.interview_platform_backend.interview_platform_backend.tenant.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    List<OrganizationMember> findByOrganizationId(UUID organizationId);

    List<OrganizationMember> findByUserId(UUID userId);

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    long countByOrganizationId(UUID organizationId);
}
