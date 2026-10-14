CREATE TABLE login_pre_authentications (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    attempts smallint NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 10),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_login_pre_auth_expiry CHECK (expires_at > created_at)
);
CREATE INDEX ix_login_pre_auth_user_expiry ON login_pre_authentications(user_id, expires_at) WHERE consumed_at IS NULL;
