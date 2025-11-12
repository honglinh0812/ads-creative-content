-- =====================================================
-- Migration V24: Add trigger to automatically update campaign status
-- Issue #2: Campaign status always DRAFT
-- =====================================================

-- Step 1: Create function to update campaign status based on ads
CREATE OR REPLACE FUNCTION update_campaign_status()
RETURNS TRIGGER AS $$
BEGIN
    -- Update campaign status to READY if it has ads and is currently DRAFT
    UPDATE campaigns 
    SET status = 'READY',
        updated_at = NOW()
    WHERE id IN (
        SELECT DISTINCT c.id 
        FROM campaigns c
        WHERE c.status = 'DRAFT'
        AND EXISTS (
            SELECT 1 FROM ads a WHERE a.campaign_id = c.id
        )
    );
    
    -- Update campaign status to DRAFT if it has no ads and is currently READY
    UPDATE campaigns 
    SET status = 'DRAFT',
        updated_at = NOW()
    WHERE id IN (
        SELECT DISTINCT c.id 
        FROM campaigns c
        WHERE c.status = 'READY'
        AND NOT EXISTS (
            SELECT 1 FROM ads a WHERE a.campaign_id = c.id
        )
    );
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Step 2: Create trigger on ads table for INSERT, UPDATE, DELETE operations
CREATE TRIGGER campaign_status_update_trigger
    AFTER INSERT OR UPDATE OR DELETE ON ads
    FOR EACH STATEMENT
    EXECUTE FUNCTION update_campaign_status();

-- Step 3: Create function to update single campaign status
CREATE OR REPLACE FUNCTION update_single_campaign_status(campaign_id BIGINT)
RETURNS VOID AS $$
BEGIN
    -- Check if campaign has ads
    IF EXISTS (SELECT 1 FROM ads WHERE campaign_id = campaign_id) THEN
        -- If campaign is DRAFT, update to READY
        UPDATE campaigns 
        SET status = 'READY',
            updated_at = NOW()
        WHERE id = campaign_id AND status = 'DRAFT';
    ELSE
        -- If campaign has no ads and is READY, update to DRAFT
        UPDATE campaigns 
        SET status = 'DRAFT',
            updated_at = NOW()
        WHERE id = campaign_id AND status = 'READY';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Step 4: Create trigger function for individual row operations
CREATE OR REPLACE FUNCTION update_campaign_status_on_ad_change()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        -- Update campaign status when ad is inserted or updated
        PERFORM update_single_campaign_status(NEW.campaign_id);
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        -- Update campaign status when ad is deleted
        PERFORM update_single_campaign_status(OLD.campaign_id);
        RETURN OLD;
    END IF;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Step 5: Drop the statement-level trigger and create row-level trigger for better performance
DROP TRIGGER IF EXISTS campaign_status_update_trigger ON ads;

CREATE TRIGGER campaign_status_update_trigger
    AFTER INSERT OR UPDATE OR DELETE ON ads
    FOR EACH ROW
    EXECUTE FUNCTION update_campaign_status_on_ad_change();

-- Step 6: Initial update for existing data
SELECT update_campaign_status();

-- Log migration completion
DO $$
BEGIN
    RAISE NOTICE 'Migration V24 completed successfully';
    RAISE NOTICE 'Added trigger to automatically update campaign status based on ads';
END $$;