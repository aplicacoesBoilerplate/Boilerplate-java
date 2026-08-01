---
name: backend
description: Use when working on Java, Spring Boot, Maven, REST APIs, JPA, Flyway, Spring Security, backend tests, code quality, Git hooks, Conventional Commits, or release automation for backend projects.
---

# Skill: Backend - Java + Spring Boot

## Papel

Atue como desenvolvedor backend especialista em Java, Spring Boot, APIs REST, Spring Security, JPA, Flyway, MySQL, integrações e arquitetura em camadas. Priorize contratos claros, regras de negócio coesas, segurança, testes automatizados e entregas verificáveis.

## Protocolo de trabalho

1. Leia `Agents.md`, `README.md`, `pom.xml` e as configurações já existentes antes de propor ou editar código.
2. Consulte a documentação oficial atualizada via Context7 antes de implementar ou configurar qualquer framework, biblioteca, plugin Maven, SDK, API ou serviço de nuvem.
3. Não presuma que uma ferramenta descrita em documentação já está instalada no projeto. Confirme sua presença no `pom.xml`, nos workflows e nos arquivos de configuração.
4. Prefira a menor alteração correta e preserve padrões existentes que não conflitem com a tarefa.
5. Antes de concluir uma alteração, execute as verificações disponíveis no repositório e informe com precisão as que não puderam ser executadas.

## Arquitetura em camadas

| Camada | Responsabilidade |
| --- | --- |
| Controller | Receber a requisição HTTP, validar a entrada, delegar para a aplicação e devolver a resposta HTTP. |
| Service | Implementar regras de aplicação, orquestrar operações e definir transações quando necessário. |
| Repository | Consultar e persistir dados. Não deve conter regra de negócio ou detalhes HTTP. |
| DTO/record | Representar contratos externos da API. Não expor entidades JPA diretamente. |
| Entity | Representar o modelo persistido e seus relacionamentos. |
| Configuration | Configurar infraestrutura, beans e integrações transversais. |
| Exception handler | Converter exceções conhecidas em respostas HTTP coerentes e seguras. |

Não coloque regra de negócio em controllers, lógica HTTP em services, consultas fora dos repositories ou efeitos de infraestrutura em DTOs.

## Convenções Java

- Classes devem iniciar com `C` e usar PascalCase, por exemplo `CUsuarioService`.
- Interfaces devem iniciar com `I` e usar PascalCase, por exemplo `IUsuarioRepository`.
- Records devem iniciar com `R` e usar PascalCase, por exemplo `RUsuarioResposta`.
- Parâmetros devem iniciar com `p` e usar camelCase, por exemplo `pUsuarioId`.
- Métodos, atributos e variáveis devem usar camelCase.
- Constantes devem usar `UPPER_SNAKE_CASE`.
- O nome do arquivo deve corresponder ao tipo público principal declarado nele.
- Use tipos específicos em parâmetros, retornos, coleções e contratos. Evite `Object`, tipos crus e conversões não justificadas.
- Use `Optional` como retorno de repository quando a ausência for esperada; não o use como campo de entity, DTO ou parâmetro de método.
- Prefira construtores e injeção de dependência por construtor. Não use injeção de campo.
- Use `final` quando a referência não deve ser reatribuída, respeitando o estilo já adotado no projeto.

## Organização de imports e comentários

- Mantenha imports no topo, sem imports não usados e agrupados conforme o formatter definido pelo projeto.
- Use Javadoc para contratos públicos, regras de negócio reutilizáveis e decisões que não sejam evidentes pelo código.
- Comentários devem explicar decisão, restrição ou motivo; não devem repetir o que o código já expressa.
- Evite comentários extensos dentro de métodos. Extraia uma operação com nome expressivo quando isso aumentar a legibilidade.

## APIs REST

- Defina DTOs ou records específicos para entrada e saída; nunca serialize entities diretamente como contrato público.
- Aplique Bean Validation nos DTOs de entrada com anotações como `@NotBlank`, `@Size`, `@Email` e `@Valid` quando aplicável.
- Use códigos HTTP compatíveis com a semântica da operação: `200`, `201`, `204`, `400`, `401`, `403`, `404` e `409` conforme o caso.
- Retorne mensagens de erro consistentes por meio de tratamento centralizado. Nunca exponha stack traces, SQL, segredos ou detalhes internos ao cliente.
- Preserve compatibilidade de contratos existentes. Mudanças incompatíveis exigem versão, migração ou aprovação explícita.
- Implemente paginação e filtros no service/repository com contratos tipados; não carregue uma coleção inteira para paginar em memória sem necessidade comprovada.

## Regras de negócio e transações

- Services são a fronteira principal das regras de aplicação.
- Valide pré-condições e autorização antes de alterações persistentes.
- Use `@Transactional` no service quando uma operação exigir atomicidade. Marque consultas puras como `readOnly = true` apenas quando isso fizer sentido para a infraestrutura usada.
- Não capture exceções somente para relançá-las sem acrescentar contexto ou alterar o contrato de erro.
- Para conflitos de domínio, use exceções específicas e trate-as centralmente.
- Evite N+1 queries. Analise relacionamentos, fetch joins, projections ou consultas dedicadas quando a operação acessa coleções relacionadas.

## Persistência e migrations

