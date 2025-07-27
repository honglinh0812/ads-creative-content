-- Migration to increase primary_text length for longer AI-generated content
ALTER TABLE ad_contents ALTER COLUMN primary_text TYPE VARCHAR(10000); 