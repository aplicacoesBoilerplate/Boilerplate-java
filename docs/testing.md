# Guia de testes

## Backend

O gate integral compila do zero, executa os testes Spring/JPA/segurança e empacota o JAR:

```powershell
.\mvnw.cmd -ntp clean verify
```

A suíte atual contém 51 testes. As regressões incluem o contrato Bearer 401, separação entre liveness público e health administrativo, rate limit de GET/HEAD, isolamento exclusivo do probe loopback e ausência de PII em falhas assíncronas de e-mail.

Os testes usam H2, bootstrap administrativo desligado e segredos exclusivos do perfil `test`. Não execute dois comandos Maven simultâneos no mesmo worktree, pois ambos escrevem em `target`.

Valide também configuração e ausência de arquivos sensíveis:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-no-secrets.ps1
docker compose config --quiet
```

O último comando requer variáveis de banco/JWT/OTP/documentação no processo; ele deve falhar quando um segredo obrigatório estiver ausente.

## Frontend

Na raiz de `frontends/Boilerplate-vue`:

```powershell
npm run verify
npm run test:memory
```

`verify` encadeia lint, tipos, 18 testes unitários, build, orçamento de bundle e 25 cenários E2E. Os testes unitários também garantem que o Nginx sobrescreva `X-Forwarded-For` em todas as fronteiras da API. O cenário de memória repete navegação e coleta heap/DOM após GC no Chromium; `test:memory` permite executá-lo isoladamente.

## Dependências

```powershell
npm audit --omit=dev
npm audit
```

Não use `--force` para esconder incompatibilidades. Remova dependências sem uso e reexecute build/testes quando trocar bibliotecas de exportação ou UI.
