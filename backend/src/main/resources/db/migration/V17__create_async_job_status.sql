-- Migration V17: Create async_job_status table
-- This migration creates the table for tracking asynchronous job processing

CREATE TABLE IF NOT EXISTS async_job_status (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0,
    total_steps INTEGER,
    current_step VARCHAR(255),
    result_data TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,

    -- Foreign key to users table
    CONSTRAINT fk_async_job_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

-- Create indexes for performance (these are referenced in V11)
CREATE INDEX IF NOT EXISTS idx_async_job_status_job_id ON async_job_status(job_id);
CREATE INDEX IF NOT EXISTS idx_async_job_status_user_id ON async_job_status(user_id);
CREATE INDEX IF NOT EXISTS idx_async_job_status_status ON async_job_status(status);
CREATE INDEX IF NOT EXISTS idx_async_job_status_job_type ON async_job_status(job_type);
CREATE INDEX IF NOT EXISTS idx_async_job_user_status ON async_job_status(user_id, status);
CREATE INDEX IF NOT EXISTS idx_async_job_user_created ON async_job_status(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_async_job_status_created ON async_job_status(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_async_job_expires_at ON async_job_status(expires_at);
CREATE INDEX IF NOT EXISTS idx_async_job_cleanup ON async_job_status(status, expires_at)
    WHERE status IN ('PENDING', 'IN_PROGRESS', 'EXPIRED');
CREATE INDEX IF NOT EXISTS idx_async_job_active_count ON async_job_status(user_id, status)
    WHERE status IN ('PENDING', 'IN_PROGRESS');

-- Add comments
COMMENT ON TABLE async_job_status IS 'Tracks status and progress of asynchronous jobs';
COMMENT ON COLUMN async_job_status.job_id IS 'Unique identifier for the job (UUID)';
COMMENT ON COLUMN async_job_status.status IS 'Job status: PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED, EXPIRED';
COMMENT ON COLUMN async_job_status.job_type IS 'Type of job: AD_CONTENT_GENERATION, IMAGE_GENERATION, IMAGE_ENHANCEMENT, BULK_AD_GENERATION';
COMMENT ON COLUMN async_job_status.progress IS 'Progress percentage (0-100)';
COMMENT ON COLUMN async_job_status.expires_at IS 'When the job result expires (default 24 hours)';

-- Create trigger for auto-updating updated_at
CREATE OR REPLACE FUNCTION update_async_job_status_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_async_job_status_updated_at
    BEFORE UPDATE ON async_job_status
    FOR EACH ROW
    EXECUTE FUNCTION update_async_job_status_updated_at();
