# Protocolo de Histórico de Implantação — MDS

## Objetivo

Este documento define como o modelo, o Codex ou qualquer agente de desenvolvimento deve registrar, de forma cumulativa e padronizada, o histórico do que foi feito no módulo **MDS (Mechanism Discovery Service)** dentro do **Marketing Hub**.

O histórico existe para:

- deixar claro o que foi implementado em cada etapa;
- registrar contratos, artefatos, tabelas, APIs e fluxos afetados;
- evitar retrabalho e perda de contexto entre chats, sprints e handoffs;
- permitir auditoria técnica do que mudou, por que mudou e o que ainda ficou pendente;
- apoiar continuidade segura por outros agentes ou desenvolvedores.

---

## Alinhamento com o repositório

Este protocolo segue a regra operacional do repositório do Marketing Hub:

- o banco é **MySQL 5.7**;
- **somente o backend principal acessa o banco**;
- os demais módulos conversam via **APIs explícitas do backend**;
- mudanças cross-módulo devem sincronizar contratos e documentação.

Isso significa que, ao registrar o histórico do MDS, o modelo deve explicitar sempre que uma mudança envolver:

- migration ou mudança de tabela no backend;
- novo endpoint do backend;
- alteração de contrato backend ↔ MDS;
- novos artefatos ou mudanças de schema;
- impactos em lineage, versionamento, persistência ou governança.

---

## Princípios

1. O histórico deve ser **incremental e cumulativo**.
2. O histórico deve registrar **mudanças relevantes**, não despejar logs brutos.
3. Cada entrada deve ser **factual, objetiva e verificável**.
4. O histórico deve registrar tanto **o que foi concluído** quanto **limitações, riscos e pendências**.
5. Sempre que possível, cada entrada deve apontar:
   - arquivos alterados;
   - contratos afetados;
   - tabelas criadas ou modificadas;
   - endpoints criados ou alterados;
   - artefatos impactados;
   - testes executados;
   - próximos passos.
6. O histórico não substitui documentação canônica, mas serve como **verdade operacional da implantação**.

---

## Regras de preenchimento para o modelo

Sempre que concluir uma etapa relevante do MDS, adicionar uma nova entrada no final deste documento.

O modelo deve registrar nova entrada quando houver pelo menos um destes casos:

- criação ou alteração de schema canônico do MDS;
- criação ou alteração de tabela no banco;
- criação ou alteração de endpoint do backend;
- criação ou alteração de job, worker ou fluxo assíncrono;
- criação ou alteração de contrato entre backend e módulo;
- criação ou alteração de lineage, versionamento ou governança;
- criação ou alteração de persistência de artefatos;
- criação ou alteração de testes de contrato, integração ou validação;
- correção estrutural relevante;
- mudança arquitetural importante.

O modelo **não deve** abrir entrada nova para microajustes irrelevantes, refactors cosméticos ou mudanças sem impacto funcional ou contratual.

---

## Formato obrigatório de cada entrada

Cada entrada deve seguir exatamente esta estrutura.

## [DATA] — Etapa: <nome curto da etapa>

**Status:** `<PLANEJADO | EM_ANDAMENTO | CONCLUIDO | PARCIAL | BLOQUEADO>`

**Resumo:**

Texto curto explicando o que foi feito nesta etapa.

**Objetivo da etapa:**

Descrever qual problema esta etapa resolveu ou tentou resolver.

**O que foi implementado:**

- item 1
- item 2
- item 3

**Arquivos alterados/criados:**

- caminho/arquivo-1
- caminho/arquivo-2
- caminho/arquivo-3

**Tabelas / persistência afetadas:**

- tabela 1
- tabela 2
- ou `nenhuma`

**APIs / endpoints / contratos afetados:**

- endpoint ou contrato 1
- endpoint ou contrato 2
- ou `nenhum`

**Artefatos / schemas impactados:**

- mds.sourceDocument.v1
- mds.evidenceItem.v1
- mds.mechanismCandidate.v1
- ou `nenhum`

**Testes executados:**

- teste 1
- teste 2
- ou `não executado ainda`

**Resultado observado:**

Descrever o efeito prático da mudança.

**Limitações / pendências:**

- pendência 1
- pendência 2
- ou `nenhuma relevante`

**Próximo passo sugerido:**

Texto curto com a próxima etapa mais lógica.

---

## Convenções obrigatórias

### 1. Ordem
- As entradas devem ser adicionadas em ordem cronológica.
- A mais nova sempre entra no final.

### 2. Nível de detalhe
- Ser objetivo, mas suficiente para outro agente continuar o trabalho sem adivinhar.
- Não usar frases genéricas como “ajustado conforme necessário”.

### 3. Verdade factual
- Não afirmar que algo foi concluído se não foi realmente implementado.
- Quando algo estiver parcialmente pronto, usar `PARCIAL`.
- Quando houver impedimento real, usar `BLOQUEADO` e explicar.

### 4. Nome das etapas
Preferir nomes claros, por exemplo:
- schema inicial do MDS
- persistência inicial em MySQL via backend
- endpoint de publicação de artefatos
- lineage de artefatos
- contract tests backend ↔ MDS
- hardening de versionamento

### 5. Relação com migração de banco
Se a etapa envolver banco, registrar explicitamente:
- migration/changelog criado;
- tabelas afetadas;
- compatibilidade com ambiente atual;
- rollback ou ausência de rollback.

### 6. Relação com artefatos
Se a etapa envolver artefatos, registrar explicitamente:
- tipos de artefato impactados;
- `schemaVersion`;
- compatibilidade ou breaking change.

---

## Template pronto para copiar

## [AAAA-MM-DD] — Etapa: <nome curto da etapa>

**Status:** `<PLANEJADO | EM_ANDAMENTO | CONCLUIDO | PARCIAL | BLOQUEADO>`

**Resumo:**

<resumo curto e objetivo>

**Objetivo da etapa:**

<problema resolvido ou alvo da etapa>

**O que foi implementado:**

- <item>
- <item>
- <item>

**Arquivos alterados/criados:**

- <arquivo>
- <arquivo>
- <arquivo>

**Tabelas / persistência afetadas:**

- <tabela>
- <tabela>
- ou `nenhuma`

**APIs / endpoints / contratos afetados:**

