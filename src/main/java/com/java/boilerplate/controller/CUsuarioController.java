package com.java.boilerplate.controller;

import com.java.boilerplate.dto.common.RRespostaPaginacao;
import com.java.boilerplate.dto.filtros.RParametrosPaginacao;
import com.java.boilerplate.dto.usuarios.RRespostaUsuario;
import com.java.boilerplate.dto.usuarios.RUsuario;
import com.java.boilerplate.service.CUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class CUsuarioController {
    private final CUsuarioService usuarioService;

    public CUsuarioController(CUsuarioService pUsuarioService) {
        this.usuarioService = pUsuarioService;
    }

    @GetMapping("/consulta")
    public ResponseEntity<RRespostaPaginacao<RUsuario>> consultarGet(
            @RequestParam(required = false) Integer limite,
            @RequestParam(required = false) Long proximaEntrada,
            @RequestParam(required = false) String ordem
    ) {
        return ResponseEntity.ok(usuarioService.consultar(new RParametrosPaginacao(limite, proximaEntrada, ordem, null)));
    }

    @PostMapping("/consulta")
    public ResponseEntity<RRespostaPaginacao<RUsuario>> consultarPost(@RequestBody(required = false) RParametrosPaginacao pParametros) {
        return ResponseEntity.ok(usuarioService.consultar(pParametros));
    }

    @PostMapping("/search")
    public ResponseEntity<List<RUsuario>> pesquisar(@RequestBody(required = false) RParametrosPaginacao pParametros) {
        return ResponseEntity.ok(usuarioService.consultar(pParametros).items());
    }

    @GetMapping("/{pIdUsuario}")
    public ResponseEntity<RRespostaUsuario> buscarPorId(@PathVariable Long pIdUsuario) {
        return ResponseEntity.ok(new RRespostaUsuario(usuarioService.buscarPorId(pIdUsuario)));
    }

    @PostMapping
    public ResponseEntity<RUsuario> criar(@RequestBody @Valid RUsuario pUsuario) {
        return ResponseEntity.ok(usuarioService.criar(pUsuario));
    }

    @PutMapping("/{pIdUsuario}")
    public ResponseEntity<RUsuario> atualizar(@PathVariable Long pIdUsuario, @RequestBody @Valid RUsuario pUsuario) {
        return ResponseEntity.ok(usuarioService.atualizar(pIdUsuario, pUsuario));
    }

    @DeleteMapping("/{pIdUsuario}")
    public ResponseEntity<Void> excluir(@PathVariable Long pIdUsuario) {
        usuarioService.excluir(pIdUsuario);
        return ResponseEntity.noContent().build();
    }
}
