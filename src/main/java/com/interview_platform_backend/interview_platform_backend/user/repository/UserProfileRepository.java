package com.interview_platform_backend.interview_platform_backend.user.repository;

import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUser(User user);

    Optional<UserProfile> findByUserId(UUID userId);

    @Query(value ="Select * from user_profiles up where up.profile = :profile", nativeQuery = true)
    Optional<UserProfile> findByProfile(@Param("profile") UUID profile);

    @Query("SELECT up FROM UserProfile up JOIN FETCH up.user WHERE up.user.id = :userId")
    Optional<UserProfile> findByUserIdWithUser(@Param("userId") UUID userId);

}