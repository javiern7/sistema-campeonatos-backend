INSERT INTO app_permission (code, name, description)
VALUES (
    'permissions:govern:manage',
    'Permission governance manage',
    'Permite administrar asignaciones operativas rol-permiso'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.code = 'permissions:govern:manage'
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
