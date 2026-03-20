CREATE INDEX ix_tournament_sport_status
    ON tournament (sport_id, status);

CREATE INDEX ix_tournament_stage_tournament_order
    ON tournament_stage (tournament_id, sequence_order);

CREATE INDEX ix_stage_group_stage_order
    ON stage_group (stage_id, sequence_order);

CREATE INDEX ix_team_name
    ON team (name);

CREATE INDEX ix_tournament_team_tournament
    ON tournament_team (tournament_id);

CREATE INDEX ix_tournament_team_team
    ON tournament_team (team_id);

CREATE INDEX ix_team_player_roster_tournament_team_status
    ON team_player_roster (tournament_team_id, roster_status);

CREATE INDEX ix_team_player_roster_player
    ON team_player_roster (player_id);

CREATE UNIQUE INDEX uq_team_player_roster_jersey_number_active
    ON team_player_roster (tournament_team_id, jersey_number)
    WHERE jersey_number IS NOT NULL AND roster_status = 'ACTIVE';

CREATE INDEX ix_match_game_tournament_status_scheduled
    ON match_game (tournament_id, status, scheduled_at);

CREATE INDEX ix_match_game_stage_group_round
    ON match_game (stage_id, group_id, round_number, scheduled_at);

CREATE INDEX ix_match_game_home_team
    ON match_game (home_tournament_team_id);

CREATE INDEX ix_match_game_away_team
    ON match_game (away_tournament_team_id);

CREATE INDEX ix_standing_tournament_stage_group_rank
    ON standing (tournament_id, stage_id, group_id, rank_position);

CREATE INDEX ix_standing_tournament_team
    ON standing (tournament_team_id);
