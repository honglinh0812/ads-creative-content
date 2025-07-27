-- Migration: Sửa kiểu dữ liệu cột daily_budget về double precision (float8) cho đúng với entity Java
ALTER TABLE campaigns
ALTER COLUMN daily_budget TYPE double precision USING daily_budget::double precision; 