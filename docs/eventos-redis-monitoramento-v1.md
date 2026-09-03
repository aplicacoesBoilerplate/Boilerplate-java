# Contrato de eventos Redis para monitoramento — v1

## Estado e finalidade

Este documento define o contrato revisável da issue #24 para a futura implementação do canal de erros da #12. Ele não cria publisher, listener, stream HTTP ou estado de sessão em produção.

Redis Pub/Sub será somente um transporte opcional de notificações entre instâncias. MySQL continuará sendo a fonte de verdade dos erros e das sessões de monitoramento. Cache, rate limit e Pub/Sub compartilham a infraestrutura Redis, mas não compartilham interfaces, chaves, serializers nem regras de indisponibilidade.

O canal externo definido pelo plano da release é SSE. Uma futura camada de aplicação traduzirá eventos internos para SSE autenticado e autorizado; clientes web nunca assinam Redis diretamente. A proposta original de WebSocket da #12 fica substituída por esse limite de transporte.

## Semântica de entrega

Redis Pub/Sub entrega mensagens no máximo uma vez. Uma mensagem publicada enquanto o consumidor estiver desconectado é perdida e não existe confirmação, persistência, retenção ou replay. Portanto:

- o estado é confirmado no MySQL antes da publicação;
- a publicação ocorre somente após commit;
- falha de publicação não desfaz nem marca como falha a transação confirmada;
- ao conectar ou reconectar, o consumidor consulta primeiro o snapshot REST persistido e usa Pub/Sub/SSE apenas para mudanças posteriores;
- `eventId` e `Last-Event-ID` servem somente para deduplicação de uma entrega já observada; UUIDs aleatórios não detectam lacunas nem fornecem ordenação global;
- requisitos futuros de replay, grupos de consumidores ou entrega pelo menos uma vez exigem Redis Streams ou outro broker e uma nova versão arquitetural.

Não existe retenção no canal (`0 segundos`). A retenção de `log_errors` permanece controlada pelo banco e por `MAX_PERSISTED_ERRORS`; a futura sessão de monitoramento terá estado relacional próprio. O limite de conexões e o tempo de vida do SSE pertencem à #12.

## Canais

Cada mensagem usa UTF-8 e JSON conforme o schema [`contracts/monitoramento-evento-v1.schema.json`](contracts/monitoramento-evento-v1.schema.json).

| Evento | Canal lógico |
| --- | --- |
| Estado da sessão | `boilerplate:v1:<deploymentHash>:monitoramento:sessao:<tenantHash>` |
| Erro registrado | `boilerplate:v1:<deploymentHash>:monitoramento:erro:<tenantHash>` |

`deploymentHash` e `tenantHash` são os SHA-256 hexadecimais completos de `deploymentRef` e `tenantRef`, calculados pela aplicação. Esses identificadores evitam expor referências em comandos de introspecção do Redis; o envelope ainda carrega ambas para validação defensiva.

`deploymentRef` identifica de forma única a combinação aplicação + ambiente. O desenvolvimento standalone usa `boilerplate-java-local`; qualquer ambiente não local deve fornecer um valor explícito e a aplicação deve falhar ao iniciar se ele estiver ausente. Instâncias do mesmo deployment usam o mesmo valor. `tenantRef` usa `default` apenas no modo standalone; integrações multi-tenant usam o identificador estável do contrato de tenant, nunca nome, domínio ou e-mail do cliente. Todo subscriber valida igualdade exata de deployment e tenant antes de encaminhar o evento.

Hashes de canal evitam exposição acidental, mas não autenticam publishers. Deployments que compartilham Redis usam credenciais/ACLs próprias, limitadas ao prefixo de canais daquele deployment, além de rede privada e TLS quando houver trânsito de rede. Se esse isolamento não puder ser imposto, cada deployment usa uma instância Redis dedicada.

Se Redis Cluster for adotado, a implementação deve escolher e testar explicitamente Pub/Sub global ou Sharded Pub/Sub. Trocar `PUBLISH/SUBSCRIBE` por `SPUBLISH/SSUBSCRIBE` é decisão de implantação e não altera o envelope v1.

## Envelope v1

Campos comuns:

| Campo | Regra |
| --- | --- |
| `schemaVersion` | Inteiro fixo `1`. Mudança incompatível exige v2 e novo canal. |
| `eventId` | UUID único por tentativa lógica de publicação. Consumidores tratam duplicatas defensivamente. |
| `eventType` | `monitoramento.sessao.estado` ou `monitoramento.erro.registrado`. |
| `occurredAt` | Instante UTC em RFC 3339. |
| `deploymentRef` | Referência opaca e única para aplicação + ambiente, até 100 caracteres. |
| `tenantRef` | Referência opaca allowlistada, até 64 caracteres. |
| `correlationId` | UUID gerado pela API ou valor de entrada validado, até 128 caracteres allowlistados. |
| `severity` | `INFO` para sessão; `ERROR` ou `CRITICAL` para erro centralizado. |
| `producer` | Nome lógico do serviço, sem hostname, PID ou endereço de infraestrutura. |
| `data` | Payload específico e estritamente validado pelo schema. |

Exemplo de erro:

```json
{
  "schemaVersion": 1,
  "eventId": "d9788c66-2e44-47de-b8bc-91272f0c42ba",
  "eventType": "monitoramento.erro.registrado",
  "occurredAt": "2026-09-03T16:00:00Z",
  "deploymentRef": "boilerplate-java-local",
  "tenantRef": "default",
  "correlationId": "c33cc4cb-37df-4e31-a25c-22eb78d442fc",
  "severity": "ERROR",
  "producer": "boilerplate-java",
  "data": {
    "errorId": 1842,
    "code": "INTERNAL_ERROR",
    "httpStatus": 500,
    "summary": "Falha interna correlacionada para diagnóstico."
  }
}
```

