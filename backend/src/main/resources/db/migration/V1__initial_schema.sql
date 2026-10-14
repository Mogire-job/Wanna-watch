-- Core catalogue
CREATE TABLE titles (
    id            BIGSERIAL PRIMARY KEY,
    external_id   VARCHAR(64),                 -- id at the metadata provider
    type          VARCHAR(16)  NOT NULL CHECK (type IN ('MOVIE', 'SERIES')),
    name          VARCHAR(300) NOT NULL,
    release_year  INTEGER,
    synopsis      TEXT,
    poster_url    VARCHAR(500),
    rating        NUMERIC(3, 1),
    popularity    NUMERIC(10, 3) NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (external_id)
);

CREATE INDEX idx_titles_popularity ON titles (popularity DESC);
CREATE INDEX idx_titles_search ON titles USING GIN (to_tsvector('simple', name));

CREATE TABLE genres (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE title_genres (
    title_id BIGINT NOT NULL REFERENCES titles (id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genres (id) ON DELETE CASCADE,
    PRIMARY KEY (title_id, genre_id)
);

-- Official trailers (YouTube video keys, played through the embed)
CREATE TABLE videos (
    id            BIGSERIAL PRIMARY KEY,
    title_id      BIGINT       NOT NULL REFERENCES titles (id) ON DELETE CASCADE,
    youtube_key   VARCHAR(32)  NOT NULL,
    kind          VARCHAR(32)  NOT NULL DEFAULT 'OFFICIAL_TRAILER',
    language      VARCHAR(8),
    UNIQUE (title_id, youtube_key)
);

-- Where to watch
CREATE TABLE providers (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(120) NOT NULL UNIQUE,
    logo_url  VARCHAR(500),
    website   VARCHAR(300)
);

CREATE TABLE availability (
    id            BIGSERIAL PRIMARY KEY,
    title_id      BIGINT      NOT NULL REFERENCES titles (id) ON DELETE CASCADE,
    provider_id   BIGINT      NOT NULL REFERENCES providers (id) ON DELETE CASCADE,
    country       CHAR(2)     NOT NULL,
    offer_type    VARCHAR(16) NOT NULL CHECK (offer_type IN ('STREAM', 'RENT', 'BUY', 'FREE')),
    deep_link     VARCHAR(600),
    last_checked  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (title_id, provider_id, country, offer_type)
);

CREATE INDEX idx_availability_title_country ON availability (title_id, country);

-- Users and watch lists
CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(254) NOT NULL UNIQUE,
    display_name   VARCHAR(120),
    password_hash  VARCHAR(100),               -- null for Google sign-in
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE watchlist_items (
    user_id   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title_id  BIGINT      NOT NULL REFERENCES titles (id) ON DELETE CASCADE,
    added_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, title_id)
);

-- Usage events for retention metrics and later recommendations
CREATE TABLE events (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    session_id  VARCHAR(64),
    event_type  VARCHAR(40) NOT NULL,
    title_id    BIGINT REFERENCES titles (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_created ON events (created_at);
