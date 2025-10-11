-- Migration: Create ad_performance_reports table for Facebook Ads Manager analytics
-- Purpose: Store imported performance data from Facebook reports
-- Security: User isolation via user_id foreign key, cascade deletion
-- Performance: Indexed on ad_id, campaign_id, report_date for fast queries
-- Author: AI Engineering Panel
-- Date: 2025-10-10

-- Create ad_performance_reports table
CREATE TABLE IF NOT EXISTS ad_performance_reports (
    id BIGSERIAL PRIMARY KEY,

    -- Foreign Keys
    ad_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    -- Performance Metrics
    report_date DATE NOT NULL,
    impressions BIGINT DEFAULT 0 NOT NULL,
    clicks BIGINT DEFAULT 0 NOT NULL,
    ctr DOUBLE PRECISION,  -- Click-through rate (calculated: clicks/impressions * 100)
    spend DOUBLE PRECISION,
    cpc DOUBLE PRECISION,  -- Cost per click
    cpm DOUBLE PRECISION,  -- Cost per thousand impressions
    conversions BIGINT DEFAULT 0,
    conversion_rate DOUBLE PRECISION,

    -- Metadata
    source VARCHAR(50) DEFAULT 'FACEBOOK' NOT NULL,
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    imported_by VARCHAR(255),

    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Foreign Key Constraints with CASCADE
    CONSTRAINT fk_reports_ad FOREIGN KEY (ad_id)
        REFERENCES ads(id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_campaign FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,

    -- Unique Constraint: One report per ad per date per source
    CONSTRAINT unique_ad_report_date UNIQUE(ad_id, report_date, source),

    -- Check Constraints for Data Integrity
    CONSTRAINT check_impressions_positive CHECK (impressions >= 0),
    CONSTRAINT check_clicks_positive CHECK (clicks >= 0),
    CONSTRAINT check_clicks_lte_impressions CHECK (clicks <= impressions),
    CONSTRAINT check_ctr_range CHECK (ctr IS NULL OR (ctr >= 0 AND ctr <= 100)),
    CONSTRAINT check_spend_positive CHECK (spend IS NULL OR spend >= 0),
    CONSTRAINT check_cpc_positive CHECK (cpc IS NULL OR cpc >= 0),
    CONSTRAINT check_cpm_positive CHECK (cpm IS NULL OR cpm >= 0),
    CONSTRAINT check_conversions_positive CHECK (conversions >= 0),
    CONSTRAINT check_conversion_rate_range CHECK (conversion_rate IS NULL OR (conversion_rate >= 0 AND conversion_rate <= 100))
);

-- Performance Indexes
-- Index on ad_id for quick lookups by ad
CREATE INDEX idx_reports_ad_id ON ad_performance_reports(ad_id);

-- Index on campaign_id for campaign-level aggregations
CREATE INDEX idx_reports_campaign_id ON ad_performance_reports(campaign_id);

-- Index on user_id for user isolation queries
CREATE INDEX idx_reports_user_id ON ad_performance_reports(user_id);

-- Index on report_date for time-series queries
CREATE INDEX idx_reports_date ON ad_performance_reports(report_date);

-- Composite index for date range queries per ad
CREATE INDEX idx_reports_ad_date ON ad_performance_reports(ad_id, report_date DESC);

-- Composite index for date range queries per campaign
CREATE INDEX idx_reports_campaign_date ON ad_performance_reports(campaign_id, report_date DESC);

-- Index on source for filtering by data source
CREATE INDEX idx_reports_source ON ad_performance_reports(source);

-- Composite index for user + date queries (common analytics pattern)
CREATE INDEX idx_reports_user_date ON ad_performance_reports(user_id, report_date DESC);

-- Add comments for documentation
COMMENT ON TABLE ad_performance_reports IS 'Stores performance metrics imported from Facebook Ads Manager reports';
COMMENT ON COLUMN ad_performance_reports.ad_id IS 'Reference to the ad in our system';
COMMENT ON COLUMN ad_performance_reports.campaign_id IS 'Reference to the campaign (denormalized for performance)';
COMMENT ON COLUMN ad_performance_reports.user_id IS 'Reference to the user who owns this data';
COMMENT ON COLUMN ad_performance_reports.report_date IS 'The date this performance data represents';
COMMENT ON COLUMN ad_performance_reports.impressions IS 'Number of times the ad was shown';
COMMENT ON COLUMN ad_performance_reports.clicks IS 'Number of times the ad was clicked';
COMMENT ON COLUMN ad_performance_reports.ctr IS 'Click-through rate as percentage (clicks/impressions * 100)';
COMMENT ON COLUMN ad_performance_reports.spend IS 'Amount spent on this ad for this date';
COMMENT ON COLUMN ad_performance_reports.cpc IS 'Cost per click (spend/clicks)';
COMMENT ON COLUMN ad_performance_reports.cpm IS 'Cost per thousand impressions (spend/impressions * 1000)';
COMMENT ON COLUMN ad_performance_reports.conversions IS 'Number of conversions tracked';
COMMENT ON COLUMN ad_performance_reports.conversion_rate IS 'Conversion rate as percentage (conversions/clicks * 100)';
COMMENT ON COLUMN ad_performance_reports.source IS 'Data source (FACEBOOK, MANUAL, etc.)';
COMMENT ON COLUMN ad_performance_reports.imported_at IS 'Timestamp when this data was imported';
COMMENT ON COLUMN ad_performance_reports.imported_by IS 'Username who imported this data';

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_ad_performance_reports_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to call the function before UPDATE
CREATE TRIGGER trigger_update_ad_performance_reports_updated_at
    BEFORE UPDATE ON ad_performance_reports
    FOR EACH ROW
    EXECUTE FUNCTION update_ad_performance_reports_updated_at();

-- Grant permissions (adjust as needed for your setup)
-- GRANT SELECT, INSERT, UPDATE ON ad_performance_reports TO fbadsautomation_app;
-- GRANT USAGE, SELECT ON SEQUENCE ad_performance_reports_id_seq TO fbadsautomation_app;
