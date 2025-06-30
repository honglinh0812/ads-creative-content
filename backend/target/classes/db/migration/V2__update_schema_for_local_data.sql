-- Migration để cập nhật database schema cho yêu cầu mới
-- Loại bỏ các trường liên quan đến Facebook API và cập nhật cấu trúc

-- Cập nhật bảng campaigns - loại bỏ fb_campaign_id
ALTER TABLE campaigns DROP COLUMN IF EXISTS fb_campaign_id;

-- Cập nhật bảng ads - loại bỏ fb_ad_id
ALTER TABLE ads DROP COLUMN IF EXISTS fb_ad_id;

-- Cập nhật bảng lead_form_ads - loại bỏ fb_form_id
ALTER TABLE lead_form_ads DROP COLUMN IF EXISTS fb_form_id;

-- Cập nhật bảng users - loại bỏ các trường Facebook không cần thiết cho workflow mới
-- Giữ lại facebook_id, facebook_token để xác thực nhưng không dùng để lấy dữ liệu
ALTER TABLE users DROP COLUMN IF EXISTS facebook_page_id;
ALTER TABLE users DROP COLUMN IF EXISTS fb_access_token;
ALTER TABLE users DROP COLUMN IF EXISTS fb_token_expires_at;
ALTER TABLE users DROP COLUMN IF EXISTS fb_user_id;

-- Thêm trường mới cho campaigns
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS target_audience TEXT;
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS daily_budget DECIMAL(10,2);
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS total_budget DECIMAL(10,2);
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS end_date DATE;

-- Thêm trường mới cho ads
ALTER TABLE ads ADD COLUMN IF NOT EXISTS prompt TEXT;
ALTER TABLE ads ADD COLUMN IF NOT EXISTS media_file_path VARCHAR(500);
ALTER TABLE ads ADD COLUMN IF NOT EXISTS selected_content_id BIGINT;

-- Cập nhật ad_contents để lưu nhiều preview options
ALTER TABLE ad_contents ADD COLUMN IF NOT EXISTS preview_order INTEGER DEFAULT 1;

-- Thêm foreign key constraint cho selected_content_id
ALTER TABLE ads ADD CONSTRAINT fk_ads_selected_content 
    FOREIGN KEY (selected_content_id) REFERENCES ad_contents(id);

-- Cập nhật enum values cho ad_type
-- Đảm bảo chỉ có 3 loại: PAGE_POST_AD, WEBSITE_CONVERSION_AD, LEAD_FORM_AD

-- Thêm index để tăng performance
CREATE INDEX IF NOT EXISTS idx_campaigns_user_id ON campaigns(user_id);
CREATE INDEX IF NOT EXISTS idx_ads_campaign_id ON ads(campaign_id);
CREATE INDEX IF NOT EXISTS idx_ad_contents_ad_id ON ad_contents(ad_id);
CREATE INDEX IF NOT EXISTS idx_ads_created_date ON ads(created_date);
CREATE INDEX IF NOT EXISTS idx_campaigns_created_at ON campaigns(created_at);

