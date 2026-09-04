# Boilerplate-java

Backend Java para projetos futuros com Spring Boot, JPA, Flyway, autenticação JWE, recuperação de senha por e-mail, login com Google, solicitações de acesso, RBAC e motor de filtros compatível com o Boilerplate-vue.

## Stack

- Java 17+
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Flyway
- MySQL 8.4
- Thymeleaf para templates de e-mail
- jgitver para versionamento baseado em Git

## Recursos Implementados

- Autenticação por e-mail e senha em `/auth/login`.
- Login com Google em `/auth/login/google`.
- Usuário autenticado em `/auth/me`.
- Recuperação de senha em `/auth/recuperacao-senha/*`.
- Provisionamento administrativo de solicitações de acesso em `/auth/solicitacoes-acesso`; não há auto-registro público.
- CRUD administrativo de usuários em `/usuarios`.
- CRUD administrativo de cargos/permissões em `/rbac/cargos`.
- Liveness público mínimo em `/api/v1/actuator/health-check/public`; diagnóstico SMTP e health completo exigem `ADMIN`.
- Filtros e paginação por cursor com os operadores do frontend.
- Migrations limpas para usuários, RBAC, OTP, refresh tokens, solicitações de acesso e logs de erro.

## Variáveis Principais

```dotenv
API_PORT=8080
DB_HOST=localhost
DB_PORT=3306
DB_NAME=boilerplate_java
DB_USERNAME=boilerplate_app
DB_PASSWORD=<senha-forte-e-exclusiva>
DB_ROOT_PASSWORD=<outra-senha-forte-e-exclusiva>
DB_APP_USERNAME=boilerplate_app
DB_APP_PASSWORD=<senha-forte-e-exclusiva>
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_ENABLED=true
RATE_LIMIT_WINDOW_SECONDS=60
RATE_LIMIT_GLOBAL_REQUESTS=1000
RATE_LIMIT_AUTHENTICATED_REQUESTS=100
RATE_LIMIT_PUBLIC_REQUESTS=30
RATE_LIMIT_LOGIN_ATTEMPTS=5
RATE_LIMIT_MAX_TRACKED_KEYS=10000
REDISINSIGHT_PORT=5540
TOKEN_ENCRYPTION_KEY=<chave-aleatoria-de-32-bytes-codificada-em-Base64>
JWT_SECRET=<chave-aleatoria-com-no-minimo-32-caracteres>
OTP_PEPPER=<outra-chave-aleatoria-com-no-minimo-32-caracteres>
TOKEN_ISSUER=boilerplate-java-api
DOCS_ENABLED=true
DOC_USERNAME=seu-usuario-documentacao
DOC_PASSWORD=<senha-com-no-minimo-12-caracteres>
# opcional: use DOC_PASSWORD_HASH em vez de DOC_PASSWORD se preferir informar BCrypt pronto
FRONTEND_URL=http://localhost:5173
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:9000
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USER=
EMAIL_PASS=
MANAGEMENT_HEALTH_MAIL_ENABLED=false
GOOGLE_CLIENT_ID=
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=<senha-com-12-a-72-caracteres>
ADMIN_NAME=BOILERPLATE
ADMIN_BOOTSTRAP_ENABLED=false
MAX_PERSISTED_ERRORS=1000
```

## Execução com Docker

O Compose de infraestrutura fica no repositório DockerLib, em `boilerplateJwt/docker-compose.yml`. Configure o `.env` e execute os comandos a partir desse diretório. Ele inicia MySQL e Redis como serviços internos; phpMyAdmin e Redis Insight são opt-in no profile `tools`.

Crie o arquivo de ambiente local, ignorado pelo Git, a partir do exemplo e defina senhas e chaves próprias:

```powershell
Copy-Item .env.example src/main/resources/.env
```

Inicie a infraestrutura pelo Compose configurado para o ambiente local:

```powershell
docker compose --env-file src/main/resources/.env up --build --wait
```

URLs locais:

- Frontend: `http://localhost:5173`
- API: `http://localhost:8080/api/v1`
- Swagger (somente com `DOCS_ENABLED=true`): `http://localhost:8080/api/v1/doc`
- Liveness público: `http://localhost:8080/api/v1/actuator/health-check/public`
- Diagnóstico SMTP (Bearer com papel `ADMIN`): `http://localhost:8080/api/v1/actuator/health-check/smtp`
- phpMyAdmin (somente quando iniciado com `--profile tools`): `http://localhost:8081`

## Inspeção do Redis

O Redis não recebe conexões do navegador ou da rede externa. No diretório DockerLib `boilerplateJwt`, inicie a ferramenta visual de desenvolvimento com o profile `tools`:

```powershell
docker compose --profile tools up --wait
```

Abra `http://localhost:5540` e adicione uma conexão com host `redis` e porta `6379`. Informe `REDIS_PASSWORD` se essa variável estiver configurada. O Redis Insight permite consultar chaves, valores e TTLs armazenados.

Uma requisição HTTP continua chegando à API. Preferências autenticadas e permissões RBAC locais usam cache-aside: a API verifica primeiro o Redis; em cache hit, ela devolve o resultado sem consultar MySQL. Em cache miss ou falha do Redis, consulta MySQL e mantém a resposta normal.

