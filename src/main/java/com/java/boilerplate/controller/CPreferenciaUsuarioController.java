package com.java.boilerplate.controller;

import com.java.boilerplate.dto.preferencias.RPreferenciaUsuario;
import com.java.boilerplate.dto.preferencias.RPreferenciasUsuario;
import com.java.boilerplate.service.CPreferenciaUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/preferencias")
public class CPreferenciaUsuarioController {
    private final CPreferenciaUsuarioService preferenciaUsuarioService;

    public CPreferenciaUsuarioController(CPreferenciaUsuarioService pPreferenciaUsuarioService) {
        this.preferenciaUsuarioService = pPreferenciaUsuarioService;
    }

    @GetMapping("/me")
    public ResponseEntity<RPreferenciasUsuario> buscarPreferenciasUsuarioAutenticado() {
        return ResponseEntity.ok(preferenciaUsuarioService.buscarPreferenciasUsuarioAutenticado());
    }

    @PutMapping("/me")
    public ResponseEntity<RPreferenciasUsuario> salvarPreferenciasUsuarioAutenticado(
            @RequestBody @Valid RPreferenciasUsuario pRequest) {
        return ResponseEntity.ok(preferenciaUsuarioService.salvarPreferenciasUsuarioAutenticado(pRequest));
    }

    @PutMapping("/me/item")
    public ResponseEntity<RPreferenciaUsuario> salvarPreferenciaUsuarioAutenticado(
            @RequestBody @Valid RPreferenciaUsuario pRequest) {
        return ResponseEntity.ok(preferenciaUsuarioService.salvarPreferenciaUsuarioAutenticado(pRequest));
    }
}
