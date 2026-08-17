UPDATE usuarios_otp
SET utilizado = TRUE;

UPDATE usuarios
SET ativo = FALSE
WHERE LOWER(email) = 'boilerplate@gmail.com';

ALTER TABLE usuarios_otp
    CHANGE COLUMN codigo codigo_hash CHAR(64) NOT NULL COMMENT 'HMAC SHA-256 do código temporário',
    ADD COLUMN tentativas INT NOT NULL DEFAULT 0 COMMENT 'Quantidade de tentativas inválidas';

DELETE permissoes
FROM permissoes_cargo_rbac permissoes
JOIN cargos_rbac cargos ON cargos.id_cargo = permissoes.id_cargo
WHERE cargos.papel = 'USER'
  AND permissoes.recurso = 'api'
  AND permissoes.acao IN ('GET /usuarios/**', 'POST /usuarios/consulta', 'POST /usuarios/search');
