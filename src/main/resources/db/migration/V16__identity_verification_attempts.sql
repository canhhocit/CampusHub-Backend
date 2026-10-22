-- V16: Add identity verification attempts table for eKYC orchestration
CREATE TABLE IF NOT EXISTS identity_verification_attempts (
    id BIGSERIAL PRIMARY KEY,
    attempt_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    liveness_session_id VARCHAR(100),
    citizen_id VARCHAR(20),
    card_photo_hash VARCHAR(64),
    face_similarity_score DOUBLE PRECISION,
    face_threshold DOUBLE PRECISION,
    face_matched BOOLEAN,
    liveness_passed BOOLEAN,
    anti_spoofing_outcome VARCHAR(30),
    step_count INT DEFAULT 0,
    failure_code VARCHAR(50),
    failure_reason VARCHAR(255),
    id_card_front_url VARCHAR(500),
    selfie_url VARCHAR(500),
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_id_attempts_user_id ON identity_verification_attempts(user_id);
CREATE INDEX IF NOT EXISTS idx_id_attempts_status ON identity_verification_attempts(status);
CREATE INDEX IF NOT EXISTS idx_id_attempts_created_at ON identity_verification_attempts(created_at);
CREATE INDEX IF NOT EXISTS idx_id_attempts_citizen_id ON identity_verification_attempts(citizen_id);
