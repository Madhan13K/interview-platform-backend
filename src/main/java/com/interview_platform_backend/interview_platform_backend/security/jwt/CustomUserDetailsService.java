package com.interview_platform_backend.interview_platform_backend.security.jwt;

import com.interview_platform_backend.interview_platform_backend.user.entity.Role;
import com.interview_platform_backend.interview_platform_backend.user.entity.RolePermission;
import com.interview_platform_backend.interview_platform_backend.user.entity.User;
import com.interview_platform_backend.interview_platform_backend.user.entity.UserRole;
import com.interview_platform_backend.interview_platform_backend.user.repository.RolePermissionRepository;
import com.interview_platform_backend.interview_platform_backend.user.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public CustomUserDetailsService(UserRepository userRepository, RolePermissionRepository rolePermissionRepository) {
        this.userRepository = userRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("NullableProblems")
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Query 1: Fetch user with roles (no bag conflict)
        User user = userRepository.findByEmailWithAuthorities(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        Set<GrantedAuthority> authorities = new HashSet<>();

        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            List<Role> roles = user.getUserRoles().stream()
                    .filter(ur -> ur != null && ur.getRole() != null)
                    .map(UserRole::getRole)
                    .collect(Collectors.toList());

            // Add ROLE_ authorities
            for (Role role : roles) {
                if (role.getName() != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                }
            }

            // Query 2: Fetch permissions for these roles (separate query, no bag conflict)
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleInWithPermissions(roles);
            for (RolePermission rp : rolePermissions) {
                if (rp.getPermission() != null && rp.getPermission().getName() != null) {
                    authorities.add(new SimpleGrantedAuthority(rp.getPermission().getName()));
                }
            }
        }

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .authorities(authorities)
                .build();
    }
}