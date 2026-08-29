package com.java.boilerplate.service.base;

import com.java.boilerplate.dto.consulta.RConsultaRegistros;
import com.java.boilerplate.dto.consulta.RRespostaConsultaRegistros;

/**
 * @description Define as operações de leitura compartilhadas pelos services consultáveis.
 * @template TRegistro - DTO retornado pelo service.
 */
public interface IServiceConsulta<TRegistro> {
    RRespostaConsultaRegistros<TRegistro> consultar(RConsultaRegistros pConsulta);

    TRegistro buscarPorId(Long pIdRegistro);
}
