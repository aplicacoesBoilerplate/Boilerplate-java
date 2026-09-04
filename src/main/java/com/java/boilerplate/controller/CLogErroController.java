package com.java.boilerplate.controller;

import com.java.boilerplate.annotation.EndpointRbac;
import com.java.boilerplate.dto.consulta.RConsultaRegistros;
import com.java.boilerplate.dto.consulta.RRespostaConsultaRegistros;
import com.java.boilerplate.dto.errors.RLogErro;
import com.java.boilerplate.enums.EAcaoRbac;
import com.java.boilerplate.service.CLogErroService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/erros")
public class CLogErroController {
    private final CLogErroService logErroService;

    public CLogErroController(CLogErroService pLogErroService) {
        this.logErroService = pLogErroService;
    }

    @PostMapping("/consulta")
    @EndpointRbac(recurso = "Erros", acao = EAcaoRbac.CONSULTAR)
    public ResponseEntity<RRespostaConsultaRegistros<RLogErro>> consultar(@RequestBody(required = false) @Valid RConsultaRegistros pParametros) {
        return ResponseEntity.ok(logErroService.consultar(pParametros));
    }
}
