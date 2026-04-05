CREATE TABLE app_user_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    authentication_scheme VARCHAR(20) NOT NULL DEFAULT 'BEARER',
    access_token_hash VARCHAR(64) NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    access_token_expires_at TIMESTAMPTZ NOT NULL,
    refresh_token_expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    ip_address VARCHAR(64),
    user_agent VARCHAR(255),
    CONSTRAINT fk_app_user_session_user
        FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE UNIQUE INDEX uq_app_user_session_access_token_hash
    ON app_user_session (access_token_hash);

CREATE UNIQUE INDEX uq_app_user_session_refresh_token_hash
    ON app_user_session (refresh_token_hash);

CREATE INDEX ix_app_user_session_user_id
    ON app_user_session (user_id);

CREATE INDEX ix_app_user_session_access_expires_at
    ON app_user_session (access_token_expires_at);

CREATE INDEX ix_app_user_session_refresh_expires_at
    ON app_user_session (refresh_token_expires_at);
