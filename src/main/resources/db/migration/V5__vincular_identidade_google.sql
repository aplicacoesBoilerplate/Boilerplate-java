ALTER TABLE usuarios
    ADD COLUMN google_subject VARCHAR(255) NULL COMMENT 'Identificador estável subject da identidade Google',
    ADD CONSTRAINT uk_usuarios_google_subject UNIQUE (google_subject);
