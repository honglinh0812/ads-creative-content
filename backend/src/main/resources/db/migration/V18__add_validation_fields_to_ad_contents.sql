-- Add validation-related fields to ad_contents table for content quality tracking
-- Migration: V18__add_validation_fields_to_ad_contents.sql

ALTER TABLE ad_contents
ADD COLUMN quality_score INTEGER,
ADD COLUMN validation_warnings VARCHAR(2000),
ADD COLUMN has_warnings BOOLEAN DEFAULT FALSE;

-- Create index for filtering content with warnings
CREATE INDEX idx_ad_contents_has_warnings ON ad_contents(has_warnings);

-- Create index for quality score queries
CREATE INDEX idx_ad_contents_quality_score ON ad_contents(quality_score);

COMMENT ON COLUMN ad_contents.quality_score IS 'Content quality score from 0-100 based on validation checks';
COMMENT ON COLUMN ad_contents.validation_warnings IS 'JSON array or comma-separated list of validation warnings';
COMMENT ON COLUMN ad_contents.has_warnings IS 'Quick flag to filter content with validation warnings';
