CREATE TABLE log_errors (
    id_error BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único do erro',
    mensagem VARCHAR(1000) NOT NULL COMMENT 'Mensagem resumida do erro',
    arquivo VARCHAR(180) COMMENT 'Arquivo onde o erro foi originado',
    classe VARCHAR(220) COMMENT 'Classe Java onde o erro foi originado',
    metodo VARCHAR(180) COMMENT 'Método onde o erro foi originado',
    linha INT COMMENT 'Linha onde o erro foi originado',
    http_status_code INT NOT NULL COMMENT 'Código HTTP associado ao erro',
    id_usuario BIGINT COMMENT 'Usuário autenticado que ocasionou o erro',
    usuario_referencia VARCHAR(150) NOT NULL DEFAULT 'SISTEMA' COMMENT 'E-mail do usuário responsável ou SISTEMA para operações internas',
    data_hora DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Data e hora do erro',

    CONSTRAINT pk_log_errors PRIMARY KEY (id_error)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de logs de erro tratados pela API';

CREATE TABLE cargos_rbac (
    id_cargo BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único do cargo',
    papel VARCHAR(80) NOT NULL COMMENT 'Chave estável do papel usado pelo RBAC',
    nome VARCHAR(120) NOT NULL COMMENT 'Nome legível do cargo',
    icone VARCHAR(80) NOT NULL COMMENT 'Ícone Material Design exibido no frontend',
    descricao VARCHAR(500) COMMENT 'Descrição curta do cargo',
    comportamento_padrao VARCHAR(20) NOT NULL COMMENT 'Comportamento quando não houver permissão explícita',
    redirecionamento_path VARCHAR(255) NOT NULL COMMENT 'Rota inicial do cargo',
    redirecionamento_name VARCHAR(120) COMMENT 'Nome técnico da rota inicial',
    redirecionamento_filtros TEXT COMMENT 'Filtros serializados aplicados no redirecionamento inicial',
    ativo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Define se o cargo pode ser atribuído',
    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Data de criação do registro',
    criado_por BIGINT COMMENT 'Usuário responsável pela criação do registro',
    atualizado_em DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data da última atualização do registro',
    atualizado_por BIGINT COMMENT 'Usuário responsável pela última atualização do registro',

    CONSTRAINT pk_cargos_rbac PRIMARY KEY (id_cargo),
    CONSTRAINT uk_cargos_rbac_papel UNIQUE (papel),
    CONSTRAINT ck_cargos_rbac_comportamento CHECK (comportamento_padrao IN ('bloquear', 'liberar')),
    CONSTRAINT ck_cargos_rbac_papel_not_empty CHECK (CHAR_LENGTH(TRIM(papel)) > 0),
    CONSTRAINT ck_cargos_rbac_nome_not_empty CHECK (CHAR_LENGTH(TRIM(nome)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cargos e regras base do RBAC';

CREATE TABLE permissoes_cargo_rbac (
    id_permissao BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único da permissão',
    id_cargo BIGINT NOT NULL COMMENT 'Cargo proprietário da permissão',
    recurso VARCHAR(120) NOT NULL COMMENT 'Recurso protegido',
    acao VARCHAR(120) NOT NULL COMMENT 'Ação protegida dentro do recurso',
    liberado BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Define se a ação está liberada',
    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Data de criação do registro',
    criado_por BIGINT COMMENT 'Usuário responsável pela criação do registro',
    atualizado_em DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data da última atualização do registro',
    atualizado_por BIGINT COMMENT 'Usuário responsável pela última atualização do registro',

    CONSTRAINT pk_permissoes_cargo_rbac PRIMARY KEY (id_permissao),
    CONSTRAINT fk_permissao_cargo_rbac FOREIGN KEY (id_cargo) REFERENCES cargos_rbac(id_cargo) ON DELETE CASCADE,
    CONSTRAINT uk_permissao_cargo_recurso_acao UNIQUE (id_cargo, recurso, acao),
    CONSTRAINT ck_permissao_recurso_not_empty CHECK (CHAR_LENGTH(TRIM(recurso)) > 0),
    CONSTRAINT ck_permissao_acao_not_empty CHECK (CHAR_LENGTH(TRIM(acao)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Permissões explícitas por cargo';

CREATE TABLE funcionalidades_cargo_rbac (
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
    CONSTRAINT ck_funcionalidade_not_empty CHECK (CHAR_LENGTH(TRIM(funcionalidade)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Funcionalidades gerais liberadas por cargo';

CREATE TABLE usuarios (
    id_usuario BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único do usuário',
    nome VARCHAR(120) NOT NULL COMMENT 'Nome exibido na interface',
    email VARCHAR(150) NOT NULL COMMENT 'E-mail usado para autenticação',
    senha VARCHAR(255) NOT NULL COMMENT 'Senha criptografada do usuário',
    avatar VARCHAR(500) COMMENT 'URL ou referência visual do avatar',
    telefone VARCHAR(30) COMMENT 'Telefone opcional do usuário',
    notificar BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Define se o usuário aceita notificações',
    ativo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Define se o usuário pode autenticar',
    id_cargo BIGINT NOT NULL COMMENT 'Cargo RBAC associado ao usuário',
    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Data de criação do usuário',
    criado_por BIGINT COMMENT 'Usuário responsável pela criação do usuário',
    atualizado_em DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data da última atualização do usuário',
    atualizado_por BIGINT COMMENT 'Usuário responsável pela última atualização do usuário',

    CONSTRAINT pk_usuarios PRIMARY KEY (id_usuario),
    CONSTRAINT fk_usuario_cargo FOREIGN KEY (id_cargo) REFERENCES cargos_rbac(id_cargo),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_nome_not_empty CHECK (CHAR_LENGTH(TRIM(nome)) > 0),
    CONSTRAINT ck_usuarios_email_not_empty CHECK (CHAR_LENGTH(TRIM(email)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Usuários operacionais do boilerplate';

CREATE TABLE preferencias_usuario (
    id_preferencia BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único da preferência',
    id_usuario BIGINT NOT NULL COMMENT 'Usuário proprietário da preferência',
    contexto VARCHAR(120) NOT NULL COMMENT 'Contexto funcional da preferência ou filtro',
    chave VARCHAR(120) NOT NULL COMMENT 'Chave da preferência dentro do contexto',
    valor_json TEXT NOT NULL COMMENT 'Valor serializado em JSON para preservar estruturas flexíveis',
    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Data de criação do registro',
    criado_por BIGINT COMMENT 'Usuário responsável pela criação do registro',
    atualizado_em DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data da última atualização do registro',
    atualizado_por BIGINT COMMENT 'Usuário responsável pela última atualização do registro',

    CONSTRAINT pk_preferencias_usuario PRIMARY KEY (id_preferencia),
    CONSTRAINT fk_preferencia_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT uk_preferencia_usuario_contexto_chave UNIQUE (id_usuario, contexto, chave),
    CONSTRAINT ck_preferencias_contexto_not_empty CHECK (CHAR_LENGTH(TRIM(contexto)) > 0),
    CONSTRAINT ck_preferencias_chave_not_empty CHECK (CHAR_LENGTH(TRIM(chave)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Preferências, filtros e ajustes persistidos por usuário';

CREATE TABLE refresh_tokens (
    id_usuario BIGINT NOT NULL COMMENT 'Usuário proprietário do refresh token',
    token_hash VARCHAR(64) NOT NULL COMMENT 'Hash SHA-256 do refresh token',
    expira_em DATETIME NOT NULL COMMENT 'Data de expiração do refresh token',

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id_usuario),
    CONSTRAINT fk_refresh_token_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Refresh tokens persistidos por usuário';

CREATE TABLE usuarios_otp (
    id_usuario BIGINT NOT NULL COMMENT 'Usuário proprietário do OTP',
    codigo VARCHAR(6) NOT NULL COMMENT 'Código temporário de validação',
    expira_em DATETIME NOT NULL COMMENT 'Data de expiração do código',
    utilizado BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Controla se o código já foi consumido',

    CONSTRAINT pk_usuarios_otp PRIMARY KEY (id_usuario),
    CONSTRAINT fk_otp_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Códigos OTP para recuperação de senha';

CREATE TABLE solicitacoes_acesso (
    id_solicitacao BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único da solicitação',
    id_usuario BIGINT NOT NULL COMMENT 'Usuário criado inativo para a solicitação de acesso',
    liberado BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Define se a primeira liberação de acesso já foi concedida',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' COMMENT 'Status administrativo da solicitação',
    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Data de criação da solicitação',
    criado_por BIGINT COMMENT 'Usuário responsável pela criação da solicitação',
    atualizado_em DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data da última atualização da solicitação',
    atualizado_por BIGINT COMMENT 'Usuário responsável pela última atualização da solicitação',

    CONSTRAINT pk_solicitacoes_acesso PRIMARY KEY (id_solicitacao),
    CONSTRAINT fk_solicitacao_acesso_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    CONSTRAINT uk_solicitacoes_acesso_usuario UNIQUE (id_usuario),
    CONSTRAINT ck_solicitacoes_status CHECK (status IN ('PENDENTE', 'APROVADA', 'RECUSADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Solicitações públicas de acesso ao sistema';

CREATE INDEX idx_log_errors_usuario ON log_errors(id_usuario);
CREATE INDEX idx_log_errors_data_hora ON log_errors(data_hora);
CREATE INDEX idx_usuarios_id_cargo ON usuarios(id_cargo);
CREATE INDEX idx_usuarios_ativo ON usuarios(ativo);
CREATE INDEX idx_permissoes_cargo_recurso ON permissoes_cargo_rbac(id_cargo, recurso);
CREATE INDEX idx_funcionalidades_cargo ON funcionalidades_cargo_rbac(id_cargo);
CREATE INDEX idx_preferencias_usuario_contexto ON preferencias_usuario(id_usuario, contexto);
CREATE INDEX idx_solicitacoes_usuario_status ON solicitacoes_acesso(id_usuario, status, liberado);
CREATE INDEX idx_solicitacoes_liberado ON solicitacoes_acesso(liberado);
CREATE INDEX idx_cargos_rbac_criado_por ON cargos_rbac(criado_por);
CREATE INDEX idx_cargos_rbac_atualizado_por ON cargos_rbac(atualizado_por);
CREATE INDEX idx_permissoes_cargo_rbac_criado_por ON permissoes_cargo_rbac(criado_por);
CREATE INDEX idx_permissoes_cargo_rbac_atualizado_por ON permissoes_cargo_rbac(atualizado_por);
CREATE INDEX idx_funcionalidades_cargo_rbac_criado_por ON funcionalidades_cargo_rbac(criado_por);
CREATE INDEX idx_funcionalidades_cargo_rbac_atualizado_por ON funcionalidades_cargo_rbac(atualizado_por);
CREATE INDEX idx_usuarios_criado_por ON usuarios(criado_por);
CREATE INDEX idx_usuarios_atualizado_por ON usuarios(atualizado_por);
CREATE INDEX idx_preferencias_usuario_criado_por ON preferencias_usuario(criado_por);
CREATE INDEX idx_preferencias_usuario_atualizado_por ON preferencias_usuario(atualizado_por);
CREATE INDEX idx_solicitacoes_acesso_criado_por ON solicitacoes_acesso(criado_por);
CREATE INDEX idx_solicitacoes_acesso_atualizado_por ON solicitacoes_acesso(atualizado_por);

ALTER TABLE log_errors
    ADD CONSTRAINT fk_log_errors_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL;

ALTER TABLE cargos_rbac
    ADD CONSTRAINT fk_cargos_rbac_criado_por FOREIGN KEY (criado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    ADD CONSTRAINT fk_cargos_rbac_atualizado_por FOREIGN KEY (atualizado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL;

ALTER TABLE permissoes_cargo_rbac
    ADD CONSTRAINT fk_permissoes_cargo_rbac_criado_por FOREIGN KEY (criado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    ADD CONSTRAINT fk_permissoes_cargo_rbac_atualizado_por FOREIGN KEY (atualizado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL;

ALTER TABLE funcionalidades_cargo_rbac
    ADD CONSTRAINT fk_funcionalidades_cargo_rbac_criado_por FOREIGN KEY (criado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    ADD CONSTRAINT fk_funcionalidades_cargo_rbac_atualizado_por FOREIGN KEY (atualizado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL;

ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_criado_por FOREIGN KEY (criado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    ADD CONSTRAINT fk_usuarios_atualizado_por FOREIGN KEY (atualizado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL;

ALTER TABLE preferencias_usuario
    ADD CONSTRAINT fk_preferencias_usuario_criado_por FOREIGN KEY (criado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    ADD CONSTRAINT fk_preferencias_usuario_atualizado_por FOREIGN KEY (atualizado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL;

ALTER TABLE solicitacoes_acesso
    ADD CONSTRAINT fk_solicitacoes_acesso_criado_por FOREIGN KEY (criado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    ADD CONSTRAINT fk_solicitacoes_acesso_atualizado_por FOREIGN KEY (atualizado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL;
