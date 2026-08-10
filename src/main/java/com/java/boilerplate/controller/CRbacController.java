package com.java.boilerplate.controller;

import com.java.boilerplate.dto.consulta.RConsultaRegistros;
import com.java.boilerplate.dto.consulta.RRespostaConsultaRegistros;
import com.java.boilerplate.dto.rbac.RCargoRbac;
import com.java.boilerplate.service.CRbacService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rbac")
public class CRbacController {
    private final CRbacService rbacService;

    public CRbacController(CRbacService pRbacService) {
        this.rbacService = pRbacService;
    }

    @GetMapping("/cargos")
    public ResponseEntity<List<RCargoRbac>> listarTodos() {
        return ResponseEntity.ok(rbacService.listarTodos());
    }

    @PostMapping("/cargos/consulta")
    public ResponseEntity<RRespostaConsultaRegistros<RCargoRbac>> consultar(@RequestBody(required = false) @Valid RConsultaRegistros pParametros) {
        return ResponseEntity.ok(rbacService.consultar(pParametros));
    }

    @GetMapping("/cargos/{pIdCargo}")
    public ResponseEntity<RCargoRbac> buscarPorId(@PathVariable Long pIdCargo) {
        return ResponseEntity.ok(rbacService.buscarPorId(pIdCargo));
    }

    @PostMapping("/cargos")
    public ResponseEntity<RCargoRbac> salvar(@RequestBody @Valid RCargoRbac pCargo) {
        return ResponseEntity.ok(rbacService.cadastrar(pCargo));
    }

    @PutMapping("/cargos")
    public ResponseEntity<RCargoRbac> editar(@RequestBody @Valid RCargoRbac pCargo) {
        return ResponseEntity.ok(rbacService.editar(pCargo));
    }

    @PatchMapping("/cargos")
    public ResponseEntity<RCargoRbac> modificar(@RequestBody @Valid RCargoRbac pCargo) {
        return ResponseEntity.ok(rbacService.modificar(pCargo));
    }

    @DeleteMapping("/cargos/{pIdCargo}")
    public ResponseEntity<Void> excluir(@PathVariable Long pIdCargo) {
        rbacService.excluir(pIdCargo);
        return ResponseEntity.noContent().build();
    }
}
