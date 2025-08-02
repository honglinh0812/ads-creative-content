-- Add new columns for ad type specific fields
ALTER TABLE ads ADD COLUMN website_url VARCHAR(1000);
ALTER TABLE ads ADD COLUMN lead_form_questions TEXT;

-- Add comments for documentation
COMMENT ON COLUMN ads.website_url IS 'Website URL for WEBSITE_CONVERSION_AD type';
COMMENT ON COLUMN ads.lead_form_questions IS 'JSON string containing lead form questions for LEAD_FORM_AD type'; 