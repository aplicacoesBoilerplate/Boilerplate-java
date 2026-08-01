package com.java.boilerplate.integration.softwarecenter;

import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RCadastro;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RCriarSessaoGoogle;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RCriarSessaoSenha;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RRedefinirSenha;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RSessaoCriada;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RSessaoValidada;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RSolicitarRecuperacao;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RVerificarRecuperacao;

/**
 * @description Contrato do cliente privado que comunica o BFF com a Software Center.
 */
public interface ISoftwareCenterClient {
    RSessaoCriada criarSessaoComSenha(RCriarSessaoSenha pComando);

    RSessaoCriada criarSessaoComGoogle(RCriarSessaoGoogle pComando);

    RSessaoValidada revalidarSessao(String pSessionId);

    void revogarSessao(String pSessionId);

    void cadastrar(RCadastro pComando);

    void solicitarRecuperacao(RSolicitarRecuperacao pComando);

    void verificarRecuperacao(RVerificarRecuperacao pComando);

    void redefinirSenha(RRedefinirSenha pComando);
}
