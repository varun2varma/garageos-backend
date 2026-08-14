INSERT INTO roles (
    code,
    display_name,
    description,
    system_role,
    created_at,
    updated_at
)
VALUES (
    'DRIVER',
    'Driver',
    'Employee responsible for vehicle pickup and delivery',
    false,
    NOW(),
    NOW()
)
ON CONFLICT (code) DO NOTHING;