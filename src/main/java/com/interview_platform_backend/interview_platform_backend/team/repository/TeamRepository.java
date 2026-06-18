package com.interview_platform_backend.interview_platform_backend.team.repository;

import com.interview_platform_backend.interview_platform_backend.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    List<Team> findByIsActiveTrue();

    List<Team> findByDepartment(String department);

    List<Team> findByManagerId(UUID managerId);

    @Query("SELECT t FROM Team t LEFT JOIN FETCH t.members m LEFT JOIN FETCH m.user LEFT JOIN FETCH t.manager WHERE t.id = :id")
    Optional<Team> findByIdWithMembers(@Param("id") UUID id);

    @Query("SELECT DISTINCT t FROM Team t JOIN t.members m WHERE m.user.id = :userId")
    List<Team> findByMemberUserId(@Param("userId") UUID userId);
}

