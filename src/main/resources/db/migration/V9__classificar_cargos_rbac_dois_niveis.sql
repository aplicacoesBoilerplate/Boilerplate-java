ALTER TABLE cargos_rbac
    ADD COLUMN destinado_cliente_final BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'Indica se o cargo e terminal para Cliente Final';

UPDATE cargos_rbac
SET destinado_cliente_final = CASE
    WHEN papel = 'USER' THEN TRUE
    ELSE FALSE
END;