- <endpoint ou contrato>
- <endpoint ou contrato>
- ou `nenhum`

**Artefatos / schemas impactados:**

- <artifactType>
- <artifactType>
- ou `nenhum`

**Testes executados:**

- <teste>
- <teste>
- ou `não executado ainda`

**Resultado observado:**

<efeito prático observado>

**Limitações / pendências:**

- <pendência>
- <pendência>
- ou `nenhuma relevante`

**Próximo passo sugerido:**

<próxima etapa recomendada>

---

## Exemplo de entrada

## [2026-04-16] — Etapa: persistência inicial de artefatos do MDS em MySQL via backend

**Status:** `CONCLUIDO`

**Resumo:**

Foi definida a primeira camada de persistência do MDS no backend principal usando MySQL como catálogo transacional central.

**Objetivo da etapa:**

Permitir que os artefatos do MDS sejam armazenados de forma versionada e governada sem depender, na primeira fase, de stack adicional de busca semântica ou object storage.

**O que foi implementado:**

- definição da estratégia de persistência inicial no backend principal;
- decisão de usar tabela de artefatos com colunas fixas + `content_json`;
- decisão de manter lineage em tabela própria;
- separação entre catálogo transacional e futuras extensões.

**Arquivos alterados/criados:**

- docs/novos-modulos/MDS/protocolo_historico_implantacao_mds.md
- docs/novos-modulos/MDS/mds_documento_canonico_artefatos.md
- docs/novos-modulos/MDS/plano_basico_implementacao_mds_por_sprints.md

**Tabelas / persistência afetadas:**

- artifact_record
- artifact_lineage_edge
- source_access_record

**APIs / endpoints / contratos afetados:**

- contrato interno de publicação de artefatos do MDS para o backend

**Artefatos / schemas impactados:**

- mds.sourceDocument.v1
- mds.evidenceItem.v1
- mds.mechanismCandidate.v1
- mds.mechanismSpec.v1
- mds.practicalKnowledgePack.v1

**Testes executados:**

- não executado ainda

**Resultado observado:**

A direção arquitetural ficou definida e pronta para virar DDL, migrations e endpoints do backend.

**Limitações / pendências:**

- DDL ainda não gerado;
- migrations ainda não criadas;
- endpoints ainda não implementados.

**Próximo passo sugerido:**

Gerar o DDL inicial do MySQL e o contrato backend ↔ MDS para publicação de artefatos.

---

## Regra final de uso pelo modelo

Ao fim de cada etapa relevante, o modelo deve:

1. revisar o que realmente foi feito;
2. adicionar uma nova entrada neste histórico;
3. não reescrever entradas antigas, exceto para correção factual;
4. registrar pendências reais;
5. manter o documento utilizável como handoff entre chats e entre agentes.

## [2026-04-17] — Etapa: sprint 1 - persistência e orquestração base no backend

**Status:** `CONCLUIDO`

**Resumo:**

Foi implementada a base de Sprint 1 do MDS no backend principal, com persistência MySQL 5.7 via Liquibase, endpoints internos de orquestração de requests e publicação inicial de artefatos com lineage.

**Objetivo da etapa:**

Preparar o `backend/ads-service` como camada única de persistência e orquestração para o MDS, sem antecipar o módulo worker da Sprint 2.

**O que foi implementado:**

- Criação do namespace `com.marketinghub.mds` no backend com entidades e repositórios para as tabelas mínimas da sprint.
- Implementação dos serviços de request lifecycle (`create`, `pending`, `claim`, `heartbeat`, `complete`, `fail`) e publicação em lote de artefatos.
- Implementação de endpoints internos em `/api/internal/mds` para requests, artifact publish batch, lineage e health.
- Criação de changelog Liquibase YAML da Sprint 1 com `preConditions` para MySQL e SQL com `splitStatements` / `stripComments`.
- Documentação do contrato backend ↔ MDS da Sprint 1 em arquivo dedicado.

**Arquivos alterados/criados:**

- backend/ads-service/src/main/java/com/marketinghub/mds/MdsRequest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/MdsArtifactRecord.java
- backend/ads-service/src/main/java/com/marketinghub/mds/MdsArtifactLineageEdge.java
- backend/ads-service/src/main/java/com/marketinghub/mds/MdsSourceAccessRecord.java
- backend/ads-service/src/main/java/com/marketinghub/mds/MdsProcessingEvent.java
- backend/ads-service/src/main/java/com/marketinghub/mds/MdsRequestStatus.java
- backend/ads-service/src/main/java/com/marketinghub/mds/MdsArtifactStatus.java
- backend/ads-service/src/main/java/com/marketinghub/mds/MdsEventType.java
- backend/ads-service/src/main/java/com/marketinghub/mds/repository/MdsRequestRepository.java
- backend/ads-service/src/main/java/com/marketinghub/mds/repository/MdsArtifactRecordRepository.java
- backend/ads-service/src/main/java/com/marketinghub/mds/repository/MdsArtifactLineageEdgeRepository.java
- backend/ads-service/src/main/java/com/marketinghub/mds/repository/MdsSourceAccessRecordRepository.java
- backend/ads-service/src/main/java/com/marketinghub/mds/repository/MdsProcessingEventRepository.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsRequestCreateRequest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsRequestStatusResponse.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsClaimRequest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsHeartbeatRequest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsCompleteRequest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsFailRequest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsArtifactPublishBatchRequest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsArtifactPublishBatchResponse.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsLineageCreateRequest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsLineageResponse.java
- backend/ads-service/src/main/java/com/marketinghub/mds/service/MdsRequestService.java
- backend/ads-service/src/main/java/com/marketinghub/mds/service/MdsArtifactService.java
- backend/ads-service/src/main/java/com/marketinghub/mds/web/MdsInternalController.java
- backend/ads-service/src/main/resources/db/changelog/changesets/2026-04-17-mds-sprint1-base.yaml
- backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml
- docs/novos-modulos/MDS/contrato_backend_mds.md
- docs/novos-modulos/MDS/plano_implementacao_mds_baseado_na_especificacao.md

**Tabelas / persistência afetadas:**

- `mds_request`
- `artifact_record`
- `artifact_lineage_edge`
- `source_access_record`
- `mds_processing_event`

**APIs / endpoints / contratos afetados:**

