ALTER TABLE users_app
ADD CONSTRAINT phone_number_is_unique UNIQUE(user_phone_number);

ALTER TABLE users_app
ADD COLUMN email_hash VARCHAR(64) COMMENT 'SHA-256 hash of the original email to prevent trial abuse';
CREATE INDEX idx_email_hash ON users_app(email_hash);