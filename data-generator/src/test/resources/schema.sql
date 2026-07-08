CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255),
    name       VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    locked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE contents
(
    id          UUID PRIMARY KEY,
    type        VARCHAR(20)  NOT NULL,
    title       VARCHAR(500) NOT NULL,
    description TEXT,
    released_at TIMESTAMPTZ,
    metadata    JSONB,
    source      VARCHAR(20)  NOT NULL,
    external_id VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_contents_source_external_id UNIQUE (source, external_id),
    CONSTRAINT ck_contents_type CHECK (type IN ('MOVIE', 'TV_SERIES', 'SPORT')),
    CONSTRAINT ck_contents_source CHECK (source IN ('TMDB', 'SPORTS_DB', 'ADMIN'))
);

CREATE TABLE content_stats
(
    id            UUID             PRIMARY KEY REFERENCES contents (id) ON DELETE CASCADE,
    review_count  INTEGER          NOT NULL DEFAULT 0,
    rating_sum    DOUBLE PRECISION NOT NULL DEFAULT 0,
    watcher_count BIGINT           NOT NULL DEFAULT 0
);

CREATE TABLE tags
(
    id   UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uk_tags_name UNIQUE (name)
);

CREATE TABLE content_tags
(
    content_id UUID NOT NULL REFERENCES contents (id) ON DELETE CASCADE,
    tag_id     UUID NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    CONSTRAINT pk_content_tags PRIMARY KEY (content_id, tag_id)
);

CREATE TABLE reviews
(
    id         UUID             PRIMARY KEY,
    content_id UUID             NOT NULL REFERENCES contents (id) ON DELETE CASCADE,
    user_id    UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text       TEXT,
    rating     DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ      NOT NULL,
    updated_at TIMESTAMPTZ      NOT NULL,
    CONSTRAINT uk_reviews_content_id_user_id UNIQUE (content_id, user_id),
    CONSTRAINT ck_review_rating CHECK (rating >= 0 AND rating <= 5)
);

CREATE TABLE playlists
(
    id               UUID         PRIMARY KEY,
    owner_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title            VARCHAR(255) NOT NULL,
    description      TEXT         NOT NULL,
    subscriber_count INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE TABLE playlist_items
(
    id          UUID        PRIMARY KEY,
    playlist_id UUID        NOT NULL REFERENCES playlists (id) ON DELETE CASCADE,
    content_id  UUID        NOT NULL REFERENCES contents (id) ON DELETE CASCADE,
    added_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_playlist_items_playlist_id_content_id UNIQUE (playlist_id, content_id)
);

CREATE TABLE playlist_subscriptions
(
    id            UUID        PRIMARY KEY,
    playlist_id   UUID        NOT NULL REFERENCES playlists (id) ON DELETE CASCADE,
    subscriber_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_playlist_subscriptions_playlist_id_subscriber_id UNIQUE (playlist_id, subscriber_id)
);

CREATE TABLE follows
(
    id          UUID        PRIMARY KEY,
    follower_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    followee_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_follows_follower_id_followee_id UNIQUE (follower_id, followee_id),
    CONSTRAINT ck_follow_self CHECK (follower_id <> followee_id)
);

CREATE TABLE notifications
(
    id          UUID         PRIMARY KEY,
    receiver_id UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT,
    level       VARCHAR(20)  NOT NULL DEFAULT 'INFO',
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_noti_type CHECK (type IN (
        'ROLE_CHANGED', 'PLAYLIST_SUBSCRIBED', 'PLAYLIST_UPDATED',
        'FOLLOWED', 'DIRECT_MESSAGE', 'WATCHING_ACTIVITY'
    )),
    CONSTRAINT ck_noti_level CHECK (level IN ('INFO', 'WARNING', 'ERROR')),
    CONSTRAINT ck_noti_read_state CHECK (
        (is_read = FALSE AND read_at IS NULL) OR
        (is_read = TRUE AND read_at IS NOT NULL)
    )
);
