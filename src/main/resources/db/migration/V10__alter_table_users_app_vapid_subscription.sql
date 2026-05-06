ALTER TABLE users_app
ADD COLUMN push_subscription TEXT COMMENT 'JSON string containing endpoint and keys for push notifications';