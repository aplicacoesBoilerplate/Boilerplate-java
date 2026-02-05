CREATE TABLE log_erros (
    id_error INT NOT NULL COMMENT 'Identificador único do erro (0 = registro sentinela)',
    erro VARCHAR(255) NOT NULL COMMENT 'Descrição resumida do erro',
    arquivo_error VARCHAR(150) COMMENT 'Arquivo onde o erro ocorreu',
    classe_error VARCHAR(150) COMMENT 'Classe Java onde o erro ocorreu',
    metodo_error VARCHAR(150) COMMENT 'Método Java onde o erro ocorreu',
    linha_error INT COMMENT 'Linha do código onde o erro ocorreu',
    hora_error DATETIME NOT NULL COMMENT 'Data e hora do erro',
    status_code_error INT COMMENT 'Status HTTP relacionado ao erro',
    CONSTRAINT pk_log_erros PRIMARY KEY (id_error)
) COMMENT='Tabela de persistência de erros da aplicação';

INSERT INTO log_erros (
    id_error,
    erro,
    arquivo_error,
    classe_error,
    metodo_error,
    linha_error,
    hora_error,
    status_code_error
) VALUES (
    0,
    'REGISTRO INICIAL',
    'N/A',
    'N/A',
    'N/A',
    NULL,
    NOW(),
    NULL
);