As chaves de preferências e permissões não expiram automaticamente. Elas permanecem no Redis até que uma escrita confirmada no MySQL invalide somente a chave afetada. Notificações para sessões já autenticadas após uma alteração de cargo pertencem a uma entrega posterior de eventos em tempo real.

## Remover uma preferência no frontend

Para limpar uma preferência persistida — por exemplo, filtros em `filters/<contexto>` — o frontend deve chamar `DELETE /api/v1/preferencias/me/item?contexto=<contexto>&chave=<chave>` com o mesmo token Bearer usado nas demais preferências. A resposta é sempre `204 No Content` quando `contexto` e `chave` são válidos, inclusive quando a preferência já foi removida; não envie nem aceite identificador de usuário, pois a operação usa exclusivamente o usuário autenticado.

```javascript
await fetch(`${apiUrl}/preferencias/me/item?${new URLSearchParams({
  contexto: 'filters',
  chave: contexto,
})}`, {
  method: 'DELETE',
  headers: { Authorization: `Bearer ${token}` },
});
```

## Rate limit distribuído

Cada chamada a `/api/v1` consome, em uma única decisão atômica no Redis, a quota global e uma quota por sujeito. Usuários autenticados são identificados pelo ID já validado pelo bearer; para requisições anônimas é usada apenas a origem remota. As chaves Redis usam hash SHA-256 e a hash-tag `{api}`, portanto não armazenam IP, ID, token ou credencial em texto puro e permanecem elegíveis para o mesmo slot de Redis Cluster.

A janela fixa começa na primeira requisição e é configurada por `RATE_LIMIT_WINDOW_SECONDS`. As quotas independentes são `RATE_LIMIT_GLOBAL_REQUESTS`, `RATE_LIMIT_AUTHENTICATED_REQUESTS` e `RATE_LIMIT_PUBLIC_REQUESTS`; os limites de login e demais fluxos de identidade continuam como controles adicionais no serviço. Ao exceder qualquer quota, a API devolve `429` com `Retry-After` em segundos arredondados para cima. Somente `GET` e `HEAD` do health público originados em loopback são isentos.

Se Redis estiver temporariamente indisponível, a API usa o fallback local limitado por `RATE_LIMIT_MAX_TRACKED_KEYS`, com expiração. Chaves já rastreadas continuam limitadas; quando a capacidade local se esgota, somente novos sujeitos são rejeitados. Ao Redis retornar, a decisão volta automaticamente ao script atômico sem migrar contadores locais.

Para encerrar os containers, preserve os dados do banco:

```powershell
docker compose --env-file src/main/resources/.env down
```

Remover o volume `mysql_data` apaga o banco e só deve ser feito de forma explícita, depois de um backup validado.

## Execução local da API

Para desenvolver apenas a API fora de containers, suba o banco e o phpMyAdmin:

```powershell
docker compose --env-file src/main/resources/.env -f src/main/resources/db/docker-compose.yml --profile dev-tools up -d
```

Em seguida, execute sem carregar variáveis manualmente:

```powershell
.\mvnw.cmd spring-boot:run
```

O arquivo local ignorado `src/main/resources/.env` é carregado automaticamente pelo Spring Boot e pode usar as mesmas variáveis de banco do Compose. Para a API local, `DB_APP_USERNAME` e `DB_APP_PASSWORD` são usados como fallback de `DB_USERNAME` e `DB_PASSWORD`; a API não deve conectar como `root`. `TOKEN_ENCRYPTION_KEY` deve ser a codificação Base64 de 32 bytes aleatórios para cifrar tokens JWE com A256GCM.
O `DB_APP_USERNAME` e `DB_APP_PASSWORD` existem apenas para o Docker criar um usuário comum no MySQL; a imagem oficial não aceita `MYSQL_USER=root`.

A documentação Swagger fica desabilitada por padrão. Para habilitá-la, defina `DOCS_ENABLED=true`; `/doc` então usa Basic Auth com usuário não público definido por `DOC_USERNAME` e senha forte definida por `DOC_PASSWORD`. A aplicação aplica BCrypt no startup; se você quiser manter um hash pronto no ambiente, informe `DOC_PASSWORD_HASH`.
No Compose integrado, a API usa o tratamento nativo de headers encaminhados apenas na rede interna entre Nginx e API, preservando o endereço real para limitação por origem. Em implantação direta, mantenha `SERVER_FORWARD_HEADERS_STRATEGY=none`; habilite-o somente atrás de proxy confiável que normalize os headers.

## Testes

```powershell
.\mvnw.cmd -ntp clean verify
```

O profile `test` usa H2 em memória e desativa Flyway para validar o contexto da aplicação sem depender de MySQL local.

## Migrations Flyway

Migrations versionadas em `src/main/resources/db/migration` são imutáveis após o commit. Para evoluir o schema, crie uma nova migration no formato `V<N>__descricao.sql`; nunca edite, renomeie ou remova uma migration existente. O hook `pre-commit` e a CI validam essa regra.

Consulte também [SECURITY.md](SECURITY.md), [docs/testing.md](docs/testing.md) e [docs/performance-runbook.md](docs/performance-runbook.md).