- `POST /api/internal/mds/requests`
- `GET /api/internal/mds/requests/pending`
- `POST /api/internal/mds/requests/{id}/claim`
- `POST /api/internal/mds/requests/{id}/heartbeat`
- `POST /api/internal/mds/requests/{id}/complete`
- `POST /api/internal/mds/requests/{id}/fail`
- `GET /api/internal/mds/requests/{id}`
- `POST /api/internal/mds/artifacts/publish-batch`
- `POST /api/internal/mds/artifacts/{id}/lineage`
- `GET /api/internal/mds/health`
- `docs/novos-modulos/MDS/contrato_backend_mds.md`

**Artefatos / schemas impactados:**

- `mechanismDiscoveryRequest`
- `artifact_record` envelope canônico para tipos MDS (status/version/schemaVersion/hash)
- `artifact_lineage_edge`

**Testes executados:**

- `cd backend/ads-service && mvn -s ../settings.xml test`

**Resultado observado:**

O backend agora consegue registrar requests de discovery, controlar ciclo de execução básico e receber lote de artefatos com persistência de lineage no MySQL por meio de contratos internos.

**Limitações / pendências:**

- Não há módulo `mds/` executando loop de processamento ainda (escopo da Sprint 2).
- Não há busca externa de evidências, deduplicação e análise científica nesta etapa.
- Não há contract tests ponta a ponta com serviço MDS externo nesta sprint.

**Próximo passo sugerido:**

Iniciar Sprint 2 com criação do módulo `mds/` independente e integração ativa com endpoints internos do backend já implementados.

## [2026-04-17] — Etapa: modulo mds independente (bootstrap Spring Boot)

**Status:** `PARCIAL`

**Resumo:**

Foi criado o módulo `mds/` como serviço independente na raiz do repositório, com projeto Maven Spring Boot completo e loop básico de integração com endpoints internos do backend, sem acesso direto ao banco.

**Objetivo da etapa:**

Atender a diretriz de desacoplamento do MDS em container próprio, preservando a regra de persistência exclusiva do backend.

**O que foi implementado:**

- Novo projeto `mds/` com `pom.xml`, `Dockerfile`, `README.md`, `application.yml`, bootstrap Java e testes.
- Cliente HTTP interno para consumo dos endpoints `/api/internal/mds/*` do backend.
- Loop básico (`MdsLoopRunner`) com polling de pendências, claim, heartbeat, execução de pipeline stub e complete/fail.
- Serviço de pipeline inicial (`MechanismDiscoveryPipelineService`) com publicação de artefatos stub no backend.
- Endpoint de health do serviço em `/internal/mechanism-discovery/actuator/health`.

**Arquivos alterados/criados:**

