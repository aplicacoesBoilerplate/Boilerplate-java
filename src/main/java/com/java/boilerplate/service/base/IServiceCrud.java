package com.java.boilerplate.service.base;

/**
 * @description Define as operações de escrita disponíveis para services que administram um recurso.
 * @template TRegistro - DTO recebido e retornado nas operações de escrita.
 */
public interface IServiceCrud<TRegistro> extends IServiceConsulta<TRegistro> {
    TRegistro cadastrar(TRegistro pRegistro);

    TRegistro editar(TRegistro pRegistro);

    TRegistro modificar(TRegistro pRegistro);

    void excluir(Long pIdRegistro);
}
