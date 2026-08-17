# Relatório de varredura e remediação de segurança

Data: 12 de agosto de 2026  
Escopo: backend Java/Spring Boot completo, incluindo código, autenticação/autorização, persistência, migrations, configuração, Docker/Compose, documentação e testes.

## Resumo executivo

A revisão ocorreu em ciclos independentes de descoberta, remediação e confirmação. Os resultados intermediários não devem ser somados como vulnerabilidades únicas, porque as varreduras posteriores reavaliaram controles relacionados e encontraram extensões das mesmas superfícies.

A varredura selada do frontend (`626df0e6-6177-46dd-9dc0-cad611a64c69`) registrou zero achados reportáveis. Uma varredura do backend (`a90d1fd7-e0ed-4514-a9ab-26ab9821e775`) encontrou exaustão do health público e PII em falhas assíncronas; a revisão pós-correção (`ab348f52-ddca-4f48-91ce-275c5bfe9f5e`) encontrou três variantes no health (HEAD, subrotas e quota do probe). A revisão seguinte (`73617b7c-90b0-420a-9d6d-aaf6977e1cc2`) encontrou a preservação de `X-Forwarded-For` não confiável. Todos os achados foram reproduzidos ou validados, corrigidos e cobertos pelo gate final; o selo canônico pós-remediação é entregue como artefato externo da tarefa para não alterar o snapshot depois de iniciado.

## Achados e correções

| Área | Problema encontrado | Correção aplicada |
|---|---|---|
| Segredos JWT/OTP | JWT possuía fallback conhecido; OTP não possuía proteção criptográfica adequada. | Segredos passaram a ser obrigatórios, independentes e validados com no mínimo 32 caracteres. OTP usa `SecureRandom`, HMAC-SHA-256 com pepper e vínculo ao usuário. |
| Bootstrap administrativo | Credenciais/defaults do administrador podiam produzir uma conta previsível. | Bootstrap desabilitado por padrão; quando habilitado, exige nome, e-mail e senha explícitos e rejeita placeholders publicados. |
| Swagger | Documentação podia iniciar com credenciais fracas/default; Basic Auth não limitava tentativas. | Swagger e OpenAPI ficam desligados por padrão via `DOCS_ENABLED=false`. Quando ligados, exigem usuário não público, senha forte ou BCrypt e limitam Basic Auth antes do BCrypt, por IP e globalmente. |
| Falha de startup reportada | `documentacao.credenciaisSeguras=false` bloqueava todo o `ApplicationContext` quando não havia credenciais locais. | A validação agora é condicional: credenciais continuam obrigatórias e fortes somente quando `DOCS_ENABLED=true`. Com documentação desligada, o backend inicia sem publicar Swagger nem exigir segredo desnecessário. |
| Recuperação de senha | Recuperação podia reativar conta, enumerar usuários por trabalho síncrono, invalidar OTP vigente e sofrer corrida de tentativas. | Recuperação não altera `ativo`; solicitação é assíncrona e usa executor limitado; SMTP ocorre fora da transação; OTP vigente é preservado; tentativas usam transação independente, comparação constante e lock pessimista; reset revoga sessão. |
| Sessões JWT | Tokens eram aceitos sem revogação persistente completa; token anterior à desativação podia reviver após reativação. | Apenas hash SHA-256 do token fica persistido; validação exige sessão ativa e estado atual do usuário/cargo; logout, troca/reset de senha e toda mutação/desativação administrativa revogam a sessão. |
| Login Google | Verificador era recriado por chamada e identidade podia depender apenas do e-mail. | Verificador singleton; `audience` e `email_verified` obrigatórios; vínculo pelo `sub` estável do Google; usuário desconhecido/inativo falha fechado. |
| Solicitação de acesso | Rota anônima criava usuário com senha escolhida antes de provar posse do e-mail, permitindo pre-hijacking e crescimento persistente. | `/auth/solicitacoes-acesso` agora exige `ROLE_ADMIN` no HTTP e novamente no service. |
| Autorização administrativa | Diretório de usuários e operações sensíveis podiam depender apenas do RBAC de rota; cargo inativo ainda emitia `ROLE_ADMIN`. | Métodos de usuário exigem `ADMIN`; cargo ausente/inativo desabilita o `UserDetails` e não emite authorities, alinhando `hasRole` com RBAC dinâmico. |
| Preferências/IDOR/quota | Risco de acesso por identificador e ultrapassagem concorrente da quota. | Proprietário vem exclusivamente do principal autenticado; consultas incluem ID do usuário; tamanhos e lote são limitados; lock pessimista serializa contagem e inserção. |
| Consultas dinâmicas | Filtros e listas podiam gerar consultas caras; conversão inválida podia virar erro 500 persistido. | Campos/operadores são allowlistados; até 10 filtros, 100 valores, 100 resultados e timeout de 2 s; conversões inválidas retornam 400 sem log persistente. |
| Corpos HTTP | Limite de 64 KiB existia apenas em autenticação pública; JSON autenticado podia ser desserializado sem orçamento. | Filtro precoce limita todo `POST`, `PUT` e `PATCH`, por `Content-Length` e leitura real, inclusive chunked, antes do MVC/Jackson. |
| Rate limiting | Login/recuperação não tinham proteção suficiente; decisões em dimensões diferentes drenavam a quota global; mapa de chaves podia exceder a capacidade. | Limites por identidade, IP e global; decisão multidimensional atômica; pedidos rejeitados não drenam outras dimensões; capacidade de chaves é verificada em conjunto; respostas 429 incluem `Retry-After`. |
| Proxy e endereço do cliente | Forwarded headers podiam ser falsificados; o Nginx preservava `X-Forwarded-For` enviado pelo cliente e a isenção de loopback alcançava todas as rotas públicas. | Padrão continua `none`. No Compose, o Nginx sobrescreve `X-Forwarded-For`, somente proxies internos são confiáveis e a isenção de loopback cobre exclusivamente GET/HEAD do probe público. |
| Erros e logs | Falhas esperadas de autenticação/parse podiam virar 500 e amplificar gravações; mensagens internas podiam vazar. | Autenticação retorna 401 genérico; JSON/tipos inválidos retornam 400; somente 5xx inesperado é persistido; mensagem pública é fixa, trace desativado e retenção limitada. |
| Health e contenção | Health completo era anônimo, não passava pelo rate limit e os containers não tinham cotas explícitas. Revisões de bypass também encontraram HEAD sem contador, subrotas fora do matcher ADMIN e quota externa compartilhada com o probe. | O público recebe somente `ping`; GET/HEAD externos são limitados; demais métodos são negados; health completo e subrotas exigem `ADMIN`; probe loopback não compartilha quota externa; Compose/JVM limitam recursos. |
| Falhas assíncronas | Exceção de e-mail interpolava o destinatário e podia levar PII ao log do executor. | Exceções não carregam destinatário nem causa sensível; handler assíncrono registra somente método e classe da falha, sem parâmetros/mensagem controlada. |
| Exposição operacional | MySQL/phpMyAdmin e execução do container tinham defaults inseguros; datadir e uploads reais estavam sob o worktree. | Banco e phpMyAdmin somente em loopback/perfil opt-in; Compose exige senhas, usa volume nomeado e imagem não root; datadir/uploads existentes foram preservados fora do repositório com ACL restrita. |
| Dependências | Versões antigas elevavam risco conhecido. | Spring Boot, java-jwt, Google API Client e springdoc foram atualizados; BOMs Jackson foram alinhados. A análise OSV do SBOM encontrou 0 pacotes afetados entre 148 analisados. |

