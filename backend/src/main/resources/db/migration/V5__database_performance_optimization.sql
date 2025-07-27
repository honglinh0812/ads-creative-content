-- Migration V4: Database Performance Optimization
-- This migration adds indexes, optimizes queries, and improves overall database performance

-- =====================================================
-- PERFORMANCE INDEXES
-- =====================================================

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_facebook_id ON users(facebook_id);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_date);

-- Campaigns table indexes
CREATE INDEX IF NOT EXISTS idx_campaigns_user_id_status ON campaigns(user_id, status);
CREATE INDEX IF NOT EXISTS idx_campaigns_user_id_created_at ON campaigns(user_id, created_date DESC);
CREATE INDEX IF NOT EXISTS idx_campaigns_status ON campaigns(status);
CREATE INDEX IF NOT EXISTS idx_campaigns_objective ON campaigns(objective);
CREATE INDEX IF NOT EXISTS idx_campaigns_start_date ON campaigns(start_date);
CREATE INDEX IF NOT EXISTS idx_campaigns_end_date ON campaigns(end_date);
CREATE INDEX IF NOT EXISTS idx_campaigns_budget_type ON campaigns(budget_type);

-- Ads table indexes
CREATE INDEX IF NOT EXISTS idx_ads_campaign_id_status ON ads(campaign_id, status);
CREATE INDEX IF NOT EXISTS idx_ads_user_id_status ON ads(user_id, status);
CREATE INDEX IF NOT EXISTS idx_ads_user_id_created_date ON ads(user_id, created_date DESC);
CREATE INDEX IF NOT EXISTS idx_ads_campaign_id_created_date ON ads(campaign_id, created_date DESC);
CREATE INDEX IF NOT EXISTS idx_ads_ad_type ON ads(ad_type);
CREATE INDEX IF NOT EXISTS idx_ads_status ON ads(status);
CREATE INDEX IF NOT EXISTS idx_ads_selected_content_id ON ads(selected_content_id);

-- Ad Contents table indexes
CREATE INDEX IF NOT EXISTS idx_ad_contents_ad_id_preview_order ON ad_contents(ad_id, preview_order);
CREATE INDEX IF NOT EXISTS idx_ad_contents_user_id_created_date ON ad_contents(user_id, created_date DESC);
CREATE INDEX IF NOT EXISTS idx_ad_contents_ad_id_is_selected ON ad_contents(ad_id, is_selected);
CREATE INDEX IF NOT EXISTS idx_ad_contents_ai_provider ON ad_contents(ai_provider);
CREATE INDEX IF NOT EXISTS idx_ad_contents_content_type ON ad_contents(content_type);
CREATE INDEX IF NOT EXISTS idx_ad_contents_is_selected ON ad_contents(is_selected);

-- =====================================================
-- COMPOSITE INDEXES FOR COMMON QUERY PATTERNS
-- =====================================================

-- Dashboard queries optimization
CREATE INDEX IF NOT EXISTS idx_campaigns_dashboard ON campaigns(user_id, status, created_date DESC);
CREATE INDEX IF NOT EXISTS idx_ads_dashboard ON ads(user_id, status, created_date DESC);

-- Campaign-Ad relationship optimization
CREATE INDEX IF NOT EXISTS idx_ads_campaign_user ON ads(campaign_id, user_id, status);

-- Content filtering optimization
CREATE INDEX IF NOT EXISTS idx_ad_contents_filtering ON ad_contents(ad_id, user_id, is_selected, preview_order);

-- =====================================================
-- PARTIAL INDEXES FOR BETTER PERFORMANCE
-- =====================================================

-- Index only active campaigns
CREATE INDEX IF NOT EXISTS idx_campaigns_active ON campaigns(user_id, created_date DESC) 
WHERE status = 'ACTIVE';

-- Index only active ads
CREATE INDEX IF NOT EXISTS idx_ads_active ON ads(user_id, created_date DESC) 
WHERE status = 'ACTIVE';

-- Index only selected ad contents
CREATE INDEX IF NOT EXISTS idx_ad_contents_selected ON ad_contents(ad_id, preview_order) 
WHERE is_selected = true;

-- =====================================================
-- TEXT SEARCH INDEXES
-- =====================================================

