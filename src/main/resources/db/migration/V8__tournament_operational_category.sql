ALTER TABLE tournament
    ADD COLUMN operational_category VARCHAR(20);

UPDATE tournament
SET operational_category = 'PRODUCTION'
WHERE operational_category IS NULL;

ALTER TABLE tournament
    ALTER COLUMN operational_category SET NOT NULL,
    ALTER COLUMN operational_category SET DEFAULT 'PRODUCTION';

ALTER TABLE tournament
    ADD CONSTRAINT ck_tournament_operational_category
        CHECK (operational_category IN ('PRODUCTION', 'QA', 'DEMO', 'SANDBOX', 'ARCHIVED'));

CREATE INDEX ix_tournament_operational_category
    ON tournament (operational_category);
