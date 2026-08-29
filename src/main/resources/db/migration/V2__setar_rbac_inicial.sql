INSERT INTO cargos_rbac (
    papel,
    nome,
    icone,
    descricao,
    comportamento_padrao,
    redirecionamento_path,
    redirecionamento_name,
    redirecionamento_filtros,
    ativo
)
VALUES
    ('ADMIN', 'Administrador', 'mdi-account-tie', 'Acesso operacional completo ao boilerplate.', 'liberar', '/', 'Inicio', '[]', TRUE),
    ('USER', 'Usuário', 'mdi-account', 'Acesso básico para uso diário do sistema.', 'bloquear', '/usuarios', 'Usuarios', '[]', TRUE)
ON DUPLICATE KEY UPDATE
    nome = VALUES(nome),
    icone = VALUES(icone),
    descricao = VALUES(descricao),
    comportamento_padrao = VALUES(comportamento_padrao),
    redirecionamento_path = VALUES(redirecionamento_path),
    redirecionamento_name = VALUES(redirecionamento_name),
    ativo = VALUES(ativo);

INSERT INTO permissoes_cargo_rbac (id_cargo, recurso, acao, liberado)
SELECT cargos.id_cargo, permissoes.recurso, permissoes.acao, permissoes.liberado
FROM cargos_rbac cargos
JOIN (
    SELECT 'rotas' AS recurso, 'Inicio' AS acao, TRUE AS liberado
    UNION ALL SELECT 'rotas', 'Usuario', TRUE
    UNION ALL SELECT 'api', 'GET /usuarios/**', TRUE
    UNION ALL SELECT 'api', 'POST /usuarios/consulta', TRUE
    UNION ALL SELECT 'api', 'POST /usuarios/search', TRUE
) permissoes
WHERE cargos.papel = 'USER'
ON DUPLICATE KEY UPDATE liberado = VALUES(liberado);

INSERT INTO funcionalidades_cargo_rbac (id_cargo, funcionalidade, liberado)
SELECT cargos.id_cargo, funcionalidades.funcionalidade, TRUE
FROM cargos_rbac cargos
JOIN (
    SELECT 'exportarDados' AS funcionalidade
    UNION ALL SELECT 'visualizarGraficos'
) funcionalidades
WHERE cargos.papel = 'USER'
ON DUPLICATE KEY UPDATE liberado = VALUES(liberado);
