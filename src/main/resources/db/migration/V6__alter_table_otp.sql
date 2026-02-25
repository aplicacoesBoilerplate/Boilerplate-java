ALTER TABLE users_otp ADD COLUMN used BOOLEAN DEFAULT false COMMENT 'Controls the security of using the OTP code, preventing the same code from being used twice';
