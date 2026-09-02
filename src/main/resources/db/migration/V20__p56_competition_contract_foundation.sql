-- P56: additive foundation. Existing tournaments remain valid until explicitly configured for the pilot.
ALTER TABLE tournament_team ADD COLUMN group_id BIGINT;
ALTER TABLE tournament_team ADD CONSTRAINT fk_tournament_team_group FOREIGN KEY (group_id) REFERENCES stage_group (id);
CREATE UNIQUE INDEX uq_tournament_team_group_draw_position ON tournament_team (group_id, group_draw_position) WHERE group_id IS NOT NULL AND group_draw_position IS NOT NULL;
CREATE INDEX ix_tournament_team_group ON tournament_team (group_id);

ALTER TABLE match_game ADD COLUMN home_penalty_score INTEGER;
ALTER TABLE match_game ADD COLUMN away_penalty_score INTEGER;
ALTER TABLE match_game ADD COLUMN resolution_method VARCHAR(20);
ALTER TABLE match_game ADD COLUMN match_purpose VARCHAR(20);
ALTER TABLE match_game ADD COLUMN bracket_position INTEGER;
ALTER TABLE match_game ADD COLUMN home_source_match_id BIGINT;
ALTER TABLE match_game ADD COLUMN home_source_outcome VARCHAR(10);
ALTER TABLE match_game ADD COLUMN away_source_match_id BIGINT;
ALTER TABLE match_game ADD COLUMN away_source_outcome VARCHAR(10);
ALTER TABLE match_game ADD CONSTRAINT fk_match_game_home_source FOREIGN KEY (home_source_match_id) REFERENCES match_game (id);
ALTER TABLE match_game ADD CONSTRAINT fk_match_game_away_source FOREIGN KEY (away_source_match_id) REFERENCES match_game (id);
ALTER TABLE match_game ADD CONSTRAINT ck_match_game_penalties_non_negative CHECK ((home_penalty_score IS NULL OR home_penalty_score >= 0) AND (away_penalty_score IS NULL OR away_penalty_score >= 0));
ALTER TABLE match_game ADD CONSTRAINT ck_match_game_penalties_both_or_none CHECK ((home_penalty_score IS NULL AND away_penalty_score IS NULL) OR (home_penalty_score IS NOT NULL AND away_penalty_score IS NOT NULL));
ALTER TABLE match_game ADD CONSTRAINT ck_match_game_resolution_method CHECK (resolution_method IS NULL OR resolution_method IN ('REGULATION', 'PENALTIES'));
ALTER TABLE match_game ADD CONSTRAINT ck_match_game_purpose CHECK (match_purpose IS NULL OR match_purpose IN ('SEMIFINAL', 'FINAL', 'THIRD_PLACE'));
ALTER TABLE match_game ADD CONSTRAINT ck_match_game_source_outcome CHECK ((home_source_outcome IS NULL OR home_source_outcome IN ('WINNER', 'LOSER')) AND (away_source_outcome IS NULL OR away_source_outcome IN ('WINNER', 'LOSER')));
CREATE UNIQUE INDEX uq_match_game_group_normalized_pair ON match_game (group_id, LEAST(home_tournament_team_id, away_tournament_team_id), GREATEST(home_tournament_team_id, away_tournament_team_id)) WHERE group_id IS NOT NULL;
CREATE UNIQUE INDEX uq_match_game_knockout_purpose ON match_game (stage_id, match_purpose) WHERE match_purpose IS NOT NULL;
CREATE UNIQUE INDEX uq_match_game_knockout_position ON match_game (stage_id, bracket_position) WHERE bracket_position IS NOT NULL;
CREATE INDEX ix_match_game_sources ON match_game (home_source_match_id, away_source_match_id);

ALTER TABLE standing ADD COLUMN decisive_criterion VARCHAR(40);
ALTER TABLE standing ADD COLUMN tie_break_draw_id BIGINT;
CREATE TABLE standing_tie_break_draw (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournament (id),
    stage_id BIGINT NOT NULL REFERENCES tournament_stage (id),
    group_id BIGINT NOT NULL REFERENCES stage_group (id),
    affected_team_ids JSONB NOT NULL,
    resulting_order_team_ids JSONB NOT NULL,
    reason TEXT NOT NULL,
    recorded_by_user_id BIGINT NOT NULL REFERENCES app_user (id),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_standing_tie_break_draw_teams_not_empty CHECK (jsonb_array_length(affected_team_ids) >= 2),
    CONSTRAINT ck_standing_tie_break_draw_order_not_empty CHECK (jsonb_array_length(resulting_order_team_ids) >= 2)
);
ALTER TABLE standing ADD CONSTRAINT fk_standing_tie_break_draw FOREIGN KEY (tie_break_draw_id) REFERENCES standing_tie_break_draw (id);
CREATE INDEX ix_standing_tie_break_draw_scope ON standing_tie_break_draw (tournament_id, stage_id, group_id, recorded_at DESC);

CREATE TABLE tournament_publication (
    tournament_id BIGINT PRIMARY KEY REFERENCES tournament (id),
    publication_status VARCHAR(20) NOT NULL DEFAULT 'UNPUBLISHED',
    access_mode VARCHAR(20) NOT NULL DEFAULT 'UNLISTED',
    published_at TIMESTAMPTZ,
    unpublished_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by_user_id BIGINT REFERENCES app_user (id),
    CONSTRAINT ck_tournament_publication_status CHECK (publication_status IN ('UNPUBLISHED', 'PUBLISHED')),
    CONSTRAINT ck_tournament_publication_access_mode CHECK (access_mode = 'UNLISTED')
);
CREATE INDEX ix_tournament_publication_status ON tournament_publication (publication_status, access_mode);
