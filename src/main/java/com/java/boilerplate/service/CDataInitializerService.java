package com.java.boilerplate.service;

import com.java.boilerplate.config.RAdminProperties;
import com.java.boilerplate.enums.EComportamentoPadraoPermissao;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CPermissaoCargoRbac;
import com.java.boilerplate.repository.ICargoRbacRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CDataInitializerService implements ApplicationRunner {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final ICargoRbacRepository cargoRepository;
    private final CUsuarioService usuarioService;
    private final RAdminProperties adminProperties;

    public CDataInitializerService(
            ICargoRbacRepository pCargoRepository,
            CUsuarioService pUsuarioService,
            RAdminProperties pAdminProperties
    ) {
        this.cargoRepository = pCargoRepository;
        this.usuarioService = pUsuarioService;
        this.adminProperties = pAdminProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments pArgs) {
        criarCargoAdminSeNecessario();
        criarCargoUserSeNecessario();
        criarAdministradorSeHabilitado();
    }

    private void criarAdministradorSeHabilitado() {
        if (!adminProperties.habilitado()) {
            return;
        }
        if (adminProperties.name() == null || adminProperties.name().isBlank() || adminProperties.name().length() > 120
                || adminProperties.email() == null || !EMAIL.matcher(adminProperties.email()).matches()
                || adminProperties.password() == null || adminProperties.password().length() < 12 || adminProperties.password().length() > 72
                || "troque-esta-senha".equalsIgnoreCase(adminProperties.password())) {
            throw new IllegalStateException("Bootstrap administrativo habilitado exige nome, e-mail e senha forte explícitos");
        }
        usuarioService.criarUsuarioSistema(
                adminProperties.name(), adminProperties.email(), adminProperties.password(), "ADMIN", true
        );
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
        cargo.setDestinadoClienteFinal("USER".equals(pPapel));
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
                        pPermissao -> { },
                        () -> pPermissoes.add(criarPermissao(pRecurso, pAcao, pLiberado))
                );
    }
}
