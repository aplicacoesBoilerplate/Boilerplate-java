package com.java.boilerplate.controller;

import com.java.boilerplate.dto.common.RRespostaPaginacao;
import com.java.boilerplate.dto.errors.RLogErro;
import com.java.boilerplate.dto.filtros.RParametrosPaginacao;
import com.java.boilerplate.service.CLogErroService;
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
    public ResponseEntity<RRespostaPaginacao<RLogErro>> consultar(
            @RequestBody(required = false) RParametrosPaginacao pParametros) {
        return ResponseEntity.ok(logErroService.consultar(pParametros));
    }
}
