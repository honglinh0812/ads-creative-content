-- Migration V3: Thêm user_id vào các entity cần thiết để đảm bảo cô lập dữ liệu người dùng

-- Thêm user_id vào bảng ads nếu chưa có
ALTER TABLE ads ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- Thêm user_id vào bảng ad_contents nếu chưa có  
ALTER TABLE ad_contents ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- Thêm user_id vào bảng lead_form_ads nếu chưa có
ALTER TABLE lead_form_ads ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- Thêm user_id vào bảng page_post_ads nếu chưa có
ALTER TABLE page_post_ads ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- Thêm user_id vào bảng website_conversion_ads nếu chưa có
ALTER TABLE website_conversion_ads ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- Thêm foreign key constraints
ALTER TABLE ads ADD CONSTRAINT fk_ads_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE ad_contents ADD CONSTRAINT fk_ad_contents_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE lead_form_ads ADD CONSTRAINT fk_lead_form_ads_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE page_post_ads ADD CONSTRAINT fk_page_post_ads_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE website_conversion_ads ADD CONSTRAINT fk_website_conversion_ads_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id);

-- Thêm index để tăng performance
CREATE INDEX IF NOT EXISTS idx_ads_user_id ON ads(user_id);
CREATE INDEX IF NOT EXISTS idx_ad_contents_user_id ON ad_contents(user_id);
CREATE INDEX IF NOT EXISTS idx_lead_form_ads_user_id ON lead_form_ads(user_id);
CREATE INDEX IF NOT EXISTS idx_page_post_ads_user_id ON page_post_ads(user_id);
CREATE INDEX IF NOT EXISTS idx_website_conversion_ads_user_id ON website_conversion_ads(user_id);

-- Cập nhật dữ liệu hiện có (nếu có) để gán user_id từ campaign
UPDATE ads SET user_id = (
    SELECT c.user_id 
    FROM campaigns c 
    WHERE c.id = ads.campaign_id
) WHERE user_id IS NULL;

UPDATE ad_contents SET user_id = (
    SELECT a.user_id 
    FROM ads a 
    WHERE a.id = ad_contents.ad_id
) WHERE user_id IS NULL;

UPDATE lead_form_ads SET user_id = (
    SELECT a.user_id 
    FROM ads a 
    WHERE a.id = lead_form_ads.ad_id
) WHERE user_id IS NULL;

UPDATE page_post_ads SET user_id = (
    SELECT a.user_id 
    FROM ads a 
    WHERE a.id = page_post_ads.ad_id
) WHERE user_id IS NULL;

UPDATE website_conversion_ads SET user_id = (
    SELECT a.user_id 
    FROM ads a 
    WHERE a.id = website_conversion_ads.ad_id
) WHERE user_id IS NULL;

-- Đặt NOT NULL constraint sau khi đã cập nhật dữ liệu
ALTER TABLE ads ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ad_contents ALTER COLUMN user_id SET NOT NULL;

