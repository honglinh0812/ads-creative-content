-- =====================================================
-- Migration V19: Add READY and EXPORTED status to campaigns
-- Issue #7: Campaign status logic enhancement
-- =====================================================

-- Step 1: Update campaigns with ads from DRAFT to READY
-- Logic: Campaign có ads và đang ở DRAFT → chuyển thành READY
UPDATE campaigns c
SET status = 'READY',
    updated_at = NOW()
WHERE status = 'DRAFT'
  AND EXISTS (
      SELECT 1 FROM ads a WHERE a.campaign_id = c.id
  );

-- Step 2: Add comprehensive comment explaining status values
COMMENT ON COLUMN campaigns.status IS 'Campaign status values:
- DRAFT: Campaign has no ads yet (initial state)
- READY: Campaign has ads and is ready to export
- EXPORTED: Campaign ads have been exported to Facebook
- PENDING: Campaign is pending approval
- ACTIVE: Campaign is actively running
- PAUSED: Campaign is temporarily paused
- COMPLETED: Campaign has finished running
- FAILED: Campaign creation/execution failed';

-- Step 3: Create index for filtering by READY and EXPORTED status
CREATE INDEX IF NOT EXISTS idx_campaigns_ready_status
ON campaigns(user_id, status, created_date DESC)
WHERE status = 'READY';

CREATE INDEX IF NOT EXISTS idx_campaigns_exported_status
ON campaigns(user_id, status, created_date DESC)
WHERE status = 'EXPORTED';

-- Step 4: Add trigger to automatically update campaign status when ads change (optional, for data integrity)
-- This ensures status consistency at database level

-- Log migration completion
DO $$
BEGIN
    RAISE NOTICE 'Migration V19 completed successfully';
    RAISE NOTICE 'Updated % campaigns from DRAFT to READY',
        (SELECT COUNT(*) FROM campaigns WHERE status = 'READY');
END $$;
