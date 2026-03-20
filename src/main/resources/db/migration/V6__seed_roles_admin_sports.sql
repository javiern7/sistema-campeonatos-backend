INSERT INTO app_role (code, name, description)
VALUES
    ('SUPER_ADMIN', 'Super Admin', 'Acceso total a la plataforma'),
    ('TOURNAMENT_ADMIN', 'Tournament Admin', 'Gestiona torneos y configuraciones'),
    ('OPERATOR', 'Operator', 'Opera resultados, partidos y rosters');

INSERT INTO app_user (
    username,
    email,
    password_hash,
    first_name,
    last_name,
    status
)
VALUES (
    'admin',
    'admin@local.test',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO.Hj3J0Q6vY6gG0K9Q2cF6lM0m1aBcDe',
    'System',
    'Admin',
    'ACTIVE'
);

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN app_role r ON r.code = 'SUPER_ADMIN'
WHERE u.username = 'admin';

INSERT INTO sport (code, name, team_based, max_players_on_field, score_label, active)
VALUES
    ('FOOTBALL', 'Football', TRUE, 11, 'GOALS', TRUE),
    ('FUTSAL', 'Futsal', TRUE, 5, 'GOALS', TRUE),
    ('BASKETBALL', 'Basketball', TRUE, 5, 'POINTS', TRUE),
    ('VOLLEYBALL', 'Volleyball', TRUE, 6, 'POINTS', TRUE);
