-- Verifies that baseline RBAC seed data exists after Flyway migrations.
-- Usage: psql "$DATABASE_URL" -f scripts/verify_rbac_seed.sql

WITH checks AS (
    SELECT
        'roles_present' AS check_name,
        CASE
            WHEN COUNT(*) FILTER (WHERE name IN ('ADMIN', 'RECRUITER', 'INTERVIEWER', 'CANDIDATE')) = 4
            THEN 'PASS' ELSE 'FAIL'
        END AS status,
        COUNT(*) FILTER (WHERE name IN ('ADMIN', 'RECRUITER', 'INTERVIEWER', 'CANDIDATE'))::text || '/4 required roles found' AS details
    FROM roles

    UNION ALL

    SELECT
        'core_permissions_present',
        CASE
            WHEN COUNT(*) FILTER (WHERE name IN ('INTERVIEW_CREATE', 'INTERVIEW_VIEW', 'VIEW_FEEDBACK', 'JOIN_SESSION', 'MANAGE_USERS', 'MANAGE_ROLES', 'MANAGE_PERMISSIONS')) = 7
            THEN 'PASS' ELSE 'FAIL'
        END,
        COUNT(*) FILTER (WHERE name IN ('INTERVIEW_CREATE', 'INTERVIEW_VIEW', 'VIEW_FEEDBACK', 'JOIN_SESSION', 'MANAGE_USERS', 'MANAGE_ROLES', 'MANAGE_PERMISSIONS'))::text || '/7 core permissions found'
    FROM permissions

    UNION ALL

    SELECT
        'default_users_present',
        CASE
            WHEN COUNT(*) FILTER (WHERE email IN ('admin@interview.local', 'recruiter@interview.local', 'interviewer@interview.local', 'candidate@interview.local')) = 4
            THEN 'PASS' ELSE 'FAIL'
        END,
        COUNT(*) FILTER (WHERE email IN ('admin@interview.local', 'recruiter@interview.local', 'interviewer@interview.local', 'candidate@interview.local'))::text || '/4 default users found'
    FROM users

    UNION ALL

    SELECT
        'default_user_role_links',
        CASE WHEN COUNT(*) = 4 THEN 'PASS' ELSE 'FAIL' END,
        COUNT(*)::text || '/4 expected user-role links found'
    FROM (
        SELECT u.email, r.name
        FROM user_roles ur
        JOIN users u ON u.id = ur.user_id
        JOIN roles r ON r.id = ur.role_id
        WHERE (u.email, r.name) IN (
            ('admin@interview.local', 'ADMIN'),
            ('recruiter@interview.local', 'RECRUITER'),
            ('interviewer@interview.local', 'INTERVIEWER'),
            ('candidate@interview.local', 'CANDIDATE')
        )
    ) matched
)
SELECT *
FROM checks
ORDER BY check_name;

