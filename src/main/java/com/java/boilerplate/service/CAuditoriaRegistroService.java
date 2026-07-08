package com.java.boilerplate.service;

import com.java.boilerplate.dto.common.RAuditoriaRegistro;
import com.java.boilerplate.model.CEntidadeAuditavel;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CAuditoriaRegistroService {
    private final IUsuarioRepository usuarioRepository;

    public CAuditoriaRegistroService(IUsuarioRepository pUsuarioRepository) {
        this.usuarioRepository = pUsuarioRepository;
    }

    @Transactional(readOnly = true)
    public RAuditoriaRegistro montar(CEntidadeAuditavel pEntidade) {
        RAuditoriaRegistro auditoria = RAuditoriaRegistro.fromEntity(pEntidade);
        return auditoria.comReferencias(
                resolverReferenciaUsuario(auditoria.criadoPor()),
                resolverReferenciaUsuario(auditoria.atualizadoPor())
        );
    }

    private String resolverReferenciaUsuario(Long pIdUsuario) {
        if (pIdUsuario == null) {
            return "Sistema";
        }

        return usuarioRepository.findById(pIdUsuario)
                .map(CUsuario::getEmail)
                .orElse("Usuário #" + pIdUsuario);
    }
}
