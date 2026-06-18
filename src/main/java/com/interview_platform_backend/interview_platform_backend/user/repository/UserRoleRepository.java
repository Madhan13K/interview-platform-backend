package com.interview_platform_backend.interview_platform_backend.user.repository;
import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUser(User user);

    List<UserRole> findByRole(Role role);

    Optional<UserRole> findByUserAndRole(User user, Role role);

    void deleteByUser(User user);
}
