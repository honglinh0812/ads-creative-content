ALTER TABLE campaigns
    ADD COLUMN IF NOT EXISTS performance_goal VARCHAR(64);
