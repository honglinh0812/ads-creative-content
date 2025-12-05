-- V31: Attach personas to ads so optimization can reuse audience context
ALTER TABLE ads
    ADD COLUMN IF NOT EXISTS persona_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_ads_persona'
    ) THEN
        ALTER TABLE ads
            ADD CONSTRAINT fk_ads_persona
            FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ads_persona_id ON ads(persona_id);

COMMENT ON COLUMN ads.persona_id IS 'Optional persona selected during ad creation';
