INSERT INTO permissoes_cargo_rbac (id_cargo, recurso, acao, liberado)
SELECT cargos.id_cargo, permissoes.recurso, permissoes.acao, TRUE
FROM cargos_rbac cargos
JOIN (
    SELECT 'rotas' AS recurso, 'Usuarios' AS acao
    UNION ALL SELECT 'api', 'GET /usuarios/**'
    UNION ALL SELECT 'api', 'POST /usuarios/consulta'
    UNION ALL SELECT 'api', 'POST /usuarios/search'
) permissoes
WHERE cargos.papel = 'USER'
ON DUPLICATE KEY UPDATE liberado = VALUES(liberado);
