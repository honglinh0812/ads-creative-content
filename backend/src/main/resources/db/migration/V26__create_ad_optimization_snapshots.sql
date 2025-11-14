CREATE TABLE IF NOT EXISTS ad_optimization_snapshots (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ad_id BIGINT NOT NULL,
    ad_name VARCHAR(500),
    campaign_name VARCHAR(500),
    language VARCHAR(10),
    suggestions_json TEXT NOT NULL,
    scorecard_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ad_optimization_snapshots_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ad_optimization_snapshots_user_created
    ON ad_optimization_snapshots (user_id, created_at DESC);
