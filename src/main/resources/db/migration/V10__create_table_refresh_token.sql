CREATE TABLE refresh_tokens (
    id_user BIGINT NOT NULL COMMENT 'Relationship with the user and Primary Key',
    token_hash VARCHAR(64) NOT NULL UNIQUE COMMENT 'SHA-256 hash of the refresh token',
    expiry_date DATETIME NOT NULL COMMENT 'Expiration date of the token',

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id_user),
    CONSTRAINT fk_rt_user FOREIGN KEY (id_user) REFERENCES users_app(id_user) ON DELETE CASCADE
) ENGINE=InnoDB, COMMENT = 'Table to store refresh tokens hashes (1:1 with user)';