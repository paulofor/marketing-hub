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
