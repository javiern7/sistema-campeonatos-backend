INSERT INTO app_permission (code, name, description)
VALUES ('tournaments:publication:manage', 'Tournament publication manage', 'Permite publicar y despublicar el enlace no listado P56')
ON CONFLICT (code) DO NOTHING;

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r JOIN app_permission p ON p.code = 'tournaments:publication:manage'
WHERE r.code IN ('SUPER_ADMIN', 'TOURNAMENT_ADMIN')
ON CONFLICT DO NOTHING;
