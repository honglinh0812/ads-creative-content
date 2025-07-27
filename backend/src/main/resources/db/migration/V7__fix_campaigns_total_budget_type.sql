-- Migration: Sửa kiểu dữ liệu cột total_budget về double precision (float8) cho đúng với entity Java
ALTER TABLE campaigns
ALTER COLUMN total_budget TYPE double precision USING total_budget::double precision; 