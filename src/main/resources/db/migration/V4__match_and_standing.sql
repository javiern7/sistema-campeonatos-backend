CREATE TABLE match_game (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    stage_id BIGINT,
    group_id BIGINT,
    round_number INTEGER,
    matchday_number INTEGER,
    home_tournament_team_id BIGINT NOT NULL,
    away_tournament_team_id BIGINT NOT NULL,
    scheduled_at TIMESTAMPTZ,
    venue_name VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    home_score INTEGER,
    away_score INTEGER,
    winner_tournament_team_id BIGINT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_match_game_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    CONSTRAINT fk_match_game_stage
        FOREIGN KEY (stage_id) REFERENCES tournament_stage (id),
    CONSTRAINT fk_match_game_group
        FOREIGN KEY (group_id) REFERENCES stage_group (id),
    CONSTRAINT fk_match_game_home_tournament_team
        FOREIGN KEY (home_tournament_team_id) REFERENCES tournament_team (id),
    CONSTRAINT fk_match_game_away_tournament_team
        FOREIGN KEY (away_tournament_team_id) REFERENCES tournament_team (id),
    CONSTRAINT fk_match_game_winner_tournament_team
        FOREIGN KEY (winner_tournament_team_id) REFERENCES tournament_team (id),
    CONSTRAINT ck_match_game_status
        CHECK (status IN ('SCHEDULED', 'PLAYED', 'FORFEIT', 'CANCELLED')),
    CONSTRAINT ck_match_game_round_number
        CHECK (round_number IS NULL OR round_number > 0),
    CONSTRAINT ck_match_game_matchday_number
        CHECK (matchday_number IS NULL OR matchday_number > 0),
    CONSTRAINT ck_match_game_teams_different
        CHECK (home_tournament_team_id <> away_tournament_team_id),
    CONSTRAINT ck_match_game_scores_non_negative
        CHECK (
            (home_score IS NULL OR home_score >= 0)
            AND (away_score IS NULL OR away_score >= 0)
        ),
    CONSTRAINT ck_match_game_scores_both_or_none
        CHECK (
            (home_score IS NULL AND away_score IS NULL)
            OR (home_score IS NOT NULL AND away_score IS NOT NULL)
        ),
    CONSTRAINT ck_match_game_winner_is_participant
        CHECK (
            winner_tournament_team_id IS NULL
            OR winner_tournament_team_id IN (home_tournament_team_id, away_tournament_team_id)
        ),
    CONSTRAINT ck_match_game_group_requires_stage
        CHECK (group_id IS NULL OR stage_id IS NOT NULL)
);

CREATE TABLE standing (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    stage_id BIGINT,
    group_id BIGINT,
    tournament_team_id BIGINT NOT NULL,
    played INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    draws INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    points_for INTEGER NOT NULL DEFAULT 0,
    points_against INTEGER NOT NULL DEFAULT 0,
    score_diff INTEGER NOT NULL DEFAULT 0,
    points INTEGER NOT NULL DEFAULT 0,
    rank_position INTEGER,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_standing_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    CONSTRAINT fk_standing_stage
        FOREIGN KEY (stage_id) REFERENCES tournament_stage (id),
    CONSTRAINT fk_standing_group
        FOREIGN KEY (group_id) REFERENCES stage_group (id),
    CONSTRAINT fk_standing_tournament_team
        FOREIGN KEY (tournament_team_id) REFERENCES tournament_team (id),
    CONSTRAINT ck_standing_group_requires_stage
        CHECK (group_id IS NULL OR stage_id IS NOT NULL),
    CONSTRAINT ck_standing_non_negative_values
        CHECK (
            played >= 0
            AND wins >= 0
            AND draws >= 0
            AND losses >= 0
            AND points_for >= 0
            AND points_against >= 0
            AND points >= 0
        ),
    CONSTRAINT ck_standing_rank_position
        CHECK (rank_position IS NULL OR rank_position > 0)
);

CREATE UNIQUE INDEX uq_standing_tournament_level
    ON standing (tournament_id, tournament_team_id)
    WHERE stage_id IS NULL AND group_id IS NULL;

CREATE UNIQUE INDEX uq_standing_stage_level
    ON standing (tournament_id, stage_id, tournament_team_id)
    WHERE stage_id IS NOT NULL AND group_id IS NULL;

CREATE UNIQUE INDEX uq_standing_group_level
    ON standing (tournament_id, stage_id, group_id, tournament_team_id)
    WHERE group_id IS NOT NULL;

CREATE TRIGGER trg_match_game_set_updated_at
BEFORE UPDATE ON match_game
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_standing_set_updated_at
BEFORE UPDATE ON standing
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
