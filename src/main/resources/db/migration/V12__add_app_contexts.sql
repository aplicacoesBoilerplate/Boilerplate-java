CREATE TABLE IF NOT EXISTS app_contexts (
    context_key VARCHAR(50) NOT NULL COMMENT 'Stable application context key',
    base_url VARCHAR(255) NOT NULL COMMENT 'Public base URL for this context',
    url_path VARCHAR(100) NOT NULL COMMENT 'Frontend path prefix for this context',
    match_policy VARCHAR(50) NOT NULL COMMENT 'User matching policy for this context',

    CONSTRAINT pk_app_contexts PRIMARY KEY (context_key),
    CONSTRAINT check_match_policy CHECK (match_policy IN ('OPPOSITE_GENDER', 'ANY_GENDER'))
) ENGINE=InnoDB, COMMENT = 'Application contexts supported by the shared backend';

INSERT INTO app_contexts (context_key, base_url, url_path, match_policy)
VALUES
    ('tz', '/', '/', 'OPPOSITE_GENDER'),
    ('lgbt', '/lgbt', '/lgbt', 'ANY_GENDER')
ON DUPLICATE KEY UPDATE
    base_url = VALUES(base_url),
    url_path = VALUES(url_path),
    match_policy = VALUES(match_policy);

DELIMITER //

CREATE PROCEDURE add_column_if_missing(
    IN tableName VARCHAR(64),
    IN columnName VARCHAR(64),
    IN ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND COLUMN_NAME = columnName
    ) THEN
        SET @sql = ddl;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

CREATE PROCEDURE add_index_if_missing(
    IN tableName VARCHAR(64),
    IN indexName VARCHAR(64),
    IN ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND INDEX_NAME = indexName
    ) THEN
        SET @sql = ddl;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

CREATE PROCEDURE drop_index_if_exists(
    IN tableName VARCHAR(64),
    IN indexName VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND INDEX_NAME = indexName
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', tableName, ' DROP INDEX ', indexName);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

CREATE PROCEDURE add_constraint_if_missing(
    IN tableName VARCHAR(64),
    IN constraintName VARCHAR(64),
    IN ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND CONSTRAINT_NAME = constraintName
    ) THEN
        SET @sql = ddl;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

CALL add_column_if_missing(
    'users_app',
    'context_key',
    'ALTER TABLE users_app ADD COLUMN context_key VARCHAR(50) NOT NULL DEFAULT ''tz'' COMMENT ''Application context that owns this user'''
);

ALTER TABLE users_app MODIFY user_gender VARCHAR(10) NULL;

CALL add_constraint_if_missing(
    'users_app',
    'fk_users_app_context',
    'ALTER TABLE users_app ADD CONSTRAINT fk_users_app_context FOREIGN KEY (context_key) REFERENCES app_contexts(context_key)'
);

CALL drop_index_if_exists('users_app', 'user_username');
CALL drop_index_if_exists('users_app', 'user_email');
CALL drop_index_if_exists('users_app', 'phone_number_is_unique');

CALL add_constraint_if_missing(
    'users_app',
    'uk_users_context_username',
    'ALTER TABLE users_app ADD CONSTRAINT uk_users_context_username UNIQUE (context_key, user_username)'
);

CALL add_constraint_if_missing(
    'users_app',
    'uk_users_context_email',
    'ALTER TABLE users_app ADD CONSTRAINT uk_users_context_email UNIQUE (context_key, user_email)'
);

CALL add_constraint_if_missing(
    'users_app',
    'uk_users_context_phone',
    'ALTER TABLE users_app ADD CONSTRAINT uk_users_context_phone UNIQUE (context_key, user_phone_number)'
);

CALL add_column_if_missing(
    'user_subscriptions',
    'context_key',
    'ALTER TABLE user_subscriptions ADD COLUMN context_key VARCHAR(50) NOT NULL DEFAULT ''tz'' COMMENT ''Application context for this subscription'''
);

CALL add_constraint_if_missing(
    'user_subscriptions',
    'fk_subscription_context',
    'ALTER TABLE user_subscriptions ADD CONSTRAINT fk_subscription_context FOREIGN KEY (context_key) REFERENCES app_contexts(context_key)'
);

CALL add_index_if_missing(
    'user_subscriptions',
    'idx_subscription_user',
    'CREATE INDEX idx_subscription_user ON user_subscriptions(id_user)'
);

CALL drop_index_if_exists('user_subscriptions', 'uc_subscription_user');

CALL add_constraint_if_missing(
    'user_subscriptions',
    'uc_subscription_context_user',
    'ALTER TABLE user_subscriptions ADD CONSTRAINT uc_subscription_context_user UNIQUE (context_key, id_user)'
);

CALL add_column_if_missing(
    'push_subscription',
    'context_key',
    'ALTER TABLE push_subscription ADD COLUMN context_key VARCHAR(50) NOT NULL DEFAULT ''tz'' COMMENT ''Application context for this push subscription'''
);

CALL add_constraint_if_missing(
    'push_subscription',
    'fk_push_subscription_context',
    'ALTER TABLE push_subscription ADD CONSTRAINT fk_push_subscription_context FOREIGN KEY (context_key) REFERENCES app_contexts(context_key)'
);

CALL drop_index_if_exists('push_subscription', 'endpoint');

CALL add_constraint_if_missing(
    'push_subscription',
    'uk_push_subscription_context_endpoint',
    'ALTER TABLE push_subscription ADD CONSTRAINT uk_push_subscription_context_endpoint UNIQUE (context_key, endpoint)'
);

CALL add_column_if_missing(
    'refresh_tokens',
    'context_key',
    'ALTER TABLE refresh_tokens ADD COLUMN context_key VARCHAR(50) NOT NULL DEFAULT ''tz'' COMMENT ''Application context for this refresh token'''
);

CALL add_constraint_if_missing(
    'refresh_tokens',
    'fk_refresh_token_context',
    'ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_token_context FOREIGN KEY (context_key) REFERENCES app_contexts(context_key)'
);

DROP PROCEDURE add_column_if_missing;
DROP PROCEDURE add_index_if_missing;
DROP PROCEDURE drop_index_if_exists;
DROP PROCEDURE add_constraint_if_missing;
