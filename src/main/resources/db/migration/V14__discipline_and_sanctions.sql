CREATE TABLE disciplinary_incident (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    match_id BIGINT NOT NULL,
    tournament_team_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    incident_type VARCHAR(40) NOT NULL,
    incident_minute INTEGER,
    notes VARCHAR(500),
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_disciplinary_incident_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    CONSTRAINT fk_disciplinary_incident_match
        FOREIGN KEY (match_id) REFERENCES match_game (id),
    CONSTRAINT fk_disciplinary_incident_tournament_team
        FOREIGN KEY (tournament_team_id) REFERENCES tournament_team (id),
    CONSTRAINT fk_disciplinary_incident_player
        FOREIGN KEY (player_id) REFERENCES player (id),
    CONSTRAINT fk_disciplinary_incident_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT ck_disciplinary_incident_type
        CHECK (incident_type IN ('AMONESTACION', 'EXPULSION', 'INFORME_DISCIPLINARIO_SIMPLE')),
    CONSTRAINT ck_disciplinary_incident_minute
        CHECK (incident_minute IS NULL OR incident_minute >= 0)
);

CREATE INDEX ix_disciplinary_incident_match
    ON disciplinary_incident (match_id, created_at, id);

CREATE INDEX ix_disciplinary_incident_tournament_player
    ON disciplinary_incident (tournament_id, player_id, created_at DESC);

CREATE TABLE disciplinary_sanction (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    incident_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    tournament_team_id BIGINT NOT NULL,
    sanction_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    matches_to_serve INTEGER NOT NULL DEFAULT 0,
    notes VARCHAR(500),
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_disciplinary_sanction_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    CONSTRAINT fk_disciplinary_sanction_incident
        FOREIGN KEY (incident_id) REFERENCES disciplinary_incident (id),
    CONSTRAINT fk_disciplinary_sanction_player
        FOREIGN KEY (player_id) REFERENCES player (id),
    CONSTRAINT fk_disciplinary_sanction_tournament_team
        FOREIGN KEY (tournament_team_id) REFERENCES tournament_team (id),
    CONSTRAINT fk_disciplinary_sanction_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT uq_disciplinary_sanction_incident_type
        UNIQUE (incident_id, sanction_type),
    CONSTRAINT ck_disciplinary_sanction_type
        CHECK (sanction_type IN ('ANOTACION_DISCIPLINARIA', 'SUSPENSION_PROXIMO_PARTIDO')),
    CONSTRAINT ck_disciplinary_sanction_status
        CHECK (status IN ('ACTIVE', 'SERVED')),
    CONSTRAINT ck_disciplinary_sanction_matches_to_serve
        CHECK (matches_to_serve >= 0)
);

CREATE INDEX ix_disciplinary_sanction_tournament
    ON disciplinary_sanction (tournament_id, created_at DESC, id DESC);

CREATE INDEX ix_disciplinary_sanction_incident
    ON disciplinary_sanction (incident_id, created_at, id);
