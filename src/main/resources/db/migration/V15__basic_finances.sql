CREATE TABLE financial_movement (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    tournament_team_id BIGINT,
    movement_type VARCHAR(20) NOT NULL,
    category VARCHAR(60) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    occurred_on DATE NOT NULL,
    description VARCHAR(300),
    reference_code VARCHAR(80),
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_financial_movement_tournament
        FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    CONSTRAINT fk_financial_movement_tournament_team
        FOREIGN KEY (tournament_team_id) REFERENCES tournament_team (id),
    CONSTRAINT fk_financial_movement_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT ck_financial_movement_type
        CHECK (movement_type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_financial_movement_category
        CHECK (category IN (
            'INSCRIPCION_EQUIPO',
            'APORTE_SIMPLE',
            'PATROCINIO_SIMPLE',
            'OTRO_INGRESO_OPERATIVO',
            'ARBITRAJE',
            'CANCHA',
            'LOGISTICA',
            'PREMIOS',
            'OTRO_GASTO_OPERATIVO'
        )),
    CONSTRAINT ck_financial_movement_amount
        CHECK (amount > 0),
    CONSTRAINT ck_financial_movement_income_expense_category
        CHECK (
            (movement_type = 'INCOME'
                AND category IN (
                    'INSCRIPCION_EQUIPO',
                    'APORTE_SIMPLE',
                    'PATROCINIO_SIMPLE',
                    'OTRO_INGRESO_OPERATIVO'
                ))
            OR
            (movement_type = 'EXPENSE'
                AND tournament_team_id IS NULL
                AND category IN (
                    'ARBITRAJE',
                    'CANCHA',
                    'LOGISTICA',
                    'PREMIOS',
                    'OTRO_GASTO_OPERATIVO'
                ))
        )
);

CREATE INDEX ix_financial_movement_tournament
    ON financial_movement (tournament_id, occurred_on DESC, id DESC);

CREATE INDEX ix_financial_movement_tournament_type
    ON financial_movement (tournament_id, movement_type, occurred_on DESC, id DESC);

CREATE INDEX ix_financial_movement_tournament_category
    ON financial_movement (tournament_id, category, occurred_on DESC, id DESC);

CREATE INDEX ix_financial_movement_tournament_team
    ON financial_movement (tournament_id, tournament_team_id, occurred_on DESC, id DESC);