- mds/pom.xml
- mds/Dockerfile
- mds/README.md
- mds/src/main/resources/application.yml
- mds/src/main/java/com/marketinghub/mds/MdsApplication.java
- mds/src/main/java/com/marketinghub/mds/config/MdsProperties.java
- mds/src/main/java/com/marketinghub/mds/config/BackendClientConfig.java
- mds/src/main/java/com/marketinghub/mds/client/BackendMdsClient.java
- mds/src/main/java/com/marketinghub/mds/controller/MdsHealthController.java
- mds/src/main/java/com/marketinghub/mds/service/MdsLoopRunner.java
- mds/src/main/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineService.java
- mds/src/main/java/com/marketinghub/mds/dto/*
- mds/src/test/java/com/marketinghub/mds/MdsApplicationTests.java
- mds/src/test/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineServiceTest.java
- docs/novos-modulos/MDS/plano_implementacao_mds_baseado_na_especificacao.md
- docs/novos-modulos/MDS/protocolo_historico_implantacao_mds.md

**Tabelas / persistência afetadas:**

- `nenhuma` (o módulo `mds/` não acessa banco diretamente)

**APIs / endpoints / contratos afetados:**

- consumo de `GET /api/internal/mds/requests/pending`
- consumo de `POST /api/internal/mds/requests/{id}/claim`
- consumo de `POST /api/internal/mds/requests/{id}/heartbeat`
- consumo de `POST /api/internal/mds/requests/{id}/complete`
- consumo de `POST /api/internal/mds/requests/{id}/fail`
- consumo de `POST /api/internal/mds/artifacts/publish-batch`
- criação de `GET /internal/mechanism-discovery/actuator/health` no módulo independente

**Artefatos / schemas impactados:**

- `mechanismSpec` (stub inicial)
- `practicalKnowledgePack` (stub inicial)

**Testes executados:**

- `cd mds && mvn test`

**Resultado observado:**

O MDS passa a existir como serviço executável independente em JAR/Container, integrado ao backend apenas por HTTP interno para persistência e ciclo de execução.

**Limitações / pendências:**

- Pipeline ainda é stub, sem busca científica real.
- Sem implementação das etapas avançadas previstas nas sprints seguintes.

**Próximo passo sugerido:**

Implementar as etapas de discovery real (formulação de perguntas, busca em fontes externas, deduplicação e análise de evidência) mantendo persistência exclusivamente via backend.

## [2026-04-17] — Etapa: sprint 2 concluída com bootstrap do módulo MDS e loop básico

**Status:** `CONCLUIDO`

**Resumo:**

A Sprint 2 foi finalizada com o módulo `mds/` operacional no fluxo mínimo de orquestração com backend, incluindo ajustes de lifecycle e testes iniciais de contrato.

**Objetivo da etapa:**

Concluir a integração básica MDS ↔ backend para consumo de requests pendentes com claim/heartbeat/complete/fail, sem antecipar o pipeline científico da Sprint 3.

**O que foi implementado:**

- Endurecimento do lifecycle no backend: `heartbeat`, `complete` e `fail` restritos a requests em `IN_PROGRESS`.
- Testes de serviço no backend cobrindo transições de estado válidas e rejeição de transição inválida.
- Contract tests iniciais no controller interno do backend cobrindo contratos de `claim`, `lineage` e `publish-batch`.
- Testes de loop no módulo MDS validando fluxo de sucesso (claim + heartbeat + execute + complete) e fluxo de falha controlada (claim + heartbeat + fail).

**Arquivos alterados/criados:**

- backend/ads-service/src/main/java/com/marketinghub/mds/service/MdsRequestService.java
- backend/ads-service/src/test/java/com/marketinghub/mds/service/MdsRequestServiceTest.java
- backend/ads-service/src/test/java/com/marketinghub/mds/web/MdsInternalControllerContractTest.java
- mds/src/test/java/com/marketinghub/mds/service/MdsLoopRunnerTest.java
- docs/novos-modulos/MDS/plano_implementacao_mds_baseado_na_especificacao.md
- docs/novos-modulos/MDS/protocolo_historico_implantacao_mds.md

**Tabelas / persistência afetadas:**

- `nenhuma`

**APIs / endpoints / contratos afetados:**

- `POST /api/internal/mds/requests/{id}/claim`
- `POST /api/internal/mds/requests/{id}/heartbeat`
- `POST /api/internal/mds/requests/{id}/complete`
- `POST /api/internal/mds/requests/{id}/fail`
- `POST /api/internal/mds/artifacts/publish-batch`
- `POST /api/internal/mds/artifacts/{id}/lineage`

**Artefatos / schemas impactados:**

- `nenhum`

**Testes executados:**

- `cd backend/ads-service && mvn -Dtest=MdsRequestServiceTest,MdsInternalControllerContractTest test`
- `cd mds && mvn test`

**Resultado observado:**

O módulo MDS processa requests pendentes no ciclo mínimo de orquestração e o backend aplica transições de estado mais seguras para o lifecycle do request.

**Limitações / pendências:**

- Sem implementação de discovery real (question builder, busca externa, deduplicação, triagem e análise de evidências).
- Sem persistência operacional de `mechanismEvidenceSearch` e `sourceDocument`.
- Sem hardening de observabilidade/retries avançados.

**Próximo passo sugerido:**

Iniciar Sprint 3 com formulação de perguntas de mecanismo e busca estruturada em fontes externas permitidas, mantendo persistência exclusivamente via backend.

## [2026-04-17] — Etapa: sprint 3 - formulação de pergunta e busca estruturada

**Status:** `CONCLUIDO`

**Resumo:**

A Sprint 3 implementou o pipeline inicial de descoberta real no módulo `mds`, com formulação de pergunta, planejamento de busca por fontes científicas, execução em APIs externas e persistência via backend de `mechanismEvidenceSearch`, `sourceDocument` e `source_access_record`.

**Objetivo da etapa:**

Sair do pipeline stubado da Sprint 2 e estabelecer a primeira trilha rastreável de descoberta com busca estruturada e persistência dos resultados normalizados sem acesso direto ao MySQL pelo MDS.

**O que foi implementado:**

- criação de `MechanismQuestionBuilder` e `SearchQueryPlanBuilder` para gerar pergunta de mecanismo e queries rastreáveis;
- criação de clientes reais de busca (`PubmedSearchClient` e `CrossrefSearchClient`) e execução agregada no `SearchExecutionService`;
- atualização do `MechanismDiscoveryPipelineService` para publicar `mechanismEvidenceSearch` e `sourceDocument`;
- classificação inicial de acesso (`open_access`, `metadata_only`, `restricted`) e permissão (`can_download`, `can_text_mine`, `link_only`);
- novo contrato backend para gravação em lote de `source_access_record` com endpoint interno específico;
- cobertura de testes de contrato/backend e testes de serviço no módulo `mds`.

**Arquivos alterados/criados:**

- mds/src/main/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineService.java
- mds/src/main/java/com/marketinghub/mds/client/BackendMdsClient.java
- mds/src/main/java/com/marketinghub/mds/dto/BackendSourceAccessPublishBatchRequestDto.java
- mds/src/main/java/com/marketinghub/mds/dto/BackendSourceAccessPublishBatchResponseDto.java
- mds/src/main/java/com/marketinghub/mds/search/MechanismQuestion.java
- mds/src/main/java/com/marketinghub/mds/search/MechanismQuestionBuilder.java
- mds/src/main/java/com/marketinghub/mds/search/SearchQueryPlan.java
- mds/src/main/java/com/marketinghub/mds/search/SearchQueryPlanBuilder.java
- mds/src/main/java/com/marketinghub/mds/search/SearchExecutionService.java
- mds/src/main/java/com/marketinghub/mds/search/EvidenceSearchClient.java
- mds/src/main/java/com/marketinghub/mds/search/PubmedSearchClient.java
- mds/src/main/java/com/marketinghub/mds/search/CrossrefSearchClient.java
- mds/src/test/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineServiceTest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/web/MdsInternalController.java
- backend/ads-service/src/main/java/com/marketinghub/mds/service/MdsSourceAccessService.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsSourceAccessPublishBatchRequest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsSourceAccessPublishBatchResponse.java
- backend/ads-service/src/test/java/com/marketinghub/mds/web/MdsInternalControllerContractTest.java
- docs/novos-modulos/MDS/plano_implementacao_mds_baseado_na_especificacao.md
- docs/novos-modulos/MDS/protocolo_historico_implantacao_mds.md

**Tabelas / persistência afetadas:**

- artifact_record (novos tipos persistidos: `mechanismEvidenceSearch`, `sourceDocument`)
- source_access_record (persistência em lote adicionada)

**APIs / endpoints / contratos afetados:**

- POST /api/internal/mds/artifacts/publish-batch (uso expandido para novos artefatos da Sprint 3)
- POST /api/internal/mds/source-access/publish-batch (novo endpoint interno para `source_access_record`)

**Artefatos / schemas impactados:**

- mds.mechanismEvidenceSearch.v1
- mds.sourceDocument.v1

**Testes executados:**

- mds: `mvn test`
- backend/ads-service: `mvn -Dtest=MdsInternalControllerContractTest test`

**Resultado observado:**

O MDS agora executa busca estruturada em fontes científicas, registra a estratégia de busca e publica documentos normalizados com classificação inicial de acesso/permissão, mantendo a governança de persistência via backend.

**Limitações / pendências:**

- deduplicação ainda não implementada com regras completas por DOI/PMID/PMCID/título/URL;
- triagem e confiança científica ainda não transformam resultados em `evidenceItem`;
- classificação de acesso/permissão ainda depende de heurísticas simples por metadados.

**Próximo passo sugerido:**

Implementar a Sprint 4 com deduplicação, triagem estruturada, `EvidenceConfidenceService` e persistência de `evidenceItem`.

## [2026-04-17] — Etapa: sprint 4 - normalização, deduplicação, triagem e evidenceItem

**Status:** `CONCLUIDO`

**Resumo:**

A Sprint 4 foi implementada no módulo `mds/` com deduplicação de documentos, triagem por relevância/aplicabilidade, classificação de confiança e publicação de `evidenceItem` com lineage para os `sourceDocument` de origem.

**Objetivo da etapa:**

Transformar os resultados brutos da busca da Sprint 3 em evidências priorizadas e persistíveis no backend, mantendo rastreabilidade e eventos por etapa.

**O que foi implementado:**

- criação de `SourceDedupService` com regras por DOI, PMID, PMCID, título normalizado e URL canônica;
- criação de `EvidenceScreeningService` para triagem mínima e priorização de evidências;
- criação de `EvidenceConfidenceService` com níveis `alta`, `moderada`, `baixa` e `muito_baixa`;
- criação de `EvidenceItemBuilder` para montar `evidenceItem` com limitação, proximidade com problema, aplicabilidade ao nicho e sinais de força da evidência;
- atualização do `MechanismDiscoveryPipelineService` para publicar `evidenceItem` e ligar lineage ao `sourceDocument` via `parentArtifactIds`;
- heartbeat por etapa (`dedup-normalize`, `screening`, `evidence-analysis`) para registrar eventos operacionais no backend.

**Arquivos alterados/criados:**

- mds/src/main/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineService.java
- mds/src/main/java/com/marketinghub/mds/search/SourceDedupService.java
- mds/src/main/java/com/marketinghub/mds/search/EvidenceScreeningService.java
- mds/src/main/java/com/marketinghub/mds/search/EvidenceConfidenceService.java
- mds/src/main/java/com/marketinghub/mds/search/EvidenceItemBuilder.java
- mds/src/main/java/com/marketinghub/mds/search/ScreenedEvidence.java
- mds/src/test/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineServiceTest.java
- mds/src/test/java/com/marketinghub/mds/search/SourceDedupServiceTest.java
- docs/novos-modulos/MDS/plano_implementacao_mds_baseado_na_especificacao.md
- docs/novos-modulos/MDS/protocolo_historico_implantacao_mds.md

**Tabelas / persistência afetadas:**

- `artifact_record` (novo tipo persistido: `evidenceItem`)
- `artifact_lineage_edge` (lineage `evidenceItem` -> `sourceDocument`)
- `mds_processing_event` (eventos adicionais por etapa do pipeline)

**APIs / endpoints / contratos afetados:**

- `POST /api/internal/mds/artifacts/publish-batch` (uso expandido para `evidenceItem`)
- `POST /api/internal/mds/requests/{id}/heartbeat` (uso expandido para eventos de etapa da Sprint 4)

**Artefatos / schemas impactados:**

- `mds.evidenceItem.v1`
- `mds.sourceDocument.v1` (lineage consumido como origem)
- `mds.mechanismEvidenceSearch.v1` (metadados de deduplicação/triagem)

**Testes executados:**

- `cd mds && mvn test`

**Resultado observado:**

O MDS agora reduz duplicatas de fontes, aplica triagem mínima, classifica confiança e publica `evidenceItem` com rastreabilidade para o documento fonte, preparando o terreno para construção de mecanismos candidatos na sprint seguinte.

**Limitações / pendências:**

- heurísticas de triagem e confiança ainda não avaliam profundidade metodológica completa dos estudos;
- extração de componentes ativos e recomendação de mecanismo não fazem parte desta sprint.

**Próximo passo sugerido:**

Implementar a Sprint 5 com extração de componentes ativos, montagem de `mechanismCandidate` e seleção do mecanismo recomendado.

## [2026-04-17] — Etapa: mechanismCandidate e mechanismSpec com recomendação final

**Status:** `CONCLUIDO`

**Resumo:**

Foi implementada a etapa de construção de `mechanismCandidate` e seleção do mecanismo recomendado com publicação de `mechanismSpec`, incluindo leitura do mecanismo final por request no backend.

**Objetivo da etapa:**

Concluir o núcleo da Sprint 5 para transformar evidências priorizadas em mecanismo candidato rastreável, selecionar recomendação com justificativa explícita e persistir `mechanismSpec` com lineage.

**O que foi implementado:**

- criação do `ActiveComponentExtractor` para extração heurística de componentes ativos a partir de título e resumo das evidências;
- criação do `MechanismCandidateBuilder` para agrupamento de componentes recorrentes, separação essencial/opcional, agregação de limitações e cálculo de confiança consolidada;
- atualização do pipeline (`MechanismDiscoveryPipelineService`) para publicar `mechanismCandidate` e `mechanismSpec` após publicação de `evidenceItem`, com lineage até artefatos de evidência;
- criação do endpoint backend `GET /api/internal/mds/requests/{id}/recommended-mechanism` para leitura do mecanismo recomendado por request com base no último `mechanismSpec`;
- ampliação de testes de unidade e contrato para cobrir a nova etapa de construção e leitura do mecanismo final.

**Arquivos alterados/criados:**

- mds/src/main/java/com/marketinghub/mds/search/ActiveComponentExtractor.java
- mds/src/main/java/com/marketinghub/mds/search/MechanismCandidateBuilder.java
- mds/src/main/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineService.java
- mds/src/test/java/com/marketinghub/mds/search/ActiveComponentExtractorTest.java
- mds/src/test/java/com/marketinghub/mds/search/MechanismCandidateBuilderTest.java
- mds/src/test/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineServiceTest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsRecommendedMechanismResponse.java
- backend/ads-service/src/main/java/com/marketinghub/mds/repository/MdsArtifactRecordRepository.java
- backend/ads-service/src/main/java/com/marketinghub/mds/service/MdsArtifactService.java
- backend/ads-service/src/main/java/com/marketinghub/mds/web/MdsInternalController.java
- backend/ads-service/src/test/java/com/marketinghub/mds/web/MdsInternalControllerContractTest.java
- docs/novos-modulos/MDS/plano_implementacao_mds_baseado_na_especificacao.md
- docs/novos-modulos/MDS/protocolo_historico_implantacao_mds.md

**Tabelas / persistência afetadas:**

- artifact_record
- artifact_lineage_edge

**APIs / endpoints / contratos afetados:**

- `POST /api/internal/mds/artifacts/publish-batch` (uso ampliado para `mechanismCandidate` e `mechanismSpec`)
- `GET /api/internal/mds/requests/{id}/recommended-mechanism` (novo endpoint)

**Artefatos / schemas impactados:**

- mds.mechanismCandidate.v1
- mds.mechanismSpec.v1

**Testes executados:**

- `cd mds && mvn test -Dtest=MechanismDiscoveryPipelineServiceTest,ActiveComponentExtractorTest,MechanismCandidateBuilderTest`
- `cd backend/ads-service && mvn test -Dtest=MdsInternalControllerContractTest`

**Resultado observado:**

Requests com evidência priorizada agora geram `mechanismCandidate`, escolhem um mecanismo recomendado com justificativa explícita e publicam `mechanismSpec` com lineage para suporte rastreável no backend.

**Limitações / pendências:**

- `practicalKnowledgePack` não foi implementado nesta etapa;
- relatório final (`mechanismDiscoveryReport`) permanece pendente;
- extração de componentes ainda é heurística lexical (sem clusterização semântica avançada).

**Próximo passo sugerido:**

Implementar a Sprint 6 para publicação de `practicalKnowledgePack`, relatório final e fechamento completo do pacote de saída do MDS.

## [2026-04-17] — Etapa: sprint 6 - practicalKnowledgePack, relatório final e publicação completa

**Status:** `CONCLUIDO`

**Resumo:**

A Sprint 6 foi implementada com publicação completa dos artefatos finais (`practicalKnowledgePack` e `mechanismDiscoveryReport`) e com os endpoints de consulta de relatório/listagem de artefatos no backend.

**Objetivo da etapa:**

Fechar o fluxo V1 do MDS com saída reutilizável por outros módulos, relatório final consultável e contratos de leitura alinhados ao plano da sprint.

**O que foi implementado:**

- criação de `PracticalKnowledgePackBuilder` para gerar as quatro variantes exigidas na sprint (técnica, executiva, prática para design de produto e simplificada para consumidor final);
- criação de `DiscoveryReportBuilder` para consolidar status final, métricas operacionais e referências dos artefatos de saída;
- atualização do `MechanismDiscoveryPipelineService` para publicar `mechanismSpec`, `practicalKnowledgePack` e `mechanismDiscoveryReport` em sequência, com heartbeat de `pack-building` e `reporting`;
- criação do endpoint `GET /api/internal/mds/reports/{id}` para recuperar relatório final por request;
- criação do endpoint `GET /api/internal/mds/requests/{id}/artifacts` para listar artefatos publicados da request;
- atualização dos testes de contrato e de pipeline para cobrir o fluxo completo da Sprint 6.

**Arquivos alterados/criados:**

- mds/src/main/java/com/marketinghub/mds/search/PracticalKnowledgePackBuilder.java
- mds/src/main/java/com/marketinghub/mds/search/DiscoveryReportBuilder.java
- mds/src/main/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineService.java
- mds/src/test/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineServiceTest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/web/MdsInternalController.java
- backend/ads-service/src/main/java/com/marketinghub/mds/service/MdsArtifactService.java
- backend/ads-service/src/main/java/com/marketinghub/mds/repository/MdsArtifactRecordRepository.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsReportResponse.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsArtifactSummaryResponse.java
- backend/ads-service/src/test/java/com/marketinghub/mds/web/MdsInternalControllerContractTest.java
- docs/novos-modulos/MDS/plano_implementacao_mds_baseado_na_especificacao.md
- docs/novos-modulos/MDS/protocolo_historico_implantacao_mds.md

**Tabelas / persistência afetadas:**

- artifact_record (novos tipos persistidos: `practicalKnowledgePack` e `mechanismDiscoveryReport`)
- artifact_lineage_edge (lineage adicional entre `evidenceItem`, `mechanismSpec`, `practicalKnowledgePack` e `mechanismDiscoveryReport`)

**APIs / endpoints / contratos afetados:**

- `POST /api/internal/mds/artifacts/publish-batch` (uso expandido para artefatos finais da Sprint 6)
- `GET /api/internal/mds/reports/{id}` (novo endpoint)
- `GET /api/internal/mds/requests/{id}/artifacts` (novo endpoint)

**Artefatos / schemas impactados:**

- mds.practicalKnowledgePack.v1
- mds.mechanismDiscoveryReport.v1
- mds.mechanismSpec.v1

**Testes executados:**

- `cd mds && mvn test -Dtest=MechanismDiscoveryPipelineServiceTest`
- `cd backend/ads-service && mvn test -Dtest=MdsInternalControllerContractTest`

**Resultado observado:**

Requests com mecanismo recomendado passam a produzir pacote prático e relatório final persistidos no backend, com leitura por endpoint e listagem dos artefatos por request.

**Limitações / pendências:**

- composição textual do `practicalKnowledgePack` permanece heurística e pode evoluir em qualidade semântica na próxima sprint;
- observabilidade avançada, retries sofisticados e hardening operacional seguem pendentes para Sprint 7.

**Próximo passo sugerido:**

Executar Sprint 7 com foco em observabilidade, hardening operacional e ampliação de testes de integração.

## [2026-04-17] — Etapa: sprint 7 - testes, observabilidade e hardening operacional

**Status:** `CONCLUIDO`

**Resumo:**

A Sprint 7 foi implementada no módulo `mds/` com hardening operacional na busca de fontes externas (timeouts + retries), classificação explícita de falhas recuperáveis/não recuperáveis, instrumentação de métricas por etapa e ampliação da suíte de testes de falha/retry.

**Objetivo da etapa:**

Estabilizar a V1 do MDS para operação previsível no ecossistema do Marketing Hub, melhorando diagnósticos, comportamento em falhas transitórias e cobertura de testes alinhada ao plano da sprint.

**O que foi implementado:**

- configuração de busca adicionada em `MdsProperties` (`timeoutMs`, `retryMaxAttempts`, `retryBackoffMs`) com variáveis de ambiente no `application.yml`;
- implementação de retries controlados no `SearchExecutionService`, com erro previsível (`RecoverableSourceException`) quando todas as fontes falham;
- implementação de timeout por fonte em `PubmedSearchClient` e `CrossrefSearchClient` usando `SimpleClientHttpRequestFactory`;
- criação das exceções `RecoverableSourceException` e `NonRecoverablePipelineException` para separar falhas recuperáveis externas de falhas internas não recuperáveis;
- atualização do `MdsLoopRunner` para classificar e propagar `failureStage` coerente (`recoverable_external`, `non_recoverable_pipeline`, `pipeline`);
- instrumentação no `MechanismDiscoveryPipelineService` com métricas por etapa (`success/failure/duration`) e log de resumo operacional com os campos mandatórios da sprint;
- inclusão do endpoint Actuator de métricas (`/actuator/metrics`) na configuração exposta;
- criação de `SearchExecutionServiceTest` cobrindo retry bem-sucedido e falha previsível quando todas as fontes externas falham;
- ajuste de `MechanismDiscoveryPipelineServiceTest` para manter validação do pipeline completo com `MeterRegistry` ativo.

**Arquivos alterados/criados:**

- mds/src/main/java/com/marketinghub/mds/config/MdsProperties.java
- mds/src/main/java/com/marketinghub/mds/search/SearchExecutionService.java
- mds/src/main/java/com/marketinghub/mds/search/PubmedSearchClient.java
- mds/src/main/java/com/marketinghub/mds/search/CrossrefSearchClient.java
- mds/src/main/java/com/marketinghub/mds/search/RecoverableSourceException.java
- mds/src/main/java/com/marketinghub/mds/search/NonRecoverablePipelineException.java
- mds/src/main/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineService.java
- mds/src/main/java/com/marketinghub/mds/service/MdsLoopRunner.java
- mds/src/main/resources/application.yml
- mds/src/test/java/com/marketinghub/mds/search/SearchExecutionServiceTest.java
- mds/src/test/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineServiceTest.java
- docs/novos-modulos/MDS/plano_implementacao_mds_baseado_na_especificacao.md
- docs/novos-modulos/MDS/protocolo_historico_implantacao_mds.md

**Tabelas / persistência afetadas:**

- `nenhuma`

**APIs / endpoints / contratos afetados:**

- `GET /internal/mechanism-discovery/actuator/health` (mantido)
- `GET /actuator/metrics` (passa a estar exposto para observabilidade)
- contrato backend ↔ MDS sem breaking change estrutural; sem alteração de payloads persistidos.

**Artefatos / schemas impactados:**

- `nenhum` (sem mudança de schema de artefatos canônicos nesta sprint)

**Testes executados:**

- `cd mds && mvn test`
- `cd backend/ads-service && mvn -Dtest=MdsInternalControllerContractTest test`

**Resultado observado:**

O pipeline do MDS passou a reagir de forma previsível a falhas transitórias de fonte externa, com tentativas controladas e classificação explícita de erro, ao mesmo tempo em que fornece telemetria mínima por etapa e logs de diagnóstico operacional consistentes.

**Limitações / pendências:**

- dashboards operacionais não foram implementados nesta sprint (apenas emissão de métricas e logs);
- política de retry/backoff ainda é uniforme por fonte/tipo de erro;
- cobertura de integração real com indisponibilidade intermitente de provedores externos ainda depende de ambiente controlado de teste.

**Próximo passo sugerido:**

Abrir a próxima fase fora da Sprint 7 para evolução de dashboards, política adaptativa de retry por tipo de erro e testes end-to-end com cenários reais de indisponibilidade externa.

## [2026-04-27] — Etapa: sprint 0 da UI administrativa MDS

### Resumo objetivo

Foi implementada a base contratual da Sprint 0 da UI MDS, incluindo endpoints administrativos no backend principal, DTOs dedicados para listagem/detalhe/artefatos/relatório e criação das rotas iniciais no frontend para as quatro telas previstas no plano de UI.

### Escopo realizado

- Backend: criação do controlador `/api/mds` com rotas para requests, detalhe, artefatos, relatório, retry e health.
- Backend: definição de DTOs administrativos da UI e serviço de agregação para timeline, classificação de falha e lineage básico.
- Backend: validação inicial de papéis por header `X-User-Role` (`ADMIN`, `MDS_OPERATOR`, `OPS`).
- Frontend: criação das páginas `MdsWorkspacePage`, `MdsRequestDetailPage`, `MdsArtifactsPage` e `MdsReportPage`.
- Frontend: inclusão das rotas e entrada de navegação para o módulo MDS.
- Documentação: atualização do plano de UI (registro da Sprint 0) e do contrato backend↔MDS para refletir endpoints administrativos.

### Endpoints criados/alterados

- `GET /api/mds/requests`
- `GET /api/mds/requests/{id}`
- `GET /api/mds/requests/{id}/artifacts`
- `GET /api/mds/reports/{requestId}`
- `POST /api/mds/requests/{id}/retry`
- `GET /api/mds/health`

### Testes executados

- `cd backend/ads-service && mvn -Dtest=MdsAdminControllerContractTest,MdsInternalControllerContractTest test`
- `cd frontend && npm run build`

### Pendências

- Integrar validação de papéis ao provedor oficial de identidade/SSO da plataforma.
- Evoluir classificação de falha para critério orientado por código/causa estruturada.

## [2026-04-27] — Etapa: sprint 1 da UI administrativa MDS (lista + detalhe)

### Resumo objetivo

Implementada a Sprint 1 da UI MDS com foco em operação mínima: tela de lista de requests com filtros e ações operacionais, tela de detalhe com timeline e classificação de falha, estados de loading/erro/vazio e testes unitários dos hooks/componentes principais.

### Escopo realizado

- Frontend: evolução da `MdsWorkspacePage` com filtros mínimos da sprint (`status`, `período`, `tenant/produto`) e ações de navegação.
- Frontend: evolução da `MdsRequestDetailPage` para diagnóstico operacional e timeline de estágios.
- Frontend: cobertura de testes unitários para hooks de API e páginas principais de Sprint 1.
- Documentação: atualização do registro da Sprint 1 no plano de UI.

### Endpoints utilizados

- `GET /api/mds/requests`
- `GET /api/mds/requests/{id}`
- `POST /api/mds/requests/{id}/retry`

### Testes executados

- `cd frontend && npm run test -- --run src/api/mds/useMdsAdmin.test.tsx src/pages/mds/MdsWorkspacePage.test.tsx src/pages/mds/MdsRequestDetailPage.test.tsx`
- `cd frontend && npm run build`
- `cd backend/ads-service && mvn -Dtest=MdsAdminControllerContractTest test`

### Pendências

- Refinar UX e feedbacks operacionais para as próximas sprints (artefatos/lineage e relatório avançado).
- Integrar observabilidade de frontend (telemetria de interação) na fase de hardening.

## [2026-04-27] — Etapa: sprint 2 da UI administrativa MDS (artefatos + lineage)

### Resumo objetivo

Implementada a Sprint 2 da UI MDS com rastreabilidade operacional: tela de artefatos por request, visualização do envelope canônico e navegação básica de lineage (pais/filhos), com testes de contrato frontend↔backend para artifacts.

### Escopo realizado

- Backend: enriquecimento do endpoint `GET /api/mds/requests/{id}/artifacts` para retornar conteúdo canônico de cada artefato e relações derivadas (`parentArtifactIds`/`childArtifactIds`).
- Backend: ampliação do teste de contrato do controller administrativo para validar estrutura de artifacts + lineage + content.
- Frontend: evolução da `MdsArtifactsPage` com:
  - agrupamento por tipo de artefato;
  - seleção de item para leitura;
  - envelope canônico renderizado em JSON;
  - navegação básica de lineage por botões de pais e filhos.
- Frontend: testes unitários para fluxo da tela de artifacts e contratos do hook `useMdsArtifacts`.
- Documentação: atualização do plano de UI (registro Sprint 2 concluído).

### Endpoints criados/alterados

- `GET /api/mds/requests/{id}/artifacts` (enriquecido)

### Testes executados

- `cd frontend && npm run test -- --run src/api/mds/useMdsAdmin.test.tsx src/pages/mds/MdsArtifactsPage.test.tsx src/pages/mds/MdsWorkspacePage.test.tsx src/pages/mds/MdsRequestDetailPage.test.tsx`
- `cd frontend && npm run build`
- `cd backend/ads-service && mvn -Dtest=MdsAdminControllerContractTest test`

### Pendências

- observar volume de payload de artifacts para requests muito grandes;
- considerar paginação/virtualização de lineage em sprint de hardening se necessário.

## [2026-04-27] — Etapa: sprint 3 da UI administrativa MDS (relatório + ações operacionais)

### Resumo objetivo

Implementada a Sprint 3 da UI MDS para fechar o ciclo de diagnóstico e ação operacional: refinamento da tela de relatório, regras explícitas de elegibilidade para retry e mensagens operacionais de sucesso/erro no fluxo administrativo.

### Escopo realizado

- Backend: evolução dos DTOs de listagem e detalhe com campos `retryEligible` e `retryReason`.
- Backend: ajuste da regra de retry para aceitar somente status `FAILED`, com motivo explícito quando bloqueado.
- Backend: atualização de teste de contrato administrativo para validar os novos campos de elegibilidade.
- Frontend: evolução da `MdsWorkspacePage` com feedback operacional (sucesso/erro) e explicação de bloqueio de retry por request.
- Frontend: evolução da `MdsReportPage` com resumo executivo (mecanismo recomendado, evidências selecionadas, confiança, limitações e justificativa).
- Frontend: criação de teste E2E do fluxo principal `lista → detalhe → artefatos → relatório`.

### Endpoints criados/alterados

- `GET /api/mds/requests` (campos novos de elegibilidade de retry)
- `GET /api/mds/requests/{id}` (campos novos de elegibilidade de retry)
- `POST /api/mds/requests/{id}/retry` (bloqueio explícito para status não elegíveis)

### Testes executados

- `cd frontend && npm run test -- --run src/api/mds/useMdsAdmin.test.tsx src/pages/mds/MdsWorkspacePage.test.tsx src/pages/mds/MdsRequestDetailPage.test.tsx src/pages/mds/MdsArtifactsPage.test.tsx src/pages/mds/MdsReportPage.test.tsx src/pages/mds/MdsMainFlow.e2e.test.tsx`
- `cd frontend && npm run build`
- `cd backend/ads-service && mvn -Dtest=MdsAdminControllerContractTest test`

### Pendências

- avaliar trilha dedicada para replay seguro de requests `COMPLETED` sem conflitar com retry operacional rápido;
- avaliar telemetria de UX dos alerts operacionais na sprint de hardening.

## [2026-04-27] — Etapa: sprint 4 da UI administrativa MDS (hardening + governança)

### Resumo objetivo

Implementada a Sprint 4 com foco em robustez operacional contínua: observabilidade da UI, ajustes de performance (polling/caching), revisão de acessibilidade e fechamento de aderência canônica das telas administrativas MDS.

### Escopo realizado

- Frontend: painel de observabilidade na `MdsWorkspacePage` com saúde do backend MDS (`/api/mds/health`) e contadores por status.
- Frontend: melhoria de performance com `staleTime`, `keepPreviousData`, polling controlável por toggle de auto-refresh e refetch periódico para detalhe/health.
- Frontend: revisão de acessibilidade e consistência visual (labels de seção, `aria-live`, feedbacks operacionais com fechamento manual).
- Frontend: manutenção da aderência canônica na leitura de envelope e relatório executivo/técnico.
- Testes: ampliação da suíte de hooks para health e manutenção do teste E2E do fluxo principal.

### Endpoints criados/alterados

- `GET /api/mds/health` (consumido no dashboard operacional da UI)
- `GET /api/mds/requests` e `GET /api/mds/requests/{id}` (campos de governança operacional de retry)

### Testes executados

- `cd frontend && npm run test -- --run src/api/mds/useMdsAdmin.test.tsx src/pages/mds/MdsWorkspacePage.test.tsx src/pages/mds/MdsRequestDetailPage.test.tsx src/pages/mds/MdsArtifactsPage.test.tsx src/pages/mds/MdsReportPage.test.tsx src/pages/mds/MdsMainFlow.e2e.test.tsx`
- `cd frontend && npm run build`
- `cd backend/ads-service && mvn -Dtest=MdsAdminControllerContractTest test`

### Pendências

- evolução futura para paginação/virtualização adicional quando o volume operacional crescer;
- avaliação de divisão de bundle para reduzir warning de chunks grandes no build de frontend.