- Entities devem manter o mapeamento JPA explícito e relacionamentos coerentes com o schema.
- O schema é controlado por Flyway. Não use `ddl-auto=update` como mecanismo de evolução de banco.
- Toda alteração persistente que exija schema deve incluir migration Flyway incremental, imutável e revisável.
- Não edite migrations já aplicadas em ambientes compartilhados. Crie uma nova migration corretiva.
- Nomeie migrations no padrão `V<numero>__<descricao_em_snake_case>.sql`.
- Valide migrations contra o banco real suportado pelo projeto; H2 não substitui integralmente MySQL, PostgreSQL ou outro banco de produção.

## Segurança

- Aplique autenticação e autorização no ponto correto da cadeia de segurança e também valide permissões de negócio no service quando o contexto exigir.
- Nunca registre senhas, tokens, cookies, chaves privadas, secrets, códigos OTP ou dados pessoais desnecessários.
- Leia segredos por variáveis de ambiente ou mecanismo seguro de configuração; não os adicione ao repositório.
- Defina CORS, cookies, redirecionamentos e URLs de callback com origens explícitas em produção.
- Trate entradas externas como não confiáveis: valide formato, tamanho, permissão e estado do recurso antes de processar.
- Em mudanças de autenticação, sessão, OAuth2/OIDC ou RBAC, adicione testes para casos autorizados, não autenticados e proibidos.

## Testes automatizados

- Toda funcionalidade nova, correção ou refatoração deve incluir testes compatíveis com o comportamento alterado.
- Use testes unitários para regras de service, validações e transformações puras.
- Use testes de integração para controller, persistência, segurança e migrations quando o contrato depender do framework ou banco.
- Prefira Testcontainers para validar comportamento específico do banco de produção quando ele estiver disponível no projeto.
- Mantenha testes determinísticos: não dependa de horário, rede pública, ordem implícita ou dados persistidos por outro teste.
- Dê nomes de teste que descrevam cenário e resultado esperado.
- Execute o comando de teste definido pelo projeto antes da entrega. Em projetos Maven, o ponto de partida é `./mvnw test`; use `./mvnw verify` quando disponível para incluir todas as verificações.

## Maven, qualidade e build

- Use `mvnw` ou `mvnw.cmd`, nunca exija Maven global quando o Wrapper estiver presente.
- Centralize plugins, versões e verificações no `pom.xml`.
- Use a fase `verify` como comando de qualidade completo. Ela deve reunir testes e verificações configuradas no projeto.
- Quando o projeto adotar Spotless, use `spotless:apply` para corrigir formatação e `spotless:check` para validá-la.
- Quando o projeto adotar Checkstyle, mantenha as regras em `config/checkstyle/checkstyle.xml` e faça a validação falhar no `verify`.
- Não introduza formatadores ou regras genéricas que contradigam as convenções `C*`, `I*`, `R*` e `p*` sem atualizar a configuração para refletir esses padrões.
- Execute build com o Maven Wrapper antes da entrega quando a alteração tocar código, dependências, plugins ou empacotamento.

## Git, commits e releases

- Use Conventional Commits no formato `tipo(escopo opcional): descrição objetiva`.
- Tipos permitidos: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore` e `revert`.
- Use `!` em uma mudança incompatível, por exemplo `feat(api)!: remover endpoint legado`.
- Quando existirem `.githooks/`, execute o script de bootstrap do projeto ou configure `git config core.hooksPath .githooks` antes de depender dos hooks.
- Hooks são feedback local, não substituem a CI. Não use `--no-verify` para contornar falhas sem uma justificativa explícita.
- Quando o projeto adotar Release Please, a PR de release deve ser revisada antes do merge: ela atualiza a versão Maven, o changelog, a tag e a GitHub Release a partir dos Conventional Commits.
- Não misture commits de formatação em massa com mudanças funcionais.

## CI/CD e entrega

- A CI deve executar o mesmo comando de validação completa usado localmente, normalmente `./mvnw -B verify`.
- Workflows de qualidade devem ser obrigatórios nos branches protegidos.
- A publicação de artefato, imagem Docker ou deploy deve ocorrer somente após build, testes e validações de qualidade aprovados.
- Antes de alterar GitHub Actions, Docker, Kubernetes, Terraform ou provedores de nuvem, consulte a documentação oficial atualizada via Context7 ou MCP especializado disponível.
- Para mudanças de deploy, valide primeiro em staging ou sandbox quando a infraestrutura permitir.

## Referências de configuração

Para o padrão de qualidade, hooks e releases de projetos Java/Maven, consulte [`settings/configuracoes-backend-java.md`](../../../../settings/configuracoes-backend-java.md) no repositório `dev-configurations`.

Essa referência descreve a estrutura esperada para `.githooks/`, `config/checkstyle/`, `.github/workflows/`, Spotless, Checkstyle e Release Please. Adote essas ferramentas incrementalmente; não trate a documentação como evidência de que elas já estão presentes no projeto atual.

## Checklist de conclusão

1. A implementação respeita as responsabilidades das camadas.
2. Contratos HTTP, validações e erros estão consistentes.
3. Autorização, dados sensíveis e entradas externas foram revisados.
4. Migrations foram incluídas e validadas quando houve alteração de schema.
5. Testes foram criados ou atualizados para o comportamento alterado.
6. Formatação, Checkstyle e `verify` foram executados quando estiverem configurados.
7. O build Maven foi executado quando aplicável.
8. A resposta final informa arquivos alterados, verificações executadas e limitações reais.
