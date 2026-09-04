UPDATE cargos_rbac
SET comportamento_padrao = 'bloquear'
WHERE destinado_cliente_final = TRUE;

DELETE FROM funcionalidades_cargo_rbac
WHERE id_cargo IN (
    SELECT id_cargo
    FROM cargos_rbac
    WHERE destinado_cliente_final = TRUE
)
AND funcionalidade IN ('gerenciarRegistros', 'gerenciarRegistrosOutros');

DELETE FROM permissoes_cargo_rbac
WHERE id_cargo IN (
    SELECT id_cargo
    FROM cargos_rbac
    WHERE destinado_cliente_final = TRUE
)
AND recurso = 'api'
AND acao IN (
    'POST /usuarios',
    'POST /usuarios/**',
    'PUT /usuarios',
    'PUT /usuarios/**',
    'PATCH /usuarios',
    'PATCH /usuarios/**',
    'DELETE /usuarios/*',
    'DELETE /usuarios/**',
    'DELETE /usuarios/{pIdUsuario}',
    'POST /rbac/cargos',
    'POST /rbac/cargos/**',
    'PUT /rbac/cargos',
    'PUT /rbac/cargos/**',
    'PATCH /rbac/cargos',
    'PATCH /rbac/cargos/**',
    'DELETE /rbac/cargos/*',
    'DELETE /rbac/cargos/**',
    'DELETE /rbac/cargos/{pIdCargo}'
);
