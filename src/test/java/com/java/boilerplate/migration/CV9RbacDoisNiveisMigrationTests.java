package com.java.boilerplate.migration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class CV9RbacDoisNiveisMigrationTests {
    private static final String MIGRATION = "db/migration/V9__classificar_cargos_rbac_dois_niveis.sql";
    private static final String MIGRATION_SANEAMENTO = "db/migration/V10__sanear_cargos_cliente_final.sql";

    @Test
    void migrationClassificaCargosLegadosESaneiaConfiguracaoInseguraDoUsuarioFinal() {
        DataSource dataSource = criarBanco();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE cargos_rbac (
                    id_cargo BIGINT NOT NULL PRIMARY KEY,
                    papel VARCHAR(80) NOT NULL UNIQUE,
                    comportamento_padrao VARCHAR(20) NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE permissoes_cargo_rbac (
                    id_permissao BIGINT NOT NULL PRIMARY KEY,
                    id_cargo BIGINT NOT NULL,
                    recurso VARCHAR(120) NOT NULL,
                    acao VARCHAR(120) NOT NULL,
                    liberado BOOLEAN NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE funcionalidades_cargo_rbac (
                    id_funcionalidade BIGINT NOT NULL PRIMARY KEY,
                    id_cargo BIGINT NOT NULL,
                    funcionalidade VARCHAR(120) NOT NULL,
                    liberado BOOLEAN NOT NULL
                )
                """);
        jdbc.update("""
                INSERT INTO cargos_rbac (id_cargo, papel, comportamento_padrao) VALUES
                (1, 'ADMIN', 'liberar'), (2, 'USER', 'liberar'), (3, 'CUSTOM_LEGADO', 'liberar')
                """);
        jdbc.update("""
                INSERT INTO funcionalidades_cargo_rbac (id_funcionalidade, id_cargo, funcionalidade, liberado) VALUES
                (1, 2, 'gerenciarRegistros', TRUE), (2, 2, 'gerenciarRegistrosOutros', TRUE),
                (3, 1, 'gerenciarRegistros', TRUE)
                """);
        jdbc.update("""
                INSERT INTO permissoes_cargo_rbac (id_permissao, id_cargo, recurso, acao, liberado) VALUES
                (1, 2, 'api', 'POST /usuarios', TRUE),
                (2, 2, 'api', 'PUT /usuarios/**', TRUE),
                (3, 2, 'api', 'PATCH /usuarios', TRUE),
                (4, 2, 'api', 'DELETE /usuarios/**', TRUE),
                (5, 2, 'api', 'POST /rbac/cargos/**', TRUE),
                (6, 2, 'api', 'PUT /rbac/cargos', TRUE),
                (7, 2, 'api', 'PATCH /rbac/cargos/**', TRUE),
                (8, 2, 'api', 'DELETE /rbac/cargos/**', TRUE),
                (9, 2, 'api', 'POST /usuarios/consulta', TRUE),
                (10, 1, 'api', 'POST /usuarios', TRUE)
                """);

        ClassPathResource migration = new ClassPathResource(MIGRATION);
        ClassPathResource migrationSaneamento = new ClassPathResource(MIGRATION_SANEAMENTO);
        assertThat(migration.exists()).isTrue();
        assertThat(migrationSaneamento.exists()).isTrue();
        new ResourceDatabasePopulator(migration).execute(dataSource);
        new ResourceDatabasePopulator(migrationSaneamento).execute(dataSource);

        assertThat(destinadoClienteFinal(jdbc, 1L)).isFalse();
        assertThat(destinadoClienteFinal(jdbc, 2L)).isTrue();
        assertThat(destinadoClienteFinal(jdbc, 3L)).isFalse();
        assertThat(comportamentoPadrao(jdbc, 2L)).isEqualTo("bloquear");
        assertThat(contar(jdbc, "SELECT COUNT(*) FROM funcionalidades_cargo_rbac WHERE id_cargo = 2")).isZero();
        assertThat(contar(jdbc, "SELECT COUNT(*) FROM permissoes_cargo_rbac WHERE id_cargo = 2 AND acao = 'POST /usuarios'")).isZero();
        assertThat(contar(jdbc, "SELECT COUNT(*) FROM permissoes_cargo_rbac WHERE id_cargo = 2 AND acao = 'DELETE /rbac/cargos/**'")).isZero();
        assertThat(contar(jdbc, "SELECT COUNT(*) FROM permissoes_cargo_rbac WHERE id_cargo = 2")).isOne();
        assertThat(contar(jdbc, "SELECT COUNT(*) FROM permissoes_cargo_rbac WHERE id_cargo = 2 AND acao = 'POST /usuarios/consulta'")).isOne();
        assertThat(contar(jdbc, "SELECT COUNT(*) FROM funcionalidades_cargo_rbac WHERE id_cargo = 1 AND funcionalidade = 'gerenciarRegistros'")).isOne();
        assertThat(contar(jdbc, "SELECT COUNT(*) FROM permissoes_cargo_rbac WHERE id_cargo = 1 AND acao = 'POST /usuarios'")).isOne();

        new ResourceDatabasePopulator(migrationSaneamento).execute(dataSource);
        assertThat(contar(jdbc, "SELECT COUNT(*) FROM permissoes_cargo_rbac WHERE id_cargo = 2 AND acao = 'POST /usuarios/consulta'")).isOne();
        assertThat(comportamentoPadrao(jdbc, 2L)).isEqualTo("bloquear");

        jdbc.update("INSERT INTO cargos_rbac (id_cargo, papel, comportamento_padrao) VALUES (4, 'NOVO', 'bloquear')");
        assertThat(destinadoClienteFinal(jdbc, 4L)).isTrue();
    }

    private DataSource criarBanco() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:migration_v9_cenarios;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        return dataSource;
    }

    private boolean destinadoClienteFinal(JdbcTemplate pJdbc, Long pIdCargo) {
        return Boolean.TRUE.equals(pJdbc.queryForObject(
                "SELECT destinado_cliente_final FROM cargos_rbac WHERE id_cargo = ?", Boolean.class, pIdCargo));
    }

    private String comportamentoPadrao(JdbcTemplate pJdbc, Long pIdCargo) {
        return pJdbc.queryForObject("SELECT comportamento_padrao FROM cargos_rbac WHERE id_cargo = ?", String.class, pIdCargo);
    }

    private int contar(JdbcTemplate pJdbc, String pSql) {
        return pJdbc.queryForObject(pSql, Integer.class);
    }
}
