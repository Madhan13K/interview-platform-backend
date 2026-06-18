package com.interview_platform_backend.interview_platform_backend.user.repository;

import com.interview_platform_backend.interview_platform_backend.user.entity.Permission;
import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import com.interview_platform_backend.interview_platform_backend.user.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findByRole(Role role);

    @Query("select rp from RolePermission rp join fetch rp.permission where rp.role in :roles")
    List<RolePermission> findByRoleInWithPermissions(@Param("roles") List<Role> roles);

    List<RolePermission> findByRoleIn(List<Role> roles);

    List<RolePermission> findByPermission(Permission permission);
    boolean existsByRoleAndPermission(
            Role role,
            Permission permission);
}