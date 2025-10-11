-- Migration V14: Create personas table
-- Purpose: Support target audience persona management for AI ad generation
-- Security: Enforces user ownership via foreign key and indexes
-- Performance: Indexed on user_id and created_at for efficient queries

-- Create personas table
CREATE TABLE IF NOT EXISTS personas (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INTEGER NOT NULL CHECK (age >= 13 AND age <= 120),
    gender VARCHAR(20) NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'ALL')),
    tone VARCHAR(20) NOT NULL CHECK (tone IN ('professional', 'funny', 'casual', 'friendly', 'formal', 'enthusiastic')),
    desired_outcome VARCHAR(500),
    description VARCHAR(1000),
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint for user ownership
    CONSTRAINT fk_persona_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,

    -- Unique constraint for persona name per user
    CONSTRAINT uk_persona_name_user UNIQUE (name, user_id)
);

-- Create persona_interests junction table
CREATE TABLE IF NOT EXISTS persona_interests (
    persona_id BIGINT NOT NULL,
    interest VARCHAR(100) NOT NULL,

    CONSTRAINT fk_persona_interests_persona FOREIGN KEY (persona_id)
        REFERENCES personas(id) ON DELETE CASCADE
);

-- Create persona_pain_points junction table
CREATE TABLE IF NOT EXISTS persona_pain_points (
    persona_id BIGINT NOT NULL,
    pain_point VARCHAR(500) NOT NULL,

    CONSTRAINT fk_persona_pain_points_persona FOREIGN KEY (persona_id)
        REFERENCES personas(id) ON DELETE CASCADE
);

-- Create indexes for performance optimization
-- Index on user_id for fast user persona lookups (most common query)
CREATE INDEX IF NOT EXISTS idx_persona_user ON personas(user_id);

-- Index on created_at for sorting and analytics
CREATE INDEX IF NOT EXISTS idx_persona_created ON personas(created_at DESC);

-- Index on tone for filtering personas by tone
CREATE INDEX IF NOT EXISTS idx_persona_tone ON personas(tone);

-- Composite index for user + name searches
CREATE INDEX IF NOT EXISTS idx_persona_user_name ON personas(user_id, name);

-- Indexes for junction tables to optimize joins
CREATE INDEX IF NOT EXISTS idx_persona_interests ON persona_interests(persona_id);
CREATE INDEX IF NOT EXISTS idx_persona_pain_points ON persona_pain_points(persona_id);

-- Add comments for documentation
COMMENT ON TABLE personas IS 'Target audience personas for AI-driven ad generation';
COMMENT ON COLUMN personas.name IS 'User-defined persona name';
COMMENT ON COLUMN personas.age IS 'Target age (13-120, COPPA compliant)';
COMMENT ON COLUMN personas.gender IS 'Target gender (MALE, FEMALE, ALL)';
COMMENT ON COLUMN personas.tone IS 'Preferred communication tone for ads';
COMMENT ON COLUMN personas.desired_outcome IS 'What the persona wants to achieve';
COMMENT ON COLUMN personas.description IS 'Optional detailed persona description';
COMMENT ON COLUMN personas.user_id IS 'Owner of this persona';
COMMENT ON COLUMN personas.created_at IS 'Timestamp when persona was created';
COMMENT ON COLUMN personas.updated_at IS 'Timestamp of last update';

COMMENT ON TABLE persona_interests IS 'Persona interests (many-to-many relationship)';
COMMENT ON TABLE persona_pain_points IS 'Persona pain points (many-to-many relationship)';