## Controles de defesa em profundidade

- `SECURITY.md` documenta segredos, bootstrap, Swagger, Actuator e resposta a incidentes.
- `.env.example` não contém credenciais reutilizáveis.
- Script `scripts/verify-no-secrets.ps1` impede publicação acidental de arquivos concretos de segredo.
- Liveness público contém somente `ping` e é limitado; health completo e métricas exigem `ADMIN`.
- CORS usa lista explícita e não aceita origem coringa com credenciais.
- Migrations V4/V5 protegem OTP, revogam grants inadequados e vinculam a identidade Google.

## Evidências de verificação

- `mvnw.cmd -ntp clean verify`: build, carregamento completo do `ApplicationContext` e 51 testes.
- Testes de regressão cobrem configuração/segredos, JWT e revogação, OTP, Google, autorização, preferências, consultas, rate limiting, limites pré-MVC, Swagger Basic e tratamento de erros.
- `docker compose config --quiet`: configuração integrada validada com segredos efêmeros.
- Compose isolado: banco, API e frontend `healthy`; login/consulta/logout/replay validados; três amostras de 100 requisições com p95 entre 37,18 ms e 114,48 ms (máximo após cota de uma CPU), Hikari pendente 0 e 24–25 threads vivas.
- `scripts/verify-no-secrets.ps1`: nenhum arquivo concreto de segredo encontrado.
- `git diff --check`: nenhuma inconsistência de whitespace no patch; avisos CRLF do Git são apenas normalização do Windows.

## Limitações

- A execução Deep Scan solicitada não pôde iniciar porque o host Desktop forneceu perfil de filesystem não gerenciado, incompatível com o worker read-only exigido. Foram executadas varreduras Standard independentes e repetidas, com auditor-base e investigador focado.
- OWASP Dependency-Check não tinha base NVD/API key disponível; a verificação de dependências utilizou SBOM + OSV. Esse tipo de base deve continuar sendo executado no CI.
- Controles de TLS, WAF, limites do balanceador, política SMTP/Google e segredos reais dependem do ambiente de implantação.

## Operação segura

Mantenha `DOCS_ENABLED=false` e `SERVER_FORWARD_HEADERS_STRATEGY=none` por padrão. Ative Swagger somente com credencial exclusiva. Ative headers encaminhados somente atrás de proxy confiável explicitamente configurado. Após o primeiro bootstrap administrativo, volte `ADMIN_BOOTSTRAP_ENABLED=false` e remova a senha do ambiente.
