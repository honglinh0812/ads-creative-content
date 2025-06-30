-- Migration V4: Tăng độ dài các trường có thể chứa nội dung AI dài

-- Tăng độ dài trường headline từ 255 lên 500 characters
ALTER TABLE ads ALTER COLUMN headline TYPE VARCHAR(500);
ALTER TABLE ad_contents ALTER COLUMN headline TYPE VARCHAR(500);

-- Tăng độ dài trường call_to_action từ 255 lên 500 characters  
ALTER TABLE ads ALTER COLUMN call_to_action TYPE VARCHAR(500);
ALTER TABLE ad_contents ALTER COLUMN call_to_action TYPE VARCHAR(500);

-- Tăng độ dài trường description từ 500 lên 1000 characters
ALTER TABLE ads ALTER COLUMN description TYPE VARCHAR(1000);
ALTER TABLE ad_contents ALTER COLUMN description TYPE VARCHAR(1000);

-- Tăng độ dài trường primary_text từ 1000 lên 2000 characters
ALTER TABLE ads ALTER COLUMN primary_text TYPE VARCHAR(2000);
ALTER TABLE ad_contents ALTER COLUMN primary_text TYPE VARCHAR(2000);

-- Tăng độ dài trường name để chứa tên ads dài hơn
ALTER TABLE ads ALTER COLUMN name TYPE VARCHAR(500);

-- Tăng độ dài trường image_url để chứa đường dẫn file dài
ALTER TABLE ads ALTER COLUMN image_url TYPE VARCHAR(1000);
ALTER TABLE ad_contents ALTER COLUMN image_url TYPE VARCHAR(1000);

-- Tăng độ dài trường media_file_path
ALTER TABLE ads ALTER COLUMN media_file_path TYPE VARCHAR(1000);

-- Thêm comment để ghi nhận thay đổi
COMMENT ON COLUMN ads.headline IS 'Increased to 500 chars to accommodate AI-generated content';
COMMENT ON COLUMN ads.primary_text IS 'Increased to 2000 chars to accommodate AI-generated content';
COMMENT ON COLUMN ads.description IS 'Increased to 1000 chars to accommodate AI-generated content';
COMMENT ON COLUMN ad_contents.headline IS 'Increased to 500 chars to accommodate AI-generated content';
COMMENT ON COLUMN ad_contents.primary_text IS 'Increased to 2000 chars to accommodate AI-generated content';
COMMENT ON COLUMN ad_contents.description IS 'Increased to 1000 chars to accommodate AI-generated content';

