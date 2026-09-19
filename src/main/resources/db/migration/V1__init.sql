-- src/main/resources/db/migration/V1__create_initial_schema.sql

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE items (
    id                    BIGSERIAL PRIMARY KEY,
    title                 VARCHAR(150) NOT NULL,
    description           VARCHAR(2000),
    type                  VARCHAR(20) NOT NULL,
    category              VARCHAR(30) NOT NULL,
    color                 VARCHAR(40),
    occurred_at           DATE NOT NULL,
    latitude              DOUBLE PRECISION,
    longitude             DOUBLE PRECISION,
    location_description  VARCHAR(255),
    status                VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    user_id               BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at            TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_items_type_category_status ON items (type, category, status);
CREATE INDEX idx_items_user_id ON items (user_id);

CREATE TABLE item_images (
    id       BIGSERIAL PRIMARY KEY,
    url      VARCHAR(500) NOT NULL,
    item_id  BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE
);

CREATE INDEX idx_item_images_item_id ON item_images (item_id);

CREATE TABLE matches (
    id              BIGSERIAL PRIMARY KEY,
    lost_item_id    BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    found_item_id   BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    score           INTEGER NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_matches_lost_found UNIQUE (lost_item_id, found_item_id)
);

CREATE INDEX idx_matches_lost_item_id ON matches (lost_item_id);
CREATE INDEX idx_matches_found_item_id ON matches (found_item_id);