package com.java.boilerplate.controller;

import com.java.boilerplate.dto.preferencias.RPreferenciaUsuario;
import com.java.boilerplate.dto.preferencias.RPreferenciasUsuario;
import com.java.boilerplate.service.CPreferenciaUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(
            summary = "Remover preferência do usuário autenticado",
            description = "Remove de forma idempotente a preferência identificada por contexto e chave do usuário autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Preferência removida ou já ausente"),
            @ApiResponse(responseCode = "400", description = "Contexto ou chave ausente ou inválido"),
            @ApiResponse(responseCode = "401", description = "Token Bearer ausente ou inválido")
    })
    @DeleteMapping("/me/item")
    public ResponseEntity<Void> removerPreferenciaUsuarioAutenticado(
            @RequestParam("contexto") String pContexto,
            @RequestParam("chave") String pChave) {
        preferenciaUsuarioService.removerPreferenciaUsuarioAutenticado(pContexto, pChave);
        return ResponseEntity.noContent().build();
    }
}
