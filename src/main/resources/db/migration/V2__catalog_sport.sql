CREATE TABLE sport (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    team_based BOOLEAN NOT NULL DEFAULT TRUE,
    max_players_on_field INTEGER,
    score_label VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_sport_code UNIQUE (code),
    CONSTRAINT ck_sport_max_players_on_field
        CHECK (max_players_on_field IS NULL OR max_players_on_field > 0)
);
