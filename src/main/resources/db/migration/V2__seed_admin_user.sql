INSERT INTO users (id, name, email, password_hash, is_enabled, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Admin User',
    'admin@smartauth.com',
    -- Password: AdminPass123! (BCrypt hash with cost 12)
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5F0F0MqxV3qnm',
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT 'a0000000-0000-0000-0000-000000000001', id
FROM roles
WHERE name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
