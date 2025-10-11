-- Migration V13: Add Audience Segments table
-- This migration adds support for audience targeting and personalization

-- Create audience_segments table
CREATE TABLE IF NOT EXISTS audience_segments (
    id BIGSERIAL PRIMARY KEY,
    ad_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'ALL')),
    min_age INTEGER CHECK (min_age >= 13 AND min_age <= 65),
    max_age INTEGER CHECK (max_age >= 13 AND max_age <= 65),
    location VARCHAR(500),
    interests VARCHAR(1000),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints with CASCADE DELETE
    CONSTRAINT fk_audience_segments_ad_id
        FOREIGN KEY (ad_id) REFERENCES ads(id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_audience_segments_user_id
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- Ensure age range is valid
    CONSTRAINT chk_age_range CHECK (max_age >= min_age OR max_age IS NULL OR min_age IS NULL)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_audience_segments_ad_id ON audience_segments(ad_id);
CREATE INDEX IF NOT EXISTS idx_audience_segments_user_id ON audience_segments(user_id);
CREATE INDEX IF NOT EXISTS idx_audience_segments_gender ON audience_segments(gender);
CREATE INDEX IF NOT EXISTS idx_audience_segments_location ON audience_segments(location);

-- Add comments for documentation
COMMENT ON TABLE audience_segments IS 'Stores audience targeting information for ads including demographics and interests';
COMMENT ON COLUMN audience_segments.gender IS 'Target gender: MALE, FEMALE, or ALL';
COMMENT ON COLUMN audience_segments.min_age IS 'Minimum age (13-65, Facebook policy)';
COMMENT ON COLUMN audience_segments.max_age IS 'Maximum age (13-65)';
COMMENT ON COLUMN audience_segments.location IS 'Target location (country, city, or region)';
COMMENT ON COLUMN audience_segments.interests IS 'Comma-separated list of interests';

-- Update statistics
ANALYZE audience_segments;
