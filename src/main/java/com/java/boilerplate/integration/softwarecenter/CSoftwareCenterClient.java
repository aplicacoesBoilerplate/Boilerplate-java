package com.java.boilerplate.integration.softwarecenter;

import com.java.boilerplate.config.RSoftwareCenterProperties;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RCadastro;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RCriarSessaoGoogle;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RCriarSessaoSenha;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RRedefinirSenha;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RSessaoCriada;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RSessaoValidada;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RSolicitarRecuperacao;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RVerificarRecuperacao;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * @description Cliente RestClient para a Software Center que conserva a credencial técnica somente no servidor.
 */
@Service
public class CSoftwareCenterClient implements ISoftwareCenterClient {
    private final RestClient restClient;
    private final RSoftwareCenterProperties properties;

    public CSoftwareCenterClient(RSoftwareCenterProperties pProperties) {
        this.properties = pProperties;
        this.restClient = RestClient.builder()
                .baseUrl(obterBaseUrl(pProperties))
                .requestFactory(criarRequestFactory(pProperties))
                .build();
    }

    @Override
    public RSessaoCriada criarSessaoComSenha(RCriarSessaoSenha pComando) {
        return validarRespostaSessao(executar(() -> restClient
                .post()
                .uri("/api/v1/integracoes/sessoes")
                .headers(this::adicionarAutorizacaoBff)
                .contentType(MediaType.APPLICATION_JSON)
                .body(pComando)
                .retrieve()
                .body(RSessaoCriada.class)));
    }

    @Override
    public RSessaoCriada criarSessaoComGoogle(RCriarSessaoGoogle pComando) {
        return validarRespostaSessao(executar(() -> restClient
                .post()
                .uri("/api/v1/integracoes/sessoes/google")
                .headers(this::adicionarAutorizacaoBff)
                .contentType(MediaType.APPLICATION_JSON)
                .body(pComando)
                .retrieve()
                .body(RSessaoCriada.class)));
    }

    @Override
    public RSessaoValidada revalidarSessao(String pSessionId) {
        RSessaoValidada resposta = executar(() -> restClient
                .get()
                .uri("/api/v1/integracoes/sessoes/{sessionId}", pSessionId)
                .headers(this::adicionarAutorizacaoBff)
                .retrieve()
                .body(RSessaoValidada.class));

        if (resposta == null || resposta.context() == null || resposta.expiresAt() == null) {
            throw new CSoftwareCenterClientException(HttpStatus.BAD_GATEWAY, "Resposta de sessão inválida");
        }

        return resposta;
    }

    @Override
    public void revogarSessao(String pSessionId) {
        executar(() -> {
            restClient
                    .delete()
                    .uri("/api/v1/integracoes/sessoes/{sessionId}", pSessionId)
                    .headers(this::adicionarAutorizacaoBff)
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    @Override
    public void cadastrar(RCadastro pComando) {
        executarComandoPublico("/api/v1/public/cadastro", pComando);
    }

    @Override
    public void solicitarRecuperacao(RSolicitarRecuperacao pComando) {
        executarComandoPublico("/api/v1/public/recuperacoes-senha", pComando);
    }

    @Override
    public void verificarRecuperacao(RVerificarRecuperacao pComando) {
        executarComandoPublico("/api/v1/public/recuperacoes-senha/verificacoes", pComando);
    }

    @Override
    public void redefinirSenha(RRedefinirSenha pComando) {
        executarComandoPublico("/api/v1/public/redefinicoes-senha", pComando);
    }

    private void executarComandoPublico(String pUri, Object pComando) {
        executar(() -> {
            restClient
                    .post()
                    .uri(pUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(pComando)
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    private RSessaoCriada validarRespostaSessao(RSessaoCriada pResposta) {
        if (pResposta == null
                || !StringUtils.hasText(pResposta.sessionId())
                || pResposta.context() == null
                || pResposta.expiresAt() == null) {
            throw new CSoftwareCenterClientException(HttpStatus.BAD_GATEWAY, "Resposta de sessão inválida");
        }

        return pResposta;
    }

    private <T> T executar(Supplier<T> pOperacao) {
        try {
            return pOperacao.get();
        } catch (CSoftwareCenterClientException pException) {
            throw pException;
        } catch (RestClientResponseException pException) {
            throw mapearResposta(pException.getStatusCode());
        } catch (RestClientException pException) {
            throw new CSoftwareCenterClientException(HttpStatus.SERVICE_UNAVAILABLE, "Software Center indisponível");
        } catch (RuntimeException pException) {
            throw new CSoftwareCenterClientException(HttpStatus.SERVICE_UNAVAILABLE, "Software Center indisponível");
        }
    }

    private void adicionarAutorizacaoBff(HttpHeaders pHeaders) {
        if (!StringUtils.hasText(properties.bffClientId()) || !StringUtils.hasText(properties.bffClientSecret())) {
            throw new CSoftwareCenterClientException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Credencial técnica da Software Center não configurada");
        }

        pHeaders.setBasicAuth(properties.bffClientId(), properties.bffClientSecret());
    }

    private CSoftwareCenterClientException mapearResposta(HttpStatusCode pStatus) {
        return switch (pStatus.value()) {
            case 400 -> new CSoftwareCenterClientException(HttpStatus.BAD_REQUEST, "Solicitação inválida");
            case 401 ->
                new CSoftwareCenterClientException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas ou sessão expirada");
            case 403 -> new CSoftwareCenterClientException(HttpStatus.FORBIDDEN, "Acesso à aplicação não autorizado");
            case 429 ->
                new CSoftwareCenterClientException(HttpStatus.TOO_MANY_REQUESTS, "Limite de tentativas atingido");
            default ->
                new CSoftwareCenterClientException(HttpStatus.SERVICE_UNAVAILABLE, "Software Center indisponível");
        };
    }

    private static String obterBaseUrl(RSoftwareCenterProperties pProperties) {
        if (!StringUtils.hasText(pProperties.baseUrl())) {
            throw new IllegalStateException("A propriedade software-center.base-url deve ser informada");
        }

        return pProperties.baseUrl();
    }

    private static SimpleClientHttpRequestFactory criarRequestFactory(RSoftwareCenterProperties pProperties) {
        int timeoutEmMillis = Math.toIntExact(pProperties.obterTimeout().toMillis());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutEmMillis);
        requestFactory.setReadTimeout(timeoutEmMillis);
        return requestFactory;
    }
}
