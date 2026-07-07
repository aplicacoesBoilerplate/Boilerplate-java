# Boilerplate-java

Backend Java para projetos futuros com Spring Boot, JPA, Flyway, autenticação JWT, recuperação de senha por e-mail, login com Google, solicitações de acesso, RBAC e motor de filtros compatível com o Boilerplate-vue.

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
- Solicitação pública de acesso em `/auth/solicitacoes-acesso`.
- CRUD administrativo de usuários em `/usuarios`.
- CRUD administrativo de cargos/permissões em `/rbac/cargos`.
- Filtros e paginação por cursor com os operadores do frontend.
- Migrations limpas para usuários, RBAC, OTP, refresh tokens, solicitações de acesso e logs de erro.

## Variáveis Principais

```dotenv
API_PORT=8080
DB_HOST=localhost
DB_PORT=3306
DB_NAME=boilerplate_java
DB_USERNAME=root
DB_PASSWORD=root
DB_ROOT_PASSWORD=root
DB_APP_USERNAME=boilerplate_app
DB_APP_PASSWORD=boilerplate_app
JWT_SECRET=troque-essa-chave
JWT_ISSUER=boilerplate-java-api
DOC_USERNAME=DeveloperArea
DOC_PASSWORD=boilerplate
# opcional: use DOC_PASSWORD_HASH em vez de DOC_PASSWORD se preferir informar BCrypt pronto
FRONTEND_URL=http://localhost:5173
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:9000
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USER=
EMAIL_PASS=
MANAGEMENT_HEALTH_MAIL_ENABLED=false
GOOGLE_CLIENT_ID=
ADMIN_EMAIL=boilerplate@gmail.com
ADMIN_PASSWORD=Boilerplate@123
ADMIN_NAME=BOILERPLATE
```

## Execução

Suba o banco:

```powershell
docker compose -f src/main/resources/db/docker-compose.yml up -d
```

O `DB_USERNAME` e `DB_PASSWORD` são usados pela aplicação. Para uso local, eles podem continuar como `root/root`.
O `DB_APP_USERNAME` e `DB_APP_PASSWORD` existem apenas para o Docker criar um usuário comum no MySQL; a imagem oficial não aceita `MYSQL_USER=root`.

Execute a API:

```powershell
.\mvnw.cmd spring-boot:run
```

A documentação Swagger fica em `/doc` e usa Basic Auth com usuário definido por `DOC_USERNAME` e senha definida por `DOC_PASSWORD`. A aplicação aplica BCrypt no startup; se você quiser manter um hash pronto no ambiente, informe `DOC_PASSWORD_HASH`.

## Testes

```powershell
.\mvnw.cmd test
```

O profile `test` usa H2 em memória e desativa Flyway para validar o contexto da aplicação sem depender de MySQL local.
