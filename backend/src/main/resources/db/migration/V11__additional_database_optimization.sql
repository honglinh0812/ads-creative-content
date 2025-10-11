-- Migration V11: Additional Database Optimization
-- This migration adds missing indexes for new tables and optimizes existing queries

-- =====================================================
-- ASYNC JOB STATUS TABLE INDEXES (if table exists)
-- =====================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'async_job_status') THEN
        -- Primary indexes for async job status
        CREATE INDEX IF NOT EXISTS idx_async_job_status_job_id ON async_job_status(job_id);
        CREATE INDEX IF NOT EXISTS idx_async_job_status_user_id ON async_job_status(user_id);
        CREATE INDEX IF NOT EXISTS idx_async_job_status_status ON async_job_status(status);
        CREATE INDEX IF NOT EXISTS idx_async_job_status_job_type ON async_job_status(job_type);

        -- Composite indexes for common query patterns
        CREATE INDEX IF NOT EXISTS idx_async_job_user_status ON async_job_status(user_id, status);
        CREATE INDEX IF NOT EXISTS idx_async_job_user_created ON async_job_status(user_id, created_at DESC);
        CREATE INDEX IF NOT EXISTS idx_async_job_status_created ON async_job_status(status, created_at DESC);

        -- Index for cleanup operations
        CREATE INDEX IF NOT EXISTS idx_async_job_expires_at ON async_job_status(expires_at);
        CREATE INDEX IF NOT EXISTS idx_async_job_cleanup ON async_job_status(status, expires_at)
        WHERE status IN ('PENDING', 'IN_PROGRESS', 'EXPIRED');

        -- Index for active jobs counting
        CREATE INDEX IF NOT EXISTS idx_async_job_active_count ON async_job_status(user_id, status)
        WHERE status IN ('PENDING', 'IN_PROGRESS');
    END IF;
END $$;

-- =====================================================
-- ADDITIONAL MISSING INDEXES
-- =====================================================

-- Username index for case-insensitive searches
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_username_lower ON users(LOWER(username));
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users(LOWER(email));

-- Facebook ID for OAuth authentication
CREATE INDEX IF NOT EXISTS idx_users_facebook_id ON users(facebook_id) WHERE facebook_id IS NOT NULL;

-- Campaign date range queries
CREATE INDEX IF NOT EXISTS idx_campaigns_date_range ON campaigns(user_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_campaigns_created_between ON campaigns(user_id, created_date);

-- Ad content AI provider statistics
CREATE INDEX IF NOT EXISTS idx_ad_contents_user_ai_provider ON ad_contents(user_id, ai_provider)
WHERE ai_provider IS NOT NULL;

-- Performance monitoring indexes
CREATE INDEX IF NOT EXISTS idx_ads_created_date_hour ON ads(date_trunc('hour', created_date));
CREATE INDEX IF NOT EXISTS idx_campaigns_created_date_day ON campaigns(date_trunc('day', created_date));

-- =====================================================
-- UPDATED FOREIGN KEY CONSTRAINTS WITH INDEXES
-- =====================================================

-- Ensure all foreign keys have proper indexes for faster joins
CREATE INDEX IF NOT EXISTS idx_campaigns_user_id_fk ON campaigns(user_id);
CREATE INDEX IF NOT EXISTS idx_ads_campaign_id_fk ON ads(campaign_id);
CREATE INDEX IF NOT EXISTS idx_ads_user_id_fk ON ads(user_id);
CREATE INDEX IF NOT EXISTS idx_ad_contents_ad_id_fk ON ad_contents(ad_id);
CREATE INDEX IF NOT EXISTS idx_ad_contents_user_id_fk ON ad_contents(user_id);

-- =====================================================
-- COVERING INDEXES FOR READ-HEAVY OPERATIONS
-- =====================================================

-- Campaign list with basic info (covering index)
CREATE INDEX IF NOT EXISTS idx_campaigns_list_covering ON campaigns(user_id, created_date DESC)
INCLUDE (id, name, status, objective, budget);

-- Ad list with basic info (covering index)
CREATE INDEX IF NOT EXISTS idx_ads_list_covering ON ads(user_id, created_date DESC)
INCLUDE (id, name, status, ad_type, campaign_id);

-- Ad content list with basic info (covering index)
CREATE INDEX IF NOT EXISTS idx_ad_contents_list_covering ON ad_contents(ad_id, preview_order)
INCLUDE (id, headline, description, is_selected, ai_provider);

-- =====================================================
-- BTREE INDEXES FOR RANGE QUERIES
-- =====================================================

-- Budget range queries
CREATE INDEX IF NOT EXISTS idx_campaigns_budget_range ON campaigns(budget) WHERE budget IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_campaigns_daily_budget_range ON campaigns(daily_budget) WHERE daily_budget IS NOT NULL;

-- Date range queries optimization
CREATE INDEX IF NOT EXISTS idx_campaigns_active_period ON campaigns(start_date, end_date)
WHERE status = 'ACTIVE';

-- =====================================================
-- PARTIAL INDEXES FOR SPECIFIC CONDITIONS
-- =====================================================

-- Only non-null selected content IDs
CREATE INDEX IF NOT EXISTS idx_ads_selected_content ON ads(selected_content_id)
WHERE selected_content_id IS NOT NULL;

-- Only campaigns with target audience
CREATE INDEX IF NOT EXISTS idx_campaigns_targeted ON campaigns(user_id, created_date DESC)
WHERE target_audience IS NOT NULL AND target_audience != '';

-- Only completed async jobs for result retrieval
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'async_job_status') THEN
        CREATE INDEX IF NOT EXISTS idx_async_job_completed ON async_job_status(user_id, job_id, completed_at)
        WHERE status = 'COMPLETED';
    END IF;
