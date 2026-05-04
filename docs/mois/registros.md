# Registros — MOIS

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

## 2026-05-03 18:45:13 UTC-3
- Ajustado o diagnóstico de coletas Hotmart no `MoisDomainService` para registrar explicitamente:
  - `method=GET` no início da tentativa;
  - `requestedUrl` e `finalUrl` nos logs de resposta rejeitada (`statusCode` fora de 2xx);
  - `requestedUrl`, `finalUrl`, `statusCode` e método no log de finalização.
- Objetivo: facilitar rastreabilidade de redirecionamentos HTTP 301 e identificar com precisão qual URL foi solicitada e qual URL final foi retornada pelo cliente HTTP.
- Commit relacionado: `356c904`.

## 2026-05-03 22:10:00 UTC-3
- Coleta Hotmart do MOIS aprimorada para reduzir falhas na extração de produtos em destaque.
- O parser da resposta do marketplace foi refatorado para:
  - manter a extração rica (título, link, descrição, produtor e imagem) quando os blocos completos estiverem presentes;
  - aplicar fallback de extração por URL de produto (`https://www.hotmart.com/product/...`) quando o HTML/JSON vier com estrutura parcial.
- O objetivo é manter geração de leads mesmo quando a Hotmart altera o shape do payload da vitrine.

## 2026-05-03 23:24:00 UTC-3
- Reintroduzida a etapa de persistência de estado de coleta no MOIS via backend principal, usando os endpoints:
  - `PUT /api/v1/mois/persistence/collection-jobs/{jobId}`
  - `GET /api/v1/mois/persistence/collection-jobs/{jobId}`
  - `GET /api/v1/mois/persistence/collection-jobs?workspaceId=&status=`
- O `MoisDomainService` deixou de operar exclusivamente em memória para coleção e passou a:
  - salvar progresso/resultado do job (`persistCollectionState`);
  - hidratar estado por workspace/status (`hydrateCollectionState`);
  - hidratar estado por job (`hydrateCollectionStateByJob`).
- Serialização JSON ajustada com descoberta automática de módulos Jackson para suportar `Instant` no payload de persistência.

## 2026-05-03 23:36:48 UTC-3
- Ajustados os testes unitários `MoisDomainServiceTest` para evitar flakiness causado por estado persistido de coletas do mesmo `workspaceId` entre execuções.
- Nos cenários de listagem de jobs e sumário operacional, os `workspaceId` agora são gerados com sufixo UUID por teste, garantindo isolamento sem depender de limpeza externa.
- Resultado esperado: os asserts de cardinalidade (`totalJobs` e tamanho da lista) voltam a refletir apenas os dados criados no próprio teste.

## 2026-05-04 10:40:00 UTC-3
- Endurecida a entrega de persistência MOIS com cobertura de testes unitários no backend (`MoisCollectionPersistenceServiceTest`).
- Validado via testes o comportamento de:
  - sincronização relacional por job em `mois_collected_reference` (delete + batch insert);
  - agregação executiva de destaques por fonte (`collection-highlights/by-source`) com ordenação por score médio e cálculo de sinal predominante.
- Registro operacional para rastrear revisão da PR "Persist collected references and add source-highlight aggregation endpoint" após feedback de qualidade.

## 2026-05-04 09:55:00 UTC-3
- Corrigida a resolução de include do Liquibase no `db.changelog-master.yaml` do `backend/ads-service`.
- O changeset `changesets/2026-04-28-mois-collection-job-state-persistence.yaml` passou a declarar `relativeToChangelogFile: true`, evitando falha de parsing em CI com mensagem de arquivo não encontrado no classpath.
- Registro criado para rastrear o incidente reportado no build Maven/Liquibase e sua correção.
