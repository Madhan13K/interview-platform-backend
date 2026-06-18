package com.interview_platform_backend.interview_platform_backend.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;


//Defines granular access controls.
//
//CREATE_INTERVIEW
//JOIN_SESSION
//VIEW_FEEDBACK
//MANAGE_USERS
@Entity
@Table(name = "permissions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RolePermission> rolePermissions;


}
