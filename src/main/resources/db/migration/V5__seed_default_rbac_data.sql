-- Ensure pgcrypto is available (needed for gen_random_uuid and crypt).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ─────────────────────────────────────────────────────────────
-- ROLES
-- ─────────────────────────────────────────────────────────────
INSERT INTO roles (id, name, description, created_at)
SELECT gen_random_uuid(), 'ADMIN', 'Platform administrator with full access', NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

INSERT INTO roles (id, name, description, created_at)
SELECT gen_random_uuid(), 'RECRUITER', 'Recruiter who can manage interview schedules', NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'RECRUITER');

INSERT INTO roles (id, name, description, created_at)
SELECT gen_random_uuid(), 'INTERVIEWER', 'Interviewer who can view interviews and submit feedback', NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'INTERVIEWER');

INSERT INTO roles (id, name, description, created_at)
SELECT gen_random_uuid(), 'CANDIDATE', 'Candidate participating in interview process', NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'CANDIDATE');

-- ─────────────────────────────────────────────────────────────
-- PERMISSIONS
-- ─────────────────────────────────────────────────────────────
INSERT INTO permissions (id, name, description)
SELECT gen_random_uuid(), 'INTERVIEW_CREATE', 'Create new interview records'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'INTERVIEW_CREATE');

INSERT INTO permissions (id, name, description)
SELECT gen_random_uuid(), 'INTERVIEW_VIEW', 'View interview records'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'INTERVIEW_VIEW');

INSERT INTO permissions (id, name, description)
SELECT gen_random_uuid(), 'VIEW_FEEDBACK', 'View interview feedback'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'VIEW_FEEDBACK');

INSERT INTO permissions (id, name, description)
SELECT gen_random_uuid(), 'JOIN_SESSION', 'Join interview session'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'JOIN_SESSION');

INSERT INTO permissions (id, name, description)
SELECT gen_random_uuid(), 'MANAGE_USERS', 'Create and manage users'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MANAGE_USERS');

INSERT INTO permissions (id, name, description)
SELECT gen_random_uuid(), 'MANAGE_ROLES', 'Create and manage roles'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MANAGE_ROLES');

INSERT INTO permissions (id, name, description)
SELECT gen_random_uuid(), 'MANAGE_PERMISSIONS', 'Create and manage permissions'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MANAGE_PERMISSIONS');

-- ─────────────────────────────────────────────────────────────
-- ROLE ↔ PERMISSION MAPPINGS
-- ─────────────────────────────────────────────────────────────
INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'INTERVIEW_CREATE' WHERE r.name = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'INTERVIEW_VIEW' WHERE r.name = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'VIEW_FEEDBACK' WHERE r.name = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'JOIN_SESSION' WHERE r.name = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'MANAGE_USERS' WHERE r.name = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'MANAGE_ROLES' WHERE r.name = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'MANAGE_PERMISSIONS' WHERE r.name = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'INTERVIEW_CREATE' WHERE r.name = 'RECRUITER'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'INTERVIEW_VIEW' WHERE r.name = 'RECRUITER'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'VIEW_FEEDBACK' WHERE r.name = 'RECRUITER'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'JOIN_SESSION' WHERE r.name = 'RECRUITER'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'MANAGE_USERS' WHERE r.name = 'RECRUITER'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'INTERVIEW_VIEW' WHERE r.name = 'INTERVIEWER'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'VIEW_FEEDBACK' WHERE r.name = 'INTERVIEWER'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'JOIN_SESSION' WHERE r.name = 'INTERVIEWER'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'INTERVIEW_VIEW' WHERE r.name = 'CANDIDATE'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'VIEW_FEEDBACK' WHERE r.name = 'CANDIDATE'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM roles r JOIN permissions p ON p.name = 'JOIN_SESSION' WHERE r.name = 'CANDIDATE'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ─────────────────────────────────────────────────────────────
-- DEFAULT USERS  (password: ChangeMe123!)
-- ─────────────────────────────────────────────────────────────
INSERT INTO users (id, first_name, last_name, email, password, status, auth_provider, created_at)
SELECT gen_random_uuid(), 'System', 'Admin', 'admin@interview.local',
       crypt('ChangeMe123!', gen_salt('bf')), 'ACTIVE', 'LOCAL', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@interview.local');

INSERT INTO users (id, first_name, last_name, email, password, status, auth_provider, created_at)
SELECT gen_random_uuid(), 'Ria', 'Recruiter', 'recruiter@interview.local',
       crypt('ChangeMe123!', gen_salt('bf')), 'ACTIVE', 'LOCAL', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'recruiter@interview.local');

INSERT INTO users (id, first_name, last_name, email, password, status, auth_provider, created_at)
SELECT gen_random_uuid(), 'Ian', 'Interviewer', 'interviewer@interview.local',
       crypt('ChangeMe123!', gen_salt('bf')), 'ACTIVE', 'LOCAL', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'interviewer@interview.local');

INSERT INTO users (id, first_name, last_name, email, password, status, auth_provider, created_at)
SELECT gen_random_uuid(), 'Cathy', 'Candidate', 'candidate@interview.local',
       crypt('ChangeMe123!', gen_salt('bf')), 'ACTIVE', 'LOCAL', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'candidate@interview.local');

-- ─────────────────────────────────────────────────────────────
-- USER ↔ ROLE MAPPINGS
-- ─────────────────────────────────────────────────────────────
INSERT INTO user_roles (id, user_id, role_id, assigned_at)
SELECT gen_random_uuid(), u.id, r.id, NOW()
FROM users u JOIN roles r ON r.name = 'ADMIN' WHERE u.email = 'admin@interview.local'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

INSERT INTO user_roles (id, user_id, role_id, assigned_at)
SELECT gen_random_uuid(), u.id, r.id, NOW()
FROM users u JOIN roles r ON r.name = 'RECRUITER' WHERE u.email = 'recruiter@interview.local'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

INSERT INTO user_roles (id, user_id, role_id, assigned_at)
SELECT gen_random_uuid(), u.id, r.id, NOW()
FROM users u JOIN roles r ON r.name = 'INTERVIEWER' WHERE u.email = 'interviewer@interview.local'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

INSERT INTO user_roles (id, user_id, role_id, assigned_at)
SELECT gen_random_uuid(), u.id, r.id, NOW()
FROM users u JOIN roles r ON r.name = 'CANDIDATE' WHERE u.email = 'candidate@interview.local'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

