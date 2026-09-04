package com.java.boilerplate.migration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class CV8AutorizacaoAutoriaMigrationTests {
    private static final String MIGRATION = "db/migration/V8__migrar_funcionalidade_gerenciar_registros.sql";

    @Test
    void migrationConsolidaLegadoPreservaNovaConfiguracaoEPreencheAdminDeFormaIdempotente() {
        DataSource dataSource = criarBanco("migration_v8_cenarios");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        prepararCenarios(jdbc);

        executarMigrationSeExistir(dataSource);
        executarMigrationSeExistir(dataSource);

        assertThat(valorFuncionalidade(jdbc, 1L, "gerenciarRegistros")).isTrue();
        assertThat(valorFuncionalidade(jdbc, 2L, "gerenciarRegistros")).isFalse();
        assertThat(valorFuncionalidade(jdbc, 3L, "gerenciarRegistros")).isTrue();
        assertThat(contarFuncionalidade(jdbc, "gerenciarRegistrosOutros")).isZero();
        assertThat(contarFuncionalidade(jdbc, "gerenciarRegistros")).isEqualTo(3);
    }

    @Test
    void migrationNaoSobrescreveLiberacaoExplicitamenteDesativadaDoAdmin() {
        DataSource dataSource = criarBanco("migration_v8_admin_existente");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        criarSchema(jdbc);
        jdbc.update("INSERT INTO cargos_rbac (id_cargo, papel) VALUES (1, 'ADMIN')");
        jdbc.update("INSERT INTO funcionalidades_cargo_rbac (id_cargo, funcionalidade, liberado) VALUES (1, 'gerenciarRegistros', FALSE)");

        executarMigrationSeExistir(dataSource);
        executarMigrationSeExistir(dataSource);

        assertThat(valorFuncionalidade(jdbc, 1L, "gerenciarRegistros")).isFalse();
        assertThat(contarFuncionalidade(jdbc, "gerenciarRegistros")).isOne();
    }

    private DataSource criarBanco(String pNome) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + pNome + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        return dataSource;
    }

    private void prepararCenarios(JdbcTemplate pJdbc) {
        criarSchema(pJdbc);
        pJdbc.update("INSERT INTO cargos_rbac (id_cargo, papel) VALUES (1, 'LEGACY_ONLY'), (2, 'COEXIST'), (3, 'ADMIN')");
        pJdbc.update("INSERT INTO funcionalidades_cargo_rbac (id_cargo, funcionalidade, liberado) VALUES (1, 'gerenciarRegistrosOutros', TRUE)");
        pJdbc.update("INSERT INTO funcionalidades_cargo_rbac (id_cargo, funcionalidade, liberado) VALUES (2, 'gerenciarRegistrosOutros', TRUE), (2, 'gerenciarRegistros', FALSE)");
    }

    private void criarSchema(JdbcTemplate pJdbc) {
        pJdbc.execute("""
                CREATE TABLE cargos_rbac (
                    id_cargo BIGINT NOT NULL PRIMARY KEY,
                    papel VARCHAR(80) NOT NULL UNIQUE
                )
                """);
        pJdbc.execute("""
                CREATE TABLE funcionalidades_cargo_rbac (
                    id_funcionalidade BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    id_cargo BIGINT NOT NULL,
                    funcionalidade VARCHAR(120) NOT NULL,
                    liberado BOOLEAN NOT NULL DEFAULT FALSE,
                    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
                    criado_por BIGINT,
                    atualizado_em DATETIME,
                    atualizado_por BIGINT,
                    CONSTRAINT uk_funcionalidade_cargo UNIQUE (id_cargo, funcionalidade),
                    CONSTRAINT fk_funcionalidade_cargo FOREIGN KEY (id_cargo) REFERENCES cargos_rbac(id_cargo)
                )
                """);
    }

    private void executarMigrationSeExistir(DataSource pDataSource) {
        ClassPathResource migration = new ClassPathResource(MIGRATION);
        if (migration.exists()) {
            new ResourceDatabasePopulator(migration).execute(pDataSource);
        }
    }

    private boolean valorFuncionalidade(JdbcTemplate pJdbc, Long pIdCargo, String pFuncionalidade) {
        return Boolean.TRUE.equals(pJdbc.queryForObject(
                "SELECT liberado FROM funcionalidades_cargo_rbac WHERE id_cargo = ? AND funcionalidade = ?",
                Boolean.class,
                pIdCargo,
                pFuncionalidade));
    }

    private int contarFuncionalidade(JdbcTemplate pJdbc, String pFuncionalidade) {
        return pJdbc.queryForObject(
                "SELECT COUNT(*) FROM funcionalidades_cargo_rbac WHERE funcionalidade = ?",
                Integer.class,
                pFuncionalidade);
    }
}
