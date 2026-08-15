# Runbook de desempenho e memória

## Browser

Execute uma build de produção e o orçamento:

```powershell
npm run build-only
npm run check:bundle
npm run test:memory
```

O teste aquece a aplicação, repete 20 ciclos de navegação e mede heap, documentos, nós e listeners após GC. Repita duas vezes. Crescimento monotônico, documentos retidos ou estouro do orçamento bloqueiam a entrega.

## JVM e banco

Com o stack iniciado e um token ADMIN efêmero, execute sem registrar o token:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/measure-runtime.ps1 -BaseUrl http://localhost:8080 -AdminToken $env:ADMIN_TOKEN -Iterations 100
```

O sampler mede p95, threads, heap/GC e conexões Hikari pendentes. Falha de requisição ou conexão persistentemente pendente encerra com código não zero. Compare duas amostras após aquecimento; crescimento monotônico exige heap dump/profiling antes de ampliar o pool.

## Evidência de fechamento — 12/08/2026

No Compose isolado, banco, API e frontend atingiram `healthy`. Três amostras aquecidas de 100 requisições registraram p95 entre 37,18 ms e 114,48 ms, sempre com zero conexões Hikari pendentes e 24–25 threads vivas. A amostra superior ocorreu após aplicar a cota de uma CPU. Logout retornou 204 e o replay do token revogado retornou 401 com `WWW-Authenticate: Bearer`.

O Compose limita CPU, memória e PIDs por serviço. A JVM usa percentuais explícitos de heap abaixo do limite do container. O probe loopback usa `/api/v1/actuator/health-check/public`, que executa somente `ping` e não compartilha a quota externa; a isenção não alcança login/recuperação. O Nginx sobrescreve `X-Forwarded-For`, GET/HEAD externos são limitados e o health completo, inclusive subrotas, permanece administrativo.

## Consultas e limites

- Consultas por cursor aceitam no máximo 100 registros/página, dez filtros e 100 valores selecionados por filtro.
- Preferências têm lote máximo de 20, quota de 100 por usuário e valor máximo de 16 KiB.
- Exportações do browser exigem progresso de cursor e limites de páginas, registros, bytes e tempo.
- RBAC e validação JWT carregam associações necessárias por `EntityGraph`, evitando uma consulta por permissão/usuário.
