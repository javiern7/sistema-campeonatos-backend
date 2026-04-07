CREATE TABLE app_permission (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_permission_code UNIQUE (code)
);

CREATE TABLE app_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_role_permission PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_app_role_permission_role
        FOREIGN KEY (role_id) REFERENCES app_role (id),
    CONSTRAINT fk_app_role_permission_permission
        FOREIGN KEY (permission_id) REFERENCES app_permission (id)
);

CREATE INDEX ix_app_role_permission_permission_id
    ON app_role_permission (permission_id);

INSERT INTO app_permission (code, name, description)
VALUES
    ('dashboard:read', 'Dashboard read', 'Permite consultar el dashboard principal'),
    ('sports:read', 'Sports read', 'Permite consultar deportes'),
    ('teams:read', 'Teams read', 'Permite consultar equipos'),
    ('players:read', 'Players read', 'Permite consultar jugadores'),
    ('tournaments:read', 'Tournaments read', 'Permite consultar torneos'),
    ('tournamentTeams:read', 'Tournament teams read', 'Permite consultar equipos de torneo'),
    ('tournamentStages:read', 'Tournament stages read', 'Permite consultar fases de torneo'),
    ('stageGroups:read', 'Stage groups read', 'Permite consultar grupos de fase'),
    ('rosters:read', 'Rosters read', 'Permite consultar plantillas'),
    ('matches:read', 'Matches read', 'Permite consultar partidos'),
    ('standings:read', 'Standings read', 'Permite consultar tablas de posiciones'),
    ('auth:session:read', 'Auth session read', 'Permite consultar la sesion autenticada'),
    ('operations:audit:read', 'Operational audit read', 'Permite consultar auditoria operativa'),
    ('teams:manage', 'Teams manage', 'Permite crear y actualizar equipos'),
    ('players:manage', 'Players manage', 'Permite crear y actualizar jugadores'),
    ('tournaments:manage', 'Tournaments manage', 'Permite crear y actualizar torneos'),
    ('tournamentTeams:manage', 'Tournament teams manage', 'Permite crear y actualizar equipos de torneo'),
    ('tournamentStages:manage', 'Tournament stages manage', 'Permite crear y actualizar fases de torneo'),
    ('stageGroups:manage', 'Stage groups manage', 'Permite crear y actualizar grupos de fase'),
    ('rosters:manage', 'Rosters manage', 'Permite crear y actualizar plantillas'),
    ('matches:manage', 'Matches manage', 'Permite crear y actualizar partidos'),
    ('standings:manage', 'Standings manage', 'Permite crear y actualizar tablas de posiciones'),
    ('teams:delete', 'Teams delete', 'Permite eliminar equipos'),
    ('players:delete', 'Players delete', 'Permite eliminar jugadores'),
    ('tournaments:delete', 'Tournaments delete', 'Permite eliminar torneos'),
    ('tournamentTeams:delete', 'Tournament teams delete', 'Permite eliminar equipos de torneo'),
    ('tournamentStages:delete', 'Tournament stages delete', 'Permite eliminar fases de torneo'),
    ('stageGroups:delete', 'Stage groups delete', 'Permite eliminar grupos de fase'),
    ('rosters:delete', 'Rosters delete', 'Permite eliminar plantillas'),
    ('matches:delete', 'Matches delete', 'Permite eliminar partidos'),
    ('standings:delete', 'Standings delete', 'Permite eliminar tablas de posiciones'),
    ('tournaments:status-transition', 'Tournaments status transition', 'Permite cambiar el estado de un torneo'),
    ('tournaments:progress-to-knockout', 'Tournaments progress to knockout', 'Permite avanzar un torneo a eliminacion'),
    ('tournaments:generate-knockout-bracket', 'Tournaments generate knockout bracket', 'Permite generar el bracket eliminatorio'),
    ('standings:recalculate', 'Standings recalculate', 'Permite recalcular tablas de posiciones');

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.code IN (
    'dashboard:read',
    'sports:read',
    'teams:read',
    'players:read',
    'tournaments:read',
    'tournamentTeams:read',
    'tournamentStages:read',
    'stageGroups:read',
    'rosters:read',
    'matches:read',
    'standings:read',
    'auth:session:read',
    'operations:audit:read',
    'teams:manage',
    'players:manage',
    'tournaments:manage',
    'tournamentTeams:manage',
    'tournamentStages:manage',
    'stageGroups:manage',
    'rosters:manage',
    'matches:manage',
    'standings:manage',
    'teams:delete',
    'players:delete',
    'tournaments:delete',
    'tournamentTeams:delete',
    'tournamentStages:delete',
    'stageGroups:delete',
    'rosters:delete',
    'matches:delete',
    'standings:delete',
    'tournaments:status-transition',
    'tournaments:progress-to-knockout',
    'tournaments:generate-knockout-bracket',
    'standings:recalculate'
)
WHERE r.code = 'SUPER_ADMIN';

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.code IN (
    'dashboard:read',
    'sports:read',
    'teams:read',
    'players:read',
    'tournaments:read',
    'tournamentTeams:read',
    'tournamentStages:read',
    'stageGroups:read',
    'rosters:read',
    'matches:read',
    'standings:read',
    'auth:session:read',
    'operations:audit:read',
    'teams:manage',
    'players:manage',
    'tournaments:manage',
    'tournamentTeams:manage',
    'tournamentStages:manage',
    'stageGroups:manage',
    'rosters:manage',
    'matches:manage',
    'standings:manage',
    'tournaments:status-transition',
    'tournaments:progress-to-knockout',
    'tournaments:generate-knockout-bracket',
    'standings:recalculate'
)
WHERE r.code = 'TOURNAMENT_ADMIN';

INSERT INTO app_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN app_permission p ON p.code IN (
    'dashboard:read',
    'sports:read',
    'teams:read',
    'players:read',
    'tournaments:read',
    'tournamentTeams:read',
    'tournamentStages:read',
    'stageGroups:read',
    'rosters:read',
    'matches:read',
    'standings:read',
    'auth:session:read',
    'rosters:manage',
    'matches:manage',
    'standings:recalculate'
)
WHERE r.code = 'OPERATOR';
