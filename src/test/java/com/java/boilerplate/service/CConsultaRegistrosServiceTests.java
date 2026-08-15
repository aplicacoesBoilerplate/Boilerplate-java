package com.java.boilerplate.service;

import com.java.boilerplate.dto.consulta.RConsultaRegistros;
import com.java.boilerplate.dto.consulta.RRespostaConsultaRegistros;
import com.java.boilerplate.dto.errors.RLogErro;
import com.java.boilerplate.dto.filtros.RFiltroConsulta;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CLogErro;
import com.java.boilerplate.repository.ILogErroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class CConsultaRegistrosServiceTests {
    @Autowired
    private CLogErroService logErroService;

    @Autowired
    private ILogErroRepository logErroRepository;

    @BeforeEach
    void prepararCenario() {
        logErroRepository.deleteAll();
        salvarLog("Erro A");
        salvarLog("Erro B");
    }

    @Test
    void consultarDeveManterContratoDoFrontendEAvancarPeloCursorNumerico() {
        RConsultaRegistros primeiraConsulta = new RConsultaRegistros(List.of(), "asc", 1, null, true);

        RRespostaConsultaRegistros<RLogErro> primeiraResposta = logErroService.consultar(primeiraConsulta);
        RRespostaConsultaRegistros<RLogErro> segundaResposta = logErroService.consultar(new RConsultaRegistros(
                primeiraResposta.filtros(),
                primeiraResposta.ordenacao(),
                primeiraResposta.limite(),
                primeiraResposta.proximaEntrada(),
                primeiraResposta.possuiMais()
        ));

        assertThat(primeiraResposta.filtros()).isEmpty();
        assertThat(primeiraResposta.ordenacao()).isEqualTo("asc");
        assertThat(primeiraResposta.limite()).isEqualTo(1);
        assertThat(primeiraResposta.registros()).hasSize(1);
        assertThat(primeiraResposta.possuiMais()).isTrue();
        assertThat(primeiraResposta.proximaEntrada()).isNotNull();
        assertThat(segundaResposta.registros()).hasSize(1);
        assertThat(segundaResposta.registros().get(0).idErro())
                .isNotEqualTo(primeiraResposta.registros().get(0).idErro());
        assertThat(segundaResposta.possuiMais()).isFalse();
        assertThat(segundaResposta.proximaEntrada()).isNull();
    }

    @Test
    void consultarDeveRejeitarCampoDeFiltroNaoPermitido() {
        RConsultaRegistros consulta = new RConsultaRegistros(
                List.of(new RFiltroConsulta("senha", "igual", "segredo", null, null, null)),
                "asc",
                10,
                null,
                true
        );

        assertThatThrownBy(() -> logErroService.consultar(consulta))
                .isInstanceOf(CExceptionsSystem.class)
                .extracting(pErro -> ((CExceptionsSystem) pErro).getStatus().value())
                .isEqualTo(400);
    }

    @Test
    void consultarDeveAplicarComparacaoNumericaSemCastCru() {
        RConsultaRegistros consulta = new RConsultaRegistros(
                List.of(new RFiltroConsulta("httpStatusCode", "maiorQue", 499, null, null, null)),
                "asc",
                10,
                null,
                true
        );

        RRespostaConsultaRegistros<RLogErro> resposta = logErroService.consultar(consulta);

        assertThat(resposta.registros()).hasSize(2);
        assertThat(resposta.registros()).allSatisfy(pErro -> assertThat(pErro.httpStatusCode()).isGreaterThan(499));
    }

    @Test
    void consultarDeveRejeitarMaisDeDezFiltros() {
        List<RFiltroConsulta> filtros = IntStream.range(0, 11)
                .mapToObj(pIndice -> new RFiltroConsulta("httpStatusCode", "maiorQue", pIndice, null, null, null))
                .toList();

        assertThatThrownBy(() -> logErroService.consultar(new RConsultaRegistros(filtros, "asc", 10, null, true)))
                .isInstanceOf(CExceptionsSystem.class)
                .hasMessageContaining("10 filtros");
    }

    @Test
    void consultarDeveRejeitarMaisDeCemValoresSelecionados() {
        List<Object> valores = IntStream.range(0, 101).boxed().map(pValor -> (Object) pValor).toList();
        RFiltroConsulta filtro = new RFiltroConsulta("idErro", "selecao", null, null, null, valores);

        assertThatThrownBy(() -> logErroService.consultar(new RConsultaRegistros(List.of(filtro), "asc", 10, null, true)))
                .isInstanceOf(CExceptionsSystem.class)
                .hasMessageContaining("100 valores");
    }

    @Test
    void consultarDeveRejeitarValorTextualAcimaDe256Caracteres() {
        RFiltroConsulta filtro = new RFiltroConsulta("mensagem", "contem", "x".repeat(257), null, null, null);

        assertThatThrownBy(() -> logErroService.consultar(new RConsultaRegistros(List.of(filtro), "asc", 10, null, true)))
                .isInstanceOf(CExceptionsSystem.class)
                .hasMessageContaining("256 caracteres");
    }

    private void salvarLog(String pMensagem) {
        CLogErro logErro = new CLogErro();
        logErro.setMensagem(pMensagem);
        logErro.setHttpStatusCode(500);
        logErro.setUsuarioReferencia("SISTEMA");
        logErroRepository.saveAndFlush(logErro);
    }
}
