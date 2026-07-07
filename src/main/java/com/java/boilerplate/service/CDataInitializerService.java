package com.java.boilerplate.service;

import com.java.boilerplate.enums.EComportamentoPadraoPermissao;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CPermissaoCargoRbac;
import com.java.boilerplate.repository.ICargoRbacRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CDataInitializerService implements ApplicationRunner {
    private final ICargoRbacRepository cargoRepository;
    private final CUsuarioService usuarioService;

    @Value("${ADMIN_EMAIL:boilerplate@gmail.com}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:Boilerplate@123}")
    private String adminPassword;

    @Value("${ADMIN_NAME:BOILERPLATE}")
    private String adminName;

    public CDataInitializerService(ICargoRbacRepository pCargoRepository, CUsuarioService pUsuarioService) {
        this.cargoRepository = pCargoRepository;
        this.usuarioService = pUsuarioService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments pArgs) {
        criarCargoAdminSeNecessario();
        criarCargoUserSeNecessario();
        usuarioService.criarUsuarioSistema(adminName, adminEmail, adminPassword, "ADMIN", true);
    }

    private void criarCargoAdminSeNecessario() {
        if (cargoRepository.existsByPapel("ADMIN")) {
            return;
        }

        CCargoRbac cargo = criarCargoBase("ADMIN", "Administrador", "mdi-account-tie", "Acesso operacional completo ao boilerplate.", EComportamentoPadraoPermissao.liberar, "/", "Inicio");
        cargoRepository.save(cargo);
    }

    private void criarCargoUserSeNecessario() {
        CCargoRbac cargo = cargoRepository.findByPapel("USER")
                .orElseGet(() -> criarCargoBase("USER", "Usuário", "mdi-account", "Acesso básico para uso diário do sistema.", EComportamentoPadraoPermissao.bloquear, "/usuarios", "Usuarios"));

        garantirPermissoesPadraoUsuario(cargo);
        cargoRepository.save(cargo);
    }

    private CCargoRbac criarCargoBase(String pPapel, String pNome, String pIcone, String pDescricao, EComportamentoPadraoPermissao pComportamento, String pPath, String pName) {
        CCargoRbac cargo = new CCargoRbac();
        cargo.setPapel(pPapel);
        cargo.setNome(pNome);
        cargo.setIcone(pIcone);
        cargo.setDescricao(pDescricao);
        cargo.setComportamentoPadrao(pComportamento);
        cargo.setRedirecionamentoPath(pPath);
        cargo.setRedirecionamentoName(pName);
        cargo.setRedirecionamentoFiltros("[]");
        cargo.setAtivo(true);
        return cargo;
    }

    private CPermissaoCargoRbac criarPermissao(String pRecurso, String pAcao, Boolean pLiberado) {
        CPermissaoCargoRbac permissao = new CPermissaoCargoRbac();
        permissao.setRecurso(pRecurso);
        permissao.setAcao(pAcao);
        permissao.setLiberado(pLiberado);
        return permissao;
    }

    private void garantirPermissoesPadraoUsuario(CCargoRbac pCargo) {
        List<CPermissaoCargoRbac> permissoes = new ArrayList<>(pCargo.getPermissoes());

        garantirPermissao(permissoes, "rotas", "Inicio", true);
        garantirPermissao(permissoes, "rotas", "Usuarios", true);
        garantirPermissao(permissoes, "api", "GET /usuarios/**", true);
        garantirPermissao(permissoes, "api", "POST /usuarios/consulta", true);
        garantirPermissao(permissoes, "api", "POST /usuarios/search", true);

        pCargo.definirPermissoes(permissoes);
    }

    private void garantirPermissao(List<CPermissaoCargoRbac> pPermissoes, String pRecurso, String pAcao, Boolean pLiberado) {
        pPermissoes.stream()
                .filter(pPermissao -> pRecurso.equals(pPermissao.getRecurso()) && pAcao.equals(pPermissao.getAcao()))
                .findFirst()
                .ifPresentOrElse(
                        pPermissao -> pPermissao.setLiberado(pLiberado),
                        () -> pPermissoes.add(criarPermissao(pRecurso, pAcao, pLiberado))
                );
    }
}
