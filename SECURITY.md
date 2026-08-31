# Política de segurança

## Configuração obrigatória

Mantenha credenciais fora deste repositório. `TOKEN_ENCRYPTION_KEY` deve ser Base64 de 32 bytes aleatórios e independente de `OTP_PEPPER`. Banco, documentação e integrações externas também devem receber credenciais exclusivas por ambiente.

Os tokens de acesso usam JWE com `dir` e `A256GCM`: as claims são cifradas e o banco guarda somente o hash SHA-256 do token. A troca da chave invalida todas as sessões existentes; gere e distribua a nova chave de forma coordenada.


O bootstrap administrativo é desativado por padrão. Para a primeira instalação, use `ADMIN_BOOTSTRAP_ENABLED=true` junto de `ADMIN_NAME`, `ADMIN_EMAIL` e uma senha de 12 a 72 caracteres. Após confirmar a conta, volte a opção para `false` e remova a senha do ambiente.

Nunca reutilize valores de `.env.example`. Arquivos `.env`, chaves, certificados, dumps e diretórios de dados de bancos não podem ficar sob a raiz do projeto. Use volumes Docker nomeados ou diretórios externos com ACL restrita. Execute antes de publicar:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-no-secrets.ps1
```

Se uma credencial entrou num worktree, backup ou artefato compartilhado, movê-la não basta: revogue-a no provedor e gere uma substituta.

## Superfícies operacionais

- `/api/v1/actuator/health-check/public` expõe somente `ping`; GET/HEAD externos possuem limite por IP/global, sem compartilhar quota com o probe loopback. A isenção de loopback não se aplica a login nem recuperação.
- O Nginx publicado deve sobrescrever `X-Forwarded-For` com o endereço observado; nunca preserve cabeçalhos de encaminhamento enviados pelo cliente.
- `/api/v1/actuator/health-check`, `/api/v1/actuator/health-check/smtp` e `/api/v1/actuator/metrics/**` exigem `ROLE_ADMIN`. O grupo SMTP contém somente o indicador `mail` e não integra o liveness público ou o healthcheck do Compose.
- `/doc` exige Basic Auth com senha forte ou hash BCrypt.
- MySQL e phpMyAdmin só devem ser publicados em loopback e sob o perfil opt-in de desenvolvimento.

## Relato

Não abra issue pública com credenciais, dados pessoais ou exploração reproduzível. Envie o relato de forma privada ao responsável pelo repositório, incluindo impacto, versão/commit, passos mínimos e correção sugerida.
