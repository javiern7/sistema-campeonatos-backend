CREATE TABLE tournament (
    id BIGSERIAL PRIMARY KEY,
    sport_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(160) NOT NULL,
    season_name VARCHAR(50) NOT NULL,
    format VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    description TEXT,
    start_date DATE,
    end_date DATE,
    registration_open_at TIMESTAMPTZ,
    registration_close_at TIMESTAMPTZ,
    max_teams INTEGER,
    points_win INTEGER NOT NULL DEFAULT 3,
    points_draw INTEGER NOT NULL DEFAULT 1,
    points_loss INTEGER NOT NULL DEFAULT 0,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tournament_slug UNIQUE (slug),
    CONSTRAINT fk_tournament_sport
        FOREIGN KEY (sport_id) REFERENCES sport (id),
    CONSTRAINT fk_tournament_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT ck_tournament_format
        CHECK (format IN ('LEAGUE', 'GROUPS_THEN_KNOCKOUT', 'KNOCKOUT')),
    CONSTRAINT ck_tournament_status
        CHECK (status IN ('DRAFT', 'OPEN', 'IN_PROGRESS', 'FINISHED', 'CANCELLED')),
    CONSTRAINT ck_tournament_date_range
        CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date),
    CONSTRAINT ck_tournament_registration_range
        CHECK (
            registration_open_at IS NULL
            OR registration_close_at IS NULL
            OR registration_open_at <= registration_close_at
        ),
    CONSTRAINT ck_tournament_max_teams
        CHECK (max_teams IS NULL OR max_teams > 1),
    CONSTRAINT ck_tournament_points
        CHECK (
            points_win >= points_draw
            AND points_draw >= points_loss
            AND points_loss >= 0
        )
);

CREATE TABLE tournament_stage (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    stage_type VARCHAR(30) NOT NULL,
    sequence_order INTEGER NOT NULL,
    legs INTEGER NOT NULL DEFAULT 1,
    round_trip BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_tournament_stage_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    CONSTRAINT uq_tournament_stage_order UNIQUE (tournament_id, sequence_order),
    CONSTRAINT ck_tournament_stage_type
        CHECK (stage_type IN ('LEAGUE', 'GROUP_STAGE', 'KNOCKOUT')),
    CONSTRAINT ck_tournament_stage_sequence_order
        CHECK (sequence_order > 0),
    CONSTRAINT ck_tournament_stage_legs
        CHECK (legs > 0)
);

CREATE TABLE stage_group (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(50) NOT NULL,
    sequence_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_stage_group_stage
        FOREIGN KEY (stage_id) REFERENCES tournament_stage (id),
    CONSTRAINT uq_stage_group_code UNIQUE (stage_id, code),
    CONSTRAINT uq_stage_group_sequence UNIQUE (stage_id, sequence_order),
    CONSTRAINT ck_stage_group_sequence_order
        CHECK (sequence_order > 0)
);

CREATE TABLE team (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    short_name VARCHAR(50),
    code VARCHAR(30),
    primary_color VARCHAR(20),
    secondary_color VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_team_code UNIQUE (code)
);

CREATE TABLE player (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_type VARCHAR(20),
    document_number VARCHAR(30),
    birth_date DATE,
    email VARCHAR(150),
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_player_birth_date
        CHECK (birth_date IS NULL OR birth_date <= CURRENT_DATE)
);

CREATE TABLE tournament_team (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    registration_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    seed_number INTEGER,
    group_draw_position INTEGER,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_tournament_team_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    CONSTRAINT fk_tournament_team_team
        FOREIGN KEY (team_id) REFERENCES team (id),
    CONSTRAINT uq_tournament_team UNIQUE (tournament_id, team_id),
    CONSTRAINT ck_tournament_team_registration_status
        CHECK (registration_status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT ck_tournament_team_seed_number
        CHECK (seed_number IS NULL OR seed_number > 0),
    CONSTRAINT ck_tournament_team_group_draw_position
        CHECK (group_draw_position IS NULL OR group_draw_position > 0)
);

CREATE UNIQUE INDEX uq_tournament_team_seed_number
    ON tournament_team (tournament_id, seed_number)
    WHERE seed_number IS NOT NULL;

CREATE TABLE team_player_roster (
    id BIGSERIAL PRIMARY KEY,
    tournament_team_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    jersey_number INTEGER,
    captain BOOLEAN NOT NULL DEFAULT FALSE,
    position_name VARCHAR(50),
    roster_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_team_player_roster_tournament_team
        FOREIGN KEY (tournament_team_id) REFERENCES tournament_team (id),
    CONSTRAINT fk_team_player_roster_player
        FOREIGN KEY (player_id) REFERENCES player (id),
    CONSTRAINT uq_team_player_roster_history
        UNIQUE (tournament_team_id, player_id, start_date),
    CONSTRAINT ck_team_player_roster_jersey_number
        CHECK (jersey_number IS NULL OR jersey_number BETWEEN 0 AND 99),
    CONSTRAINT ck_team_player_roster_status
        CHECK (roster_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    CONSTRAINT ck_team_player_roster_date_range
        CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE UNIQUE INDEX uq_player_document
    ON player (document_type, document_number)
    WHERE document_type IS NOT NULL AND document_number IS NOT NULL;

CREATE INDEX ix_player_last_name_first_name
    ON player (last_name, first_name);

CREATE TRIGGER trg_tournament_set_updated_at
BEFORE UPDATE ON tournament
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_team_set_updated_at
BEFORE UPDATE ON team
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_player_set_updated_at
BEFORE UPDATE ON player
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
