CREATE TABLE IF NOT EXISTS funcionalidades_cargo_rbac (
    id_funcionalidade BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único da funcionalidade',
    id_cargo BIGINT NOT NULL COMMENT 'Cargo proprietário da funcionalidade',
    funcionalidade VARCHAR(120) NOT NULL COMMENT 'Chave estável da funcionalidade geral',
    liberado BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Define se a funcionalidade está liberada',
    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Data de criação do registro',
    criado_por BIGINT COMMENT 'Usuário responsável pela criação do registro',
    atualizado_em DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data da última atualização do registro',
    atualizado_por BIGINT COMMENT 'Usuário responsável pela última atualização do registro',

    CONSTRAINT pk_funcionalidades_cargo_rbac PRIMARY KEY (id_funcionalidade),
    CONSTRAINT fk_funcionalidade_cargo_rbac FOREIGN KEY (id_cargo) REFERENCES cargos_rbac(id_cargo) ON DELETE CASCADE,
    CONSTRAINT uk_funcionalidade_cargo UNIQUE (id_cargo, funcionalidade),
    CONSTRAINT fk_funcionalidades_cargo_rbac_criado_por FOREIGN KEY (criado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    CONSTRAINT fk_funcionalidades_cargo_rbac_atualizado_por FOREIGN KEY (atualizado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    CONSTRAINT ck_funcionalidade_not_empty CHECK (CHAR_LENGTH(TRIM(funcionalidade)) > 0),
    INDEX idx_funcionalidades_cargo (id_cargo),
    INDEX idx_funcionalidades_cargo_rbac_criado_por (criado_por),
    INDEX idx_funcionalidades_cargo_rbac_atualizado_por (atualizado_por)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Funcionalidades gerais liberadas por cargo';

INSERT INTO funcionalidades_cargo_rbac (id_cargo, funcionalidade, liberado)
SELECT cargos.id_cargo, funcionalidades.funcionalidade, TRUE
FROM cargos_rbac cargos
JOIN (
    SELECT 'exportarDados' AS funcionalidade
    UNION ALL SELECT 'visualizarGraficos'
) funcionalidades
WHERE cargos.papel = 'USER'
ON DUPLICATE KEY UPDATE liberado = TRUE;
