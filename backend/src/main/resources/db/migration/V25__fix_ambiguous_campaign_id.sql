-- =====================================================
-- Migration V25: Fix ambiguous campaign_id in trigger function
-- Issue #1: Column reference "campaign_id" is ambiguous
-- =====================================================

-- Drop existing function to recreate with fixed parameter name
DROP FUNCTION IF EXISTS update_single_campaign_status(BIGINT);

-- Recreate function with unambiguous parameter name
CREATE OR REPLACE FUNCTION update_single_campaign_status(p_campaign_id BIGINT)
RETURNS VOID AS $$
BEGIN
    -- Check if campaign has ads
    -- Use p_campaign_id parameter name to avoid ambiguity with ads.campaign_id column
    IF EXISTS (SELECT 1 FROM ads WHERE campaign_id = p_campaign_id) THEN
        -- If campaign is DRAFT, update to READY
        UPDATE campaigns
        SET status = 'READY',
            updated_at = NOW()
        WHERE id = p_campaign_id AND status = 'DRAFT';
    ELSE
        -- If campaign has no ads and is READY, update to DRAFT
        UPDATE campaigns
        SET status = 'DRAFT',
            updated_at = NOW()
        WHERE id = p_campaign_id AND status = 'READY';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Log migration completion
DO $$
BEGIN
    RAISE NOTICE 'Migration V25 completed successfully';
    RAISE NOTICE 'Fixed ambiguous campaign_id parameter in update_single_campaign_status function';
END $$;
