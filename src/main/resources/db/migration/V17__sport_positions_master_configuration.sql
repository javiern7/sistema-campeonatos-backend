CREATE TABLE sport_position (
    id BIGSERIAL PRIMARY KEY,
    sport_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(80) NOT NULL,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_sport_position_sport
        FOREIGN KEY (sport_id) REFERENCES sport (id),
    CONSTRAINT uq_sport_position_code UNIQUE (sport_id, code),
    CONSTRAINT uq_sport_position_order UNIQUE (sport_id, display_order),
    CONSTRAINT ck_sport_position_display_order
        CHECK (display_order > 0)
);

CREATE INDEX ix_sport_position_sport_active
    ON sport_position (sport_id, active, display_order);

INSERT INTO sport_position (sport_id, code, name, display_order, active)
SELECT s.id, p.code, p.name, p.display_order, TRUE
FROM sport s
JOIN (
    VALUES
        ('FOOTBALL', 'GK', 'Arquero', 1),
        ('FOOTBALL', 'DF', 'Defensa', 2),
        ('FOOTBALL', 'MF', 'Mediocampista', 3),
        ('FOOTBALL', 'FW', 'Delantero', 4),
        ('FUTSAL', 'GK', 'Arquero', 1),
        ('FUTSAL', 'FIXO', 'Fijo', 2),
        ('FUTSAL', 'ALA', 'Ala', 3),
        ('FUTSAL', 'PIVOT', 'Pivot', 4),
        ('BASKETBALL', 'PG', 'Base', 1),
        ('BASKETBALL', 'SG', 'Escolta', 2),
        ('BASKETBALL', 'SF', 'Alero', 3),
        ('BASKETBALL', 'PF', 'Ala pivot', 4),
        ('BASKETBALL', 'C', 'Pivot', 5),
        ('VOLLEYBALL', 'S', 'Armador', 1),
        ('VOLLEYBALL', 'OH', 'Punta receptor', 2),
        ('VOLLEYBALL', 'MB', 'Central', 3),
        ('VOLLEYBALL', 'OPP', 'Opuesto', 4),
        ('VOLLEYBALL', 'L', 'Libero', 5)
) AS p(sport_code, code, name, display_order)
    ON s.code = p.sport_code;
