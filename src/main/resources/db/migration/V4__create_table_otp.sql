CREATE TABLE users_otp (
    id_user BIGINT NOT NULL COMMENT 'User identifier for password recovery',
    otp_code VARCHAR(6) NOT NULL COMMENT 'OTP code generated',
    expiry_date DATETIME NOT NULL COMMENT 'Expiration date of the generated code',

    CONSTRAINT pk_users_otp PRIMARY KEY (id_user),
    CONSTRAINT fk_otp_user FOREIGN KEY (id_user) REFERENCES users_app(id_user) ON DELETE CASCADE
) ENGINE=InnoDB, COMMENT = 'Table to separately store the OTP codes for the user to reset their password';