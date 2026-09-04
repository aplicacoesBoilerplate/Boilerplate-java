INSERT INTO funcionalidades_cargo_rbac (
    id_cargo,
    funcionalidade,
    liberado,
    criado_em,
    criado_por,
    atualizado_em,
    atualizado_por
)
SELECT
    legado.id_cargo,
    'gerenciarRegistros',
    legado.liberado,
    legado.criado_em,
    legado.criado_por,
    legado.atualizado_em,
    legado.atualizado_por
FROM funcionalidades_cargo_rbac legado
WHERE legado.funcionalidade = 'gerenciarRegistrosOutros'
ON DUPLICATE KEY UPDATE
    liberado = funcionalidades_cargo_rbac.liberado;

DELETE FROM funcionalidades_cargo_rbac
WHERE funcionalidade = 'gerenciarRegistrosOutros';

INSERT INTO funcionalidades_cargo_rbac (id_cargo, funcionalidade, liberado)
SELECT cargos.id_cargo, 'gerenciarRegistros', TRUE
FROM cargos_rbac cargos
WHERE cargos.papel = 'ADMIN'
ON DUPLICATE KEY UPDATE
    liberado = funcionalidades_cargo_rbac.liberado;
