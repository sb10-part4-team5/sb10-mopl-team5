CREATE TABLE login_sessions
(
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL,
    session_id  UUID        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_login_session_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_login_session_session_id UNIQUE (session_id)
);

CREATE INDEX idx_login_sessions_user ON login_sessions (user_id);
CREATE INDEX idx_login_sessions_expires_at ON login_sessions (expires_at);
