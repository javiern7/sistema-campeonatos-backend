CREATE TABLE app_basic_configuration (
    id BIGINT PRIMARY KEY,
    organization_name VARCHAR(120) NOT NULL,
    support_email VARCHAR(150) NOT NULL,
    default_timezone VARCHAR(60) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_app_basic_configuration_single_row CHECK (id = 1)
);

INSERT INTO app_basic_configuration (id, organization_name, support_email, default_timezone)
VALUES (1, 'Sistema Campeonatos', 'operaciones@local.test', 'America/Lima');

INSERT INTO app_permission (code, name, description)
VALUES
    ('users:read', 'Users read', 'Permite consultar usuarios operativos existentes'),
    ('users:manage', 'Users manage', 'Permite actualizar el estado operativo de usuarios existentes'),
    ('configuration:basic:read', 'Basic configuration read', 'Permite consultar configuracion operativa basica'),
    ('configuration:basic:manage', 'Basic configuration manage', 'Permite actualizar configuracion operativa basica');

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.code IN (
    'users:read',
    'users:manage',
    'configuration:basic:read',
    'configuration:basic:manage'
)
WHERE r.code = 'SUPER_ADMIN';

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.code IN (
    'users:read',
    'configuration:basic:read',
    'configuration:basic:manage'
)
WHERE r.code = 'TOURNAMENT_ADMIN';
