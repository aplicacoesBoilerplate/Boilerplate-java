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
TOKEN_ENCRYPTION_KEY=<chave-aleatoria-de-32-bytes-codificada-em-Base64>
OTP_PEPPER=<outra-chave-aleatoria-com-no-minimo-32-caracteres>
JWT_ISSUER=boilerplate-java-api
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

O Compose integrado inicia frontend, API e MySQL em imagens de runtime. O phpMyAdmin é opt-in. Os dados do banco ficam no volume Docker nomeado `mysql_data`, fora do worktree.

Crie o arquivo de ambiente local, ignorado pelo Git, a partir do exemplo e defina senhas e chaves próprias:

```powershell
Copy-Item .env.example src/main/resources/.env
```

Suba todos os serviços e espere os health checks:

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

Para encerrar os containers, preserve os dados do banco:

```powershell
docker compose --env-file src/main/resources/.env down
```

Remover o volume `mysql_data` apaga o banco e só deve ser feito de forma explícita, depois de um backup validado.

## Execução local da API

Para desenvolver apenas a API fora de containers, suba o banco e o phpMyAdmin:

```powershell
docker compose -f src/main/resources/db/docker-compose.yml --profile dev-tools up -d
```

Em seguida, execute sem carregar variáveis manualmente:

```powershell
.\mvnw.cmd spring-boot:run
```

O arquivo local ignorado `src/main/resources/.env` é carregado automaticamente pelo Spring Boot e pode usar as mesmas variáveis de banco do Compose. Para a API local, `DB_APP_USERNAME` e `DB_APP_PASSWORD` são usados como fallback de `DB_USERNAME` e `DB_PASSWORD`; a API não deve conectar como `root`. `TOKEN_ENCRYPTION_KEY` deve ser a codificação Base64 de 32 bytes aleatórios para cifrar tokens JWE com A256GCM. Durante a migração, uma `JWT_SECRET` legada de ao menos 32 caracteres é derivada em uma chave AES-256; substitua-a por `TOKEN_ENCRYPTION_KEY` assim que possível.
O `DB_APP_USERNAME` e `DB_APP_PASSWORD` existem apenas para o Docker criar um usuário comum no MySQL; a imagem oficial não aceita `MYSQL_USER=root`.

A documentação Swagger fica desabilitada por padrão. Para habilitá-la, defina `DOCS_ENABLED=true`; `/doc` então usa Basic Auth com usuário não público definido por `DOC_USERNAME` e senha forte definida por `DOC_PASSWORD`. A aplicação aplica BCrypt no startup; se você quiser manter um hash pronto no ambiente, informe `DOC_PASSWORD_HASH`.
No Compose integrado, a API usa o tratamento nativo de headers encaminhados apenas na rede interna entre Nginx e API, preservando o endereço real para limitação por origem. Em implantação direta, mantenha `SERVER_FORWARD_HEADERS_STRATEGY=none`; habilite-o somente atrás de proxy confiável que normalize os headers.

## Testes

```powershell
.\mvnw.cmd -ntp clean verify
```

O profile `test` usa H2 em memória e desativa Flyway para validar o contexto da aplicação sem depender de MySQL local.

Consulte também [SECURITY.md](SECURITY.md), [docs/testing.md](docs/testing.md) e [docs/performance-runbook.md](docs/performance-runbook.md).
