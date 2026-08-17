package com.java.boilerplate.controller;

import com.java.boilerplate.dto.consulta.RConsultaRegistros;
import com.java.boilerplate.dto.consulta.RRespostaConsultaRegistros;
import com.java.boilerplate.dto.usuarios.RUsuario;
import com.java.boilerplate.service.CUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
public class CUsuarioController {
    private final CUsuarioService usuarioService;

    public CUsuarioController(CUsuarioService pUsuarioService) {
        this.usuarioService = pUsuarioService;
    }

    @PostMapping("/consulta")
    public ResponseEntity<RRespostaConsultaRegistros<RUsuario>> consultar(@RequestBody(required = false) @Valid RConsultaRegistros pParametros) {
        return ResponseEntity.ok(usuarioService.consultar(pParametros));
    }

    @org.springframework.web.bind.annotation.GetMapping("/{pIdUsuario}")
    public ResponseEntity<RUsuario> buscarPorId(@PathVariable Long pIdUsuario) {
        return ResponseEntity.ok(usuarioService.buscarPorId(pIdUsuario));
    }

    @PostMapping
    public ResponseEntity<RUsuario> criar(@RequestBody @Valid RUsuario pUsuario) {
        return ResponseEntity.ok(usuarioService.cadastrar(pUsuario));
    }

    @PutMapping
    public ResponseEntity<RUsuario> editar(@RequestBody @Valid RUsuario pUsuario) {
        return ResponseEntity.ok(usuarioService.editar(pUsuario));
    }

    @PatchMapping
    public ResponseEntity<RUsuario> modificar(@RequestBody @Valid RUsuario pUsuario) {
        return ResponseEntity.ok(usuarioService.modificar(pUsuario));
    }

    @DeleteMapping("/{pIdUsuario}")
    public ResponseEntity<Void> excluir(@PathVariable Long pIdUsuario) {
        usuarioService.excluir(pIdUsuario);
        return ResponseEntity.noContent().build();
    }
}