END $$;

-- =====================================================
-- EXPRESSION INDEXES FOR COMPUTED QUERIES
-- =====================================================

-- Campaign name search (case-insensitive)
CREATE INDEX IF NOT EXISTS idx_campaigns_name_lower ON campaigns(LOWER(name));

-- Ad name search (case-insensitive)
CREATE INDEX IF NOT EXISTS idx_ads_name_lower ON ads(LOWER(name)) WHERE name IS NOT NULL;

-- =====================================================
-- VACUUM AND ANALYZE NEW INDEXES
-- =====================================================

-- Update statistics for query planner
ANALYZE users;
ANALYZE campaigns;
ANALYZE ads;
ANALYZE ad_contents;

-- Analyze async_job_status if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'async_job_status') THEN
        ANALYZE async_job_status;
    END IF;
END $$;

-- =====================================================
-- PERFORMANCE MONITORING VIEWS UPDATE
-- =====================================================

-- Update performance monitoring view to include new table
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

-- Update index usage view
CREATE OR REPLACE VIEW v_index_usage AS
SELECT
    schemaname,
    relname AS tablename,
    indexrelname AS indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_scan,
    idx_tup_read + idx_tup_fetch AS total_reads,
    CASE
        WHEN idx_scan = 0 THEN 0
        ELSE ROUND((idx_tup_read + idx_tup_fetch)::numeric / idx_scan, 2)
    END AS avg_reads_per_scan
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY total_reads DESC;

-- View for tracking table sizes and growth
CREATE OR REPLACE VIEW v_table_sizes AS
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) AS index_size,
    pg_stat_get_live_tuples(c.oid) AS live_tuples,
    pg_stat_get_dead_tuples(c.oid) AS dead_tuples
FROM pg_tables t
JOIN pg_class c ON c.relname = t.tablename
WHERE t.schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- =====================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'async_job_status') THEN
        COMMENT ON INDEX idx_async_job_user_status IS 'Optimizes async job queries by user and status';
        COMMENT ON INDEX idx_async_job_cleanup IS 'Optimizes cleanup operations for expired jobs';
    END IF;
END $$;

COMMENT ON INDEX idx_campaigns_list_covering IS 'Covering index for campaign list queries';
COMMENT ON INDEX idx_ads_list_covering IS 'Covering index for ad list queries';
COMMENT ON INDEX idx_ad_contents_list_covering IS 'Covering index for ad content list queries';

-- =====================================================
-- MAINTENANCE RECOMMENDATIONS
-- =====================================================

-- Note: The following maintenance tasks should be scheduled regularly:
-- 1. Weekly VACUUM ANALYZE on all tables
-- 2. Monthly REINDEX on heavily updated tables
-- 3. Monitor index usage with v_index_usage view
-- 4. Monitor table growth with v_table_sizes view
-- 5. Consider dropping unused indexes identified by monitoring