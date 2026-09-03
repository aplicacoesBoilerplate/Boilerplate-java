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

    @Test
    void migrationClassificaCargosLegadosEPreservaNovoPadraoTerminal() {
        DataSource dataSource = criarBanco();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE cargos_rbac (
                    id_cargo BIGINT NOT NULL PRIMARY KEY,
                    papel VARCHAR(80) NOT NULL UNIQUE
                )
                """);
        jdbc.update("INSERT INTO cargos_rbac (id_cargo, papel) VALUES (1, 'ADMIN'), (2, 'USER'), (3, 'CUSTOM_LEGADO')");

        ClassPathResource migration = new ClassPathResource(MIGRATION);
        assertThat(migration.exists()).isTrue();
        new ResourceDatabasePopulator(migration).execute(dataSource);

        assertThat(destinadoClienteFinal(jdbc, 1L)).isFalse();
        assertThat(destinadoClienteFinal(jdbc, 2L)).isTrue();
        assertThat(destinadoClienteFinal(jdbc, 3L)).isFalse();

        jdbc.update("INSERT INTO cargos_rbac (id_cargo, papel) VALUES (4, 'NOVO')");
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
}
