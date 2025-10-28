-- V21: Move audience_segment from ad level to campaign level (Issue #9)
-- This migration moves the target audience relationship from ads to campaigns
-- to align with Facebook Ads Manager structure where targeting is at ad set (campaign) level

-- Step 1: Add campaign_id column to audience_segments table
ALTER TABLE audience_segments ADD COLUMN campaign_id BIGINT;

-- Step 2: Populate campaign_id from existing ad relationships
-- For each audience segment, get the campaign_id from its associated ad
UPDATE audience_segments seg
SET campaign_id = ads.campaign_id
FROM ads
WHERE seg.ad_id = ads.id;

-- Step 3: Add foreign key constraint to campaigns
ALTER TABLE audience_segments
ADD CONSTRAINT fk_audience_segment_campaign
FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE;

-- Step 4: Create index for performance
CREATE INDEX idx_audience_segments_campaign ON audience_segments(campaign_id);

-- Step 5: Drop old foreign key constraint and ad_id column
-- First drop the foreign key constraint if it exists
ALTER TABLE audience_segments DROP CONSTRAINT IF EXISTS fk_audience_segment_ad;

-- Then drop the ad_id column
ALTER TABLE audience_segments DROP COLUMN ad_id;

-- Step 6: Make campaign_id NOT NULL (all rows should have campaign_id now)
ALTER TABLE audience_segments ALTER COLUMN campaign_id SET NOT NULL;

-- Step 7: Add comment explaining the change
COMMENT ON COLUMN audience_segments.campaign_id IS 'Reference to campaign - audience targeting moved from ad level to campaign level (Issue #9)';

-- Step 8: Add index on user_id and campaign_id for common queries
CREATE INDEX idx_audience_segments_user_campaign ON audience_segments(user_id, campaign_id);
