CREATE TABLE match_event (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL,
    tournament_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tournament_team_id BIGINT,
    player_id BIGINT,
    related_player_id BIGINT,
    period_label VARCHAR(40),
    event_minute INTEGER,
    event_second INTEGER,
    event_value INTEGER,
    notes VARCHAR(500),
    created_by_user_id BIGINT NOT NULL,
    annulled_by_user_id BIGINT,
    annulled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_match_event_match
        FOREIGN KEY (match_id) REFERENCES match_game (id),
    CONSTRAINT fk_match_event_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    CONSTRAINT fk_match_event_tournament_team
        FOREIGN KEY (tournament_team_id) REFERENCES tournament_team (id),
    CONSTRAINT fk_match_event_player
        FOREIGN KEY (player_id) REFERENCES player (id),
    CONSTRAINT fk_match_event_related_player
        FOREIGN KEY (related_player_id) REFERENCES player (id),
    CONSTRAINT fk_match_event_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_match_event_annulled_by_user
        FOREIGN KEY (annulled_by_user_id) REFERENCES app_user (id),
    CONSTRAINT ck_match_event_type
        CHECK (event_type IN ('SCORE', 'YELLOW_CARD', 'RED_CARD', 'SUBSTITUTION', 'INCIDENT', 'NOTE')),
    CONSTRAINT ck_match_event_status
        CHECK (status IN ('ACTIVE', 'ANNULLED')),
    CONSTRAINT ck_match_event_minute
        CHECK (event_minute IS NULL OR event_minute >= 0),
    CONSTRAINT ck_match_event_second
        CHECK (event_second IS NULL OR event_second BETWEEN 0 AND 59),
    CONSTRAINT ck_match_event_value
        CHECK (event_value IS NULL OR event_value > 0),
    CONSTRAINT ck_match_event_substitution_players
        CHECK (related_player_id IS NULL OR player_id IS NULL OR related_player_id <> player_id)
);

CREATE INDEX ix_match_event_match
    ON match_event (match_id, event_minute, event_second, created_at, id);

CREATE INDEX ix_match_event_tournament
    ON match_event (tournament_id, created_at DESC, id DESC);

CREATE TRIGGER trg_match_event_set_updated_at
BEFORE UPDATE ON match_event
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