-- Full-text search indexes for campaigns
CREATE INDEX IF NOT EXISTS idx_campaigns_name_gin ON campaigns USING gin(to_tsvector('english', name));
CREATE INDEX IF NOT EXISTS idx_campaigns_target_audience_gin ON campaigns USING gin(to_tsvector('english', target_audience));

-- Full-text search indexes for ads
CREATE INDEX IF NOT EXISTS idx_ads_name_gin ON ads USING gin(to_tsvector('english', name));
CREATE INDEX IF NOT EXISTS idx_ads_headline_gin ON ads USING gin(to_tsvector('english', headline));
CREATE INDEX IF NOT EXISTS idx_ads_primary_text_gin ON ads USING gin(to_tsvector('english', primary_text));

-- Full-text search indexes for ad contents
CREATE INDEX IF NOT EXISTS idx_ad_contents_headline_gin ON ad_contents USING gin(to_tsvector('english', headline));
CREATE INDEX IF NOT EXISTS idx_ad_contents_primary_text_gin ON ad_contents USING gin(to_tsvector('english', primary_text));

-- =====================================================
-- STATISTICS UPDATE
-- =====================================================

-- Update table statistics for better query planning
ANALYZE users;
ANALYZE campaigns;
ANALYZE ads;
ANALYZE ad_contents;

-- =====================================================
-- CONSTRAINTS OPTIMIZATION
-- =====================================================

-- Add NOT NULL constraints where appropriate for better performance
ALTER TABLE campaigns ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ads ALTER COLUMN campaign_id SET NOT NULL;
ALTER TABLE ads ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ad_contents ALTER COLUMN ad_id SET NOT NULL;
ALTER TABLE ad_contents ALTER COLUMN user_id SET NOT NULL;

-- =====================================================
-- VACUUM AND REINDEX
-- =====================================================

-- Note: These commands should be run manually during maintenance windows
-- VACUUM ANALYZE users;
-- VACUUM ANALYZE campaigns;
-- VACUUM ANALYZE ads;
-- VACUUM ANALYZE ad_contents;

-- REINDEX TABLE users;
-- REINDEX TABLE campaigns;
-- REINDEX TABLE ads;
-- REINDEX TABLE ad_contents;

-- =====================================================
-- PERFORMANCE MONITORING VIEWS
-- =====================================================

-- Create view for monitoring slow queries
CREATE OR REPLACE VIEW v_performance_stats AS
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats 
WHERE schemaname = 'public' 
AND tablename IN ('users', 'campaigns', 'ads', 'ad_contents');

-- Create view for index usage monitoring
CREATE OR REPLACE VIEW v_index_usage AS
SELECT 
    schemaname,
    relname AS tablename,
    indexrelname AS indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_scan
FROM pg_stat_user_indexes 
WHERE schemaname = 'public';

-- =====================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================

COMMENT ON INDEX idx_campaigns_user_id_status IS 'Optimizes dashboard campaign queries by user and status';
COMMENT ON INDEX idx_ads_user_id_created_date IS 'Optimizes recent ads queries for dashboard';
COMMENT ON INDEX idx_ad_contents_ad_id_preview_order IS 'Optimizes ad content ordering queries';
COMMENT ON INDEX idx_campaigns_dashboard IS 'Composite index for dashboard campaign statistics';
COMMENT ON INDEX idx_ads_dashboard IS 'Composite index for dashboard ad statistics';

-- =====================================================
-- PERFORMANCE CONFIGURATION RECOMMENDATIONS
-- =====================================================

-- The following settings should be added to postgresql.conf:
-- shared_buffers = 256MB (or 25% of RAM)
-- effective_cache_size = 1GB (or 75% of RAM)
-- work_mem = 4MB
-- maintenance_work_mem = 64MB
-- checkpoint_completion_target = 0.9
-- wal_buffers = 16MB
-- default_statistics_target = 100
-- random_page_cost = 1.1 (for SSD)
-- effective_io_concurrency = 200 (for SSD)

-- Connection pooling recommendations:
-- max_connections = 100
-- Use connection pooling (PgBouncer recommended)
-- Pool size: 10-20 connections per CPU core
