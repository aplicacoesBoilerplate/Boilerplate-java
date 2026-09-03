package com.java.boilerplate.controller;

import com.java.boilerplate.dto.rbac.RManifestoRbac;
import com.java.boilerplate.service.CRbacManifestoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rbac/manifesto")
public class CRbacManifestoController {
    private final CRbacManifestoService manifestoService;

    public CRbacManifestoController(CRbacManifestoService pManifestoService) {
        this.manifestoService = pManifestoService;
    }

    @GetMapping
    public ResponseEntity<RManifestoRbac> buscarManifesto() {
        return ResponseEntity.ok(manifestoService.buscarManifesto());
    }
}
