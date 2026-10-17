-- Preserve all submitted identity data and legacy verification flags.
-- Legacy self-verification is not evidence of a human review: approval is
-- effective only after a reviewer explicitly populates this new timestamp.
ALTER TABLE users ADD COLUMN identity_reviewed_at TIMESTAMP;
