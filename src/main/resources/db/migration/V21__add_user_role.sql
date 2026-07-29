INSERT INTO roles
(
    code,
    display_name,
    description,
    system_role,
    created_at,
    updated_at
)
VALUES
(
    'USER',
    'User',
    'Registered user awaiting onboarding',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (code) DO NOTHING;