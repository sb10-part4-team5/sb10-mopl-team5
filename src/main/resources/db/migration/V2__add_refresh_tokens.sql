-- ---------- 리프레시 토큰 (단일 세션) ----------
-- 단일 세션 정책: 유저당 토큰 1개 (user_id UNIQUE)
-- 로그인/refresh 시 기존 row를 갱신(UPSERT)하여 토큰을 회전

CREATE TABLE refresh_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL,
    token_hash VARCHAR(255) NOT NULL,   -- 원문이 아닌 해시 저장
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_refresh_tokens_user_id UNIQUE (user_id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_expires ON refresh_tokens (expires_at);