Exemplo de controle:

```json
{
  "schemaVersion": 1,
  "eventId": "0aa377ef-dd9c-4a9e-9dfd-c3f99318549f",
  "eventType": "monitoramento.sessao.estado",
  "occurredAt": "2026-09-03T16:01:00Z",
  "deploymentRef": "boilerplate-java-local",
  "tenantRef": "default",
  "correlationId": "d6c0d3af-5f0c-4b4e-a522-681bb709f2c1",
  "severity": "INFO",
  "producer": "boilerplate-java",
  "data": {
    "sessionId": "d7f02450-8b43-4d74-af83-f18b12b9a9de",
    "state": "OPEN",
    "changedAt": "2026-09-03T16:01:00Z",
    "stateVersion": 1
  }
}
```

## Limites e dados proibidos

O JSON serializado deve ter no máximo 16 KiB. O publisher recusa payload maior, incrementa uma métrica sem labels de alta cardinalidade e não publica uma versão truncada que viole o schema. `summary` já chega sanitizado pelo tratamento central de erros e tem no máximo 500 caracteres.

É proibido publicar:

- token, cookie, senha, OTP, segredo, chave, cabeçalho de autorização ou corpo bruto de requisição;
- e-mail, telefone, nome, IP, user-agent ou identificador direto de usuário;
- stack trace, SQL, nome de tabela, credencial, path local, hostname ou variável de ambiente;
- exceção serializada, classe/arquivo/linha internos ou mapa arbitrário de contexto;
- matriz de permissões ou dados do cache.

O evento referencia o registro sanitizado por `errorId`; detalhes administrativos são consultados pela API protegida. Logs de falha de serialização/publicação registram somente `eventId`, `eventType`, `correlationId` e causa categorizada, nunca o payload.

### Classificação de severidade

- `INFO`: reservado ao evento de abertura/fechamento de sessão.
- `ERROR`: valor padrão para qualquer erro inesperado 5xx aceito pelo tratamento central.
- `CRITICAL`: somente quando um classificador interno allowlistado atribuir código de falha de integridade de dados, quebra de invariante de segurança ou indisponibilidade geral da aplicação. Nunca é inferido de mensagem, parâmetro do cliente ou classe arbitrária de exceção.

O mesmo código estável sempre produz a mesma severidade. Códigos novos entram primeiro no catálogo testado do tratamento central; na ausência de classificação explícita, o publisher usa `ERROR`.

## Ordem transacional e indisponibilidade

Na implementação futura:

1. a operação persiste o estado em MySQL;
2. um evento interno imutável é registrado na transação;
3. somente após commit, a camada de transporte serializa, valida tamanho/schema e publica;
4. cada instância assinante valida novamente versão, tipo, deployment, tenant e limites antes de encaminhar a sessões SSE autorizadas.

Rollback não publica. Falha do Redis após commit gera métrica/health degradado e log sanitizado; a requisição de negócio continua com o resultado do MySQL. Não há retry ilimitado em memória. Clientes recuperam o estado pelo snapshot REST ao reconectar. Uma instância pode encaminhar localmente seu próprio evento após commit, mas a ausência de fan-out entre instâncias deve permanecer observável enquanto Redis estiver fora.

O controle REST da sessão é idempotente. Abrir uma sessão já aberta ou fechar uma já fechada devolve o estado persistido sem criar eventos duplicados. Eventos concorrentes usam `occurredAt` e `stateVersion`, incrementado na fonte relacional, para que a API descarte estados antigos.

## Fronteiras de implementação da #12

A futura entrega deve:

- manter publishers/listeners fora de `IRedisCache` e usar serializer JSON UTF-8 dedicado;
- configurar lifecycle, executor limitado, error handler e encerramento explícito do `RedisMessageListenerContainer` se Spring Data Redis for usado;
- autenticar o SSE com Bearer, autorizar o tenant e a função administrativa antes de registrar a conexão;
- usar timeout, heartbeat, `SseEmitter` e limpeza explícita de conexões;
- remover conexões em completion, timeout, erro, logout e revogação;
- limitar conexões por usuário/tenant/instância e impedir duplicatas;
- consultar a fonte relacional no início/reconexão, tratar `Last-Event-ID` apenas como deduplicação e nunca prometer detecção de lacuna ou replay pelo Redis;
- testar duas instâncias, desconexão, Redis indisponível, payload inválido, tenant incorreto e ausência de vazamento.

## Compatibilidade e evolução

O envelope e os payloads v1 são fechados: produtores não adicionam campos sem publicar nova versão. Consumidores rejeitam `schemaVersion` não suportada, tipo desconhecido, campo adicional, payload acima do limite ou campo obrigatório inválido. Qualquer mudança de forma ou semântica exige schema/canais v2 e uma janela explícita de coexistência.

Referências técnicas consultadas:

- [Redis Pub/Sub: entrega at-most-once e ausência de replay](https://redis.io/docs/latest/develop/pubsub/)
- [Spring Data Redis: suporte a Pub/Sub](https://docs.spring.io/spring-data/redis/reference/redis/pubsub.html)
- [Spring Data Redis: listeners anotados e configuração do container](https://docs.spring.io/spring-data/redis/reference/redis/pubsub-annotated.html)
