-- P56 corrective migration: two semifinal slots must be allowed.
DROP INDEX uq_match_game_knockout_purpose;
CREATE UNIQUE INDEX uq_match_game_knockout_purpose_position
    ON match_game (stage_id, match_purpose, bracket_position) WHERE match_purpose IS NOT NULL AND bracket_position IS NOT NULL;
