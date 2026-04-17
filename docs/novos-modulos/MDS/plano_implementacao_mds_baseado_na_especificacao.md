# Plano de Implementação do MDS — baseado na especificação do módulo

## Objetivo deste plano

Este plano traduz a especificação canônica do **Mechanism Discovery Service (MDS)** em uma sequência de implementação concreta, incremental e compatível com a arquitetura do **Marketing Hub**.

O plano parte das seguintes premissas já definidas:

- o MDS é um **serviço independente**;
- o MDS tem **projeto, container, imagem, API interna e observabilidade próprios**;
- o MDS entra **antes do PromptResolver, do Worker AI e do pipeline de produto**;
- o MDS é dono da **descoberta e tradução do mecanismo**, não da copy final nem da operação comercial;
- a primeira versão já deve conseguir receber dor + resultado + contexto, buscar evidência, analisar, propor mecanismos candidatos, selecionar um mecanismo recomendado e publicar `mechanismSpec` e `practicalKnowledgePack`.

Ao mesmo tempo, este plano respeita o contrato operacional do repositório:

- **somente o backend principal acessa o banco MySQL**;
- o **MDS não acessa o banco diretamente**;
- persistência, versionamento, leitura e exposição de dados acontecem **via backend**;
- novos contratos compartilhados devem nascer no backend e ser testados.

---

## Resultado esperado da V1

Ao final da V1, o sistema deve conseguir executar o seguinte fluxo ponta a ponta:

1. receber uma requisição de descoberta com dor, resultado e contexto;
2. transformar a requisição em uma ou mais perguntas de mecanismo;
3. executar busca estruturada em fontes permitidas;
4. registrar estratégia de busca e resultados normalizados;
5. deduplicar e triar documentos;
6. extrair evidências relevantes;
7. montar mecanismos candidatos;
8. selecionar um mecanismo recomendado com justificativa;
9. gerar `mechanismSpec`;
10. gerar `practicalKnowledgePack`;
11. publicar todos os artefatos no backend com lineage e versionamento;
12. expor relatório e estado operacional do processamento.

---

## Regras arquiteturais obrigatórias

### 1. Banco somente via backend

- o banco é de responsabilidade do `backend/ads-service`;
- nenhuma tabela do MDS é escrita diretamente pelo módulo `mds/`;
- o MDS envia resultados e artefatos ao backend por APIs internas;
- o backend valida, persiste, versiona e expõe leitura posterior.

### 2. Separação de responsabilidades

**MDS é responsável por:**
- formular perguntas de mecanismo;
- buscar evidência;
- normalizar e deduplicar;
- classificar relevância e confiança;
- extrair componentes ativos;
- construir mecanismos candidatos;
- gerar artefatos estruturados.

**Backend é responsável por:**
- persistência;
- migrations;
- APIs de job/request;
- APIs de publicação e leitura;
- versionamento;
- lineage;
- estado de processamento;
- governança e contratos.

### 3. V1 sem escopo inflado

A V1 não deve tentar resolver:
- score epidemiológico sofisticado;
- UI do módulo;
- dezenas de fontes externas;
- ranking por ML;
- revisão humana assistida;
- embeddings e busca semântica avançada;
- object storage completo para tudo.

---

## Estrutura alvo no repositório

### Novo módulo

```text
mds/
  pom.xml
  Dockerfile
  README.md
  src/main/java/...
  src/main/resources/application.yml
  src/test/java/...
```

### Backend principal

```text
backend/ads-service/
  src/main/java/.../mds/
  src/main/resources/db/changelog/... 
  src/test/java/.../mds/
```

### Documentação

```text
docs/novos-modulos/MDS/
  mechanism_discovery_service_responsabilidades.md
  mds_documento_canonico_artefatos.md
  protocolo_historico_implantacao_mds.md
  plano_implementacao_mds_baseado_na_especificacao.md
  contrato_backend_mds.md
```

---

## Artefatos que a V1 precisa suportar

A V1 deve implementar suporte real para os seguintes artefatos do MDS:

1. `mechanismDiscoveryRequest`
2. `mechanismEvidenceSearch`
3. `sourceDocument`
4. `evidenceItem`
5. `mechanismCandidate`
6. `mechanismSpec`
7. `practicalKnowledgePack`
8. `mechanismDiscoveryReport`

### Ordem de geração esperada

```text
mechanismDiscoveryRequest
  -> mechanismEvidenceSearch
  -> sourceDocument
  -> evidenceItem
  -> mechanismCandidate
  -> mechanismSpec
  -> practicalKnowledgePack
  -> mechanismDiscoveryReport
```

---

## Contratos mínimos que precisam existir

## 1. Contratos no backend

O backend deve expor contratos internos para:

- criar request de discovery;
- listar requests pendentes;
- reservar request para processamento;
- receber publicação de artefatos;
- registrar progresso e falhas;
- consultar relatório final.

## 2. Contratos internos do módulo MDS

O módulo MDS deve ter, no mínimo, serviços internos equivalentes a:

- `question-formulation`
- `search`
- `dedup-normalize`
- `screening`
- `evidence-analysis`
- `mechanism-building`
- `pack-building`
- `publish`

---

## Tabelas mínimas que o backend precisa criar

> Observação: os nomes podem ser ajustados ao padrão do repositório, mas essas responsabilidades precisam existir.

### 1. `mds_request`
Representa a solicitação de discovery.

Campos mínimos:
- `id`
- `status`
- `market`
- `problem`
- `desired_outcome`
- `context_json`
- `delivery_constraint`
- `evidence_preference`
- `created_at`
- `updated_at`
- `started_at`
- `finished_at`
- `failure_reason`
- `correlation_id`

### 2. `artifact_record`
Catálogo de artefatos publicados pelo MDS.

Campos mínimos:
- `id`
- `artifact_type`
- `schema_version`
- `version`
- `status`
- `producer_module`
- `owner_module`
- `request_id`
- `content_json`
- `hash`
- `created_at`

### 3. `artifact_lineage_edge`
Lineage explícito entre artefatos.

Campos mínimos:
- `id`
- `parent_artifact_id`
- `child_artifact_id`
- `relation_type`
- `created_at`

### 4. `source_access_record`
Controle de acesso/licença por documento encontrado.

Campos mínimos:
- `id`
- `source_document_id`
- `access_class`
- `permission_state`
- `license_text`
- `access_url`
- `created_at`

### 5. `mds_processing_event`
Histórico operacional do processamento.

Campos mínimos:
- `id`
- `request_id`
- `stage_name`
- `event_type`
- `message`
- `payload_json`
- `created_at`

---

## Endpoints mínimos que o backend precisa expor

### Requests e orquestração
- `POST /api/internal/mds/requests`
- `GET /api/internal/mds/requests/pending`
- `POST /api/internal/mds/requests/{id}/claim`
- `POST /api/internal/mds/requests/{id}/heartbeat`
- `POST /api/internal/mds/requests/{id}/complete`
- `POST /api/internal/mds/requests/{id}/fail`

### Publicação de artefatos
- `POST /api/internal/mds/artifacts/publish-batch`
- `POST /api/internal/mds/artifacts/{id}/lineage`

### Consulta
- `GET /api/internal/mds/requests/{id}`
- `GET /api/internal/mds/reports/{id}`
- `GET /api/internal/mds/requests/{id}/artifacts`
- `GET /api/internal/mds/health`

---

## Componentes mínimos do módulo `mds/`

## 1. Bootstrap e infraestrutura
- `MdsApplication`
- `MdsProperties`
- `BackendClientConfig`
- `RestClient` ou `WebClient` configurado
- `MdsHealthController`
- `MdsLoopRunner` ou agendador principal

## 2. Orquestração principal
- `MechanismDiscoveryJobRunner`
- `MechanismDiscoveryPipelineService`
- `MechanismDiscoveryStage`
- `MechanismDiscoveryContext`

## 3. Etapas do pipeline
- `MechanismQuestionBuilder`
- `EvidenceSearchService`
- `SourceDocumentNormalizer`
- `SourceDedupService`
- `EvidenceScreeningService`
- `EvidenceConfidenceService`
- `ActiveComponentExtractor`
- `MechanismCandidateBuilder`
- `MechanismSpecBuilder`
- `PracticalKnowledgePackBuilder`
- `DiscoveryReportBuilder`
- `ArtifactPublishService`

## 4. Clientes/integrações externas
- `PubMedClient`
- `EuropePmcClient`
- `CrossrefClient`
- `OpenAlexClient`
- `PmcAccessClient`

> Na V1, basta que 1 a 2 fontes estejam operacionais de ponta a ponta. As outras podem entrar como interface com implementação posterior.

## 5. Modelos internos mínimos
- `MechanismDiscoveryInput`
- `MechanismQuestion`
- `SearchQueryPlan`
- `RawSearchResult`
- `NormalizedSourceDocument`
- `EvidenceAssessment`
- `MechanismCandidateDraft`
- `MechanismRecommendation`
- `PracticalPackDraft`

---

## Plano por sprints

## Sprint 1 — Contrato, persistência e orquestração no backend

### Objetivo
Preparar o backend para ser o centro de persistência e orquestração do MDS.

### O que precisa ser construído

#### Backend
- package/backend namespace do MDS no `ads-service`;
- DTOs de request e response do MDS;
- entidades JPA ou modelo equivalente para:
  - `mds_request`
  - `artifact_record`
  - `artifact_lineage_edge`
  - `source_access_record`
  - `mds_processing_event`
- repositories;
- services de orquestração;
- migrations Liquibase MySQL 5.7;
- endpoints internos de request/claim/complete/fail;
- endpoint de publicação em lote de artefatos;
- validação básica de payload e versionamento inicial.

#### Documentação
- contrato backend ↔ MDS;
- atualização do histórico;
- atualização do cânone de artefatos se necessário.

### Critério de aceite
- backend consegue criar uma request de discovery;
- backend consegue reservá-la para processamento;
- backend consegue receber publicação de um lote mínimo de artefatos;
- lineage básico é persistido;
- migrations sobem no ambiente compatível.

### Pendências esperadas para próxima sprint
- ainda sem worker MDS funcional;
- ainda sem busca real em fontes externas;
- ainda sem pipeline de análise.

### Registro do Codex ao final da sprint
**Status:** `CONCLUIDO`

**O que foi concluído:**
- Namespace backend do MDS criado no `ads-service` com entidades, repositories, services e controller interno para requests, claim, heartbeat, complete, fail e publicação em lote de artefatos.
- Migration Liquibase em YAML (MySQL 5.7) criada com as tabelas mínimas de Sprint 1 (`mds_request`, `artifact_record`, `artifact_lineage_edge`, `source_access_record`, `mds_processing_event`).
- Contrato backend ↔ MDS documentado no arquivo `contrato_backend_mds.md`.

**O que ficou pendente para a próxima sprint:**
- Criar módulo `mds/` independente com loop de polling e integração ativa com os endpoints do backend.
- Implementar processamento real de discovery (formulação de perguntas, busca, deduplicação e análise de evidência).
- Implementar testes de contrato backend ↔ módulo MDS em execução ponta a ponta.

**Riscos / observações:**
- O contrato OpenAPI de stub em `openapi_mds_backend_stub.yaml` descreve rotas do serviço MDS, enquanto Sprint 1 focou rotas internas no backend para orquestração e persistência.
- A tabela `source_access_record` foi criada para aderência ao artefato canônico, mas o preenchimento operacional ficará para as sprints com busca em fontes externas.

**Arquivos alterados/criados:**
- backend/ads-service/src/main/java/com/marketinghub/mds/*
- backend/ads-service/src/main/resources/db/changelog/changesets/2026-04-17-mds-sprint1-base.yaml
- backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml
- docs/novos-modulos/MDS/contrato_backend_mds.md

---

## Sprint 2 — Bootstrap do módulo MDS e loop básico de processamento

### Objetivo
Criar o módulo `mds/` e conectá-lo ao backend para consumir requests reais.

### O que precisa ser construído

#### Módulo `mds/`
- projeto Spring Boot;
- `pom.xml`;
- `Dockerfile`;
- `application.yml`;
- cliente HTTP do backend;
- scheduler/loop de polling;
- fluxo mínimo:
  - buscar request pendente
  - claim
  - heartbeat
  - completar ou falhar
- healthcheck do módulo.

#### Backend
- ajuste fino dos endpoints de claim/heartbeat/complete/fail;
- contract tests iniciais.

### Critério de aceite
- o módulo sobe localmente;
- o módulo busca request pendente no backend;
- o módulo faz claim com sucesso;
- o módulo envia heartbeat;
- o módulo marca request como concluída ou falha controlada.

### Pendências esperadas para próxima sprint
- ainda sem discovery real;
- ainda sem publicação de artefatos completos;
- ainda sem fontes externas operacionais.

### Registro do Codex ao final da sprint
**Status:** `CONCLUIDO`

**Concluído:**
- Módulo `mds/` bootstrapado como serviço Spring Boot independente com `pom.xml`, `Dockerfile`, `README.md`, `application.yml`, health endpoint e cliente HTTP para o backend interno.
- Loop básico de processamento ativo com polling de pendências, `claim`, `heartbeat`, execução de pipeline mínimo e finalização com `complete` ou `fail` controlado.
- Ajuste fino no backend para lifecycle de requests: `heartbeat`, `complete` e `fail` agora exigem status `IN_PROGRESS` para evitar transições inválidas.
- Contract tests iniciais adicionados para endpoints internos do backend (`claim`, `lineage`, `publish-batch`) e testes de loop no módulo MDS para o fluxo claim/heartbeat/complete/fail.

**Pendências para a próxima sprint:**
- Discovery real (formulação de pergunta, busca estruturada, normalização de `sourceDocument`, classificação de acesso e permissões).
- Persistência operacional de `mechanismEvidenceSearch` e `sourceDocument` em fluxo real.
- Hardening de observabilidade, retries avançados e métricas de pipeline científico.

**Riscos / observações:**
- O pipeline do Sprint 2 continua stubado para artefatos (sem busca científica real), intencionalmente limitado ao contrato de orquestração.
- A estabilidade do loop depende da disponibilidade dos endpoints internos do backend para lifecycle e publicação.

**Arquivos alterados:**
- backend/ads-service/src/main/java/com/marketinghub/mds/service/MdsRequestService.java
- backend/ads-service/src/test/java/com/marketinghub/mds/service/MdsRequestServiceTest.java
- backend/ads-service/src/test/java/com/marketinghub/mds/web/MdsInternalControllerContractTest.java
- mds/src/test/java/com/marketinghub/mds/service/MdsLoopRunnerTest.java
- docs/novos-modulos/MDS/plano_implementacao_mds_baseado_na_especificacao.md
- docs/novos-modulos/MDS/protocolo_historico_implantacao_mds.md

---

## Sprint 3 — Formulação de pergunta e busca estruturada

### Objetivo
Fazer o MDS sair de “orquestração vazia” para um pipeline que formula perguntas e executa busca estruturada.

### O que precisa ser construído

#### No módulo `mds/`
- `MechanismQuestionBuilder`;
- `SearchQueryPlanBuilder`;
- clientes reais para ao menos:
  - PubMed/NCBI E-utilities
  - Europe PMC ou Crossref
- serviço de execução da busca;
- persistência de `mechanismEvidenceSearch` via backend;
- normalização inicial de `sourceDocument`;
- classificação inicial de acesso:
  - `open_access`
  - `metadata_only`
  - `restricted`
- registro de `permissionState`:
  - `can_download`
  - `can_text_mine`
  - `link_only`

#### No backend
- suporte a persistência de `mechanismEvidenceSearch`;
- suporte a persistência de `sourceDocument`;
- suporte a persistência de `source_access_record`.

### Critério de aceite
- uma request real gera ao menos uma pergunta de mecanismo;
- o sistema monta queries rastreáveis;
- o MDS consulta pelo menos uma fonte prioritária;
- resultados são normalizados em `sourceDocument`;
- status de acesso/permissão é persistido.

### Pendências esperadas para próxima sprint
- deduplicação ainda pode estar simples;
- triagem ainda pode estar básica;
- análise de evidência ainda não concluída.

### Registro do Codex ao final da sprint
**Status:** `CONCLUIDO`

**O que foi concluído:**
- Implementados `ActiveComponentExtractor` e `MechanismCandidateBuilder` no módulo `mds/`, cobrindo extração de componentes ativos, agrupamento recorrente e separação de componentes essenciais/opcionais.
- O pipeline do MDS passou a gerar e publicar `mechanismCandidate` com justificativa explícita, riscos/limitações e nível de confiança consolidado a partir de `evidenceItem`.
- O pipeline passou a gerar e publicar `mechanismSpec` com seleção de mecanismo recomendado e lineage para o `mechanismCandidate` escolhido e evidências de suporte.
- Backend recebeu endpoint interno para leitura do mecanismo recomendado por request (`GET /api/internal/mds/requests/{id}/recommended-mechanism`), baseado no artefato `mechanismSpec`.
- Testes unitários e de contrato foram atualizados para cobrir publicação de `mechanismCandidate`, publicação de `mechanismSpec` e leitura de mecanismo final no backend.

**O que ficou pendente para a próxima sprint:**
- Geração de `practicalKnowledgePack` para consumo downstream ainda não foi implementada nesta sprint.
- Publicação de `mechanismDiscoveryReport` final ainda não foi implementada nesta sprint.
- Consolidação de relatório operacional fim-a-fim (com pacote prático + spec + métricas finais) permanece para a Sprint 6.

**Riscos / observações:**
- A extração de componentes ativos nesta sprint é heurística e lexical; agrupamento semântico mais robusto pode melhorar precisão em corpora heterogêneos.
- Em requests com baixa densidade textual nos abstracts, a cobertura de componentes pode ficar limitada e reduzir qualidade da justificativa.
- A leitura de mecanismo recomendado no backend depende da publicação prévia de `mechanismSpec`; requests sem artefato retornam `404`.

**Arquivos alterados/criados:**
- mds/src/main/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineService.java
- mds/src/main/java/com/marketinghub/mds/search/ActiveComponentExtractor.java
- mds/src/main/java/com/marketinghub/mds/search/MechanismCandidateBuilder.java
- mds/src/test/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineServiceTest.java
- mds/src/test/java/com/marketinghub/mds/search/ActiveComponentExtractorTest.java
- mds/src/test/java/com/marketinghub/mds/search/MechanismCandidateBuilderTest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/web/MdsInternalController.java
- backend/ads-service/src/main/java/com/marketinghub/mds/service/MdsArtifactService.java
- backend/ads-service/src/main/java/com/marketinghub/mds/repository/MdsArtifactRecordRepository.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsRecommendedMechanismResponse.java
- backend/ads-service/src/test/java/com/marketinghub/mds/web/MdsInternalControllerContractTest.java
- docs/novos-modulos/MDS/plano_implementacao_mds_baseado_na_especificacao.md
- docs/novos-modulos/MDS/protocolo_historico_implantacao_mds.md

---

## Sprint 4 — Normalização, deduplicação, triagem e `evidenceItem`

### Objetivo
Transformar resultados brutos de busca em evidência utilizável para construção do mecanismo.

### O que precisa ser construído

#### No módulo `mds/`
- `SourceDedupService` com regras por DOI/PMID/PMCID/título/URL canônica;
- `EvidenceScreeningService`;
- `EvidenceConfidenceService` com escala:
  - alta
  - moderada
  - baixa
  - muito baixa
- `EvidenceItemBuilder`;
- seleção de evidências prioritárias;
- extração de:
  - limitação
  - proximidade com o problema
  - aplicabilidade ao nicho
  - sinais de força da evidência

#### No backend
- suporte a persistência de `evidenceItem`;
- suporte a eventos de processamento por etapa.

### Critério de aceite
- documentos duplicados deixam de gerar itens redundantes;
- há triagem mínima por relevância e aplicabilidade;
- `evidenceItem` é publicado no backend;
- cada `evidenceItem` referencia sua origem via lineage.

### Pendências esperadas para próxima sprint
- extração de componentes ativos ainda pode estar parcial;
- mecanismo candidato ainda não gerado de forma robusta.

### Registro do Codex ao final da sprint
**Status:** `CONCLUIDO`

**O que foi concluído:**
- Implementado `SourceDedupService` no módulo `mds/` com deduplicação por DOI, PMID, PMCID, título normalizado e URL canônica, priorizando o registro mais completo.
- Implementado `EvidenceScreeningService` para triagem mínima por relevância e aplicabilidade ao nicho, com priorização de evidências para publicação.
- Implementado `EvidenceConfidenceService` com classificação de confiança em quatro níveis (`alta`, `moderada`, `baixa`, `muito_baixa`).
- Implementado `EvidenceItemBuilder` e publicação de artefatos `evidenceItem` no backend com lineage explícito para o `sourceDocument` de origem.
- Pipeline do MDS atualizado para registrar eventos por etapa via heartbeat (`dedup-normalize`, `screening`, `evidence-analysis`) e publicar `mechanismEvidenceSearch` com contadores de deduplicação/triagem.
- Testes do pipeline atualizados e novo teste unitário de deduplicação adicionado no módulo `mds/`.

**O que ficou pendente para a próxima sprint:**
- Extração robusta de componentes ativos para construção de `mechanismCandidate`.
- Agrupamento semântico de componentes recorrentes e regra de seleção final de mecanismo recomendado.

**Riscos / observações:**
- Triagem e confiança ainda são heurísticas iniciais baseadas em metadados e texto (sem avaliação científica profunda por desenho de estudo).
- A deduplicação cobre os identificadores principais, mas pode exigir refinamentos para fontes heterogêneas adicionais nas próximas sprints.

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

---

## Sprint 5 — `mechanismCandidate` e seleção do mecanismo recomendado

### Objetivo
Implementar o coração do MDS: extrair componentes ativos e montar mecanismos candidatos.

### O que precisa ser construído

#### No módulo `mds/`
- `ActiveComponentExtractor`;
- `MechanismCandidateBuilder`;
- agrupamento de componentes recorrentes;
- separação entre:
  - componente essencial
  - componente opcional
  - risco/limitação
- lógica de recomendação final do mecanismo;
- justificativa explícita da seleção;
- geração de `mechanismCandidate`;
- geração de `mechanismSpec`.

#### No backend
- suporte a persistência de `mechanismCandidate`;
- suporte a persistência de `mechanismSpec`;
- leitura do mecanismo final por request.

### Critério de aceite
- uma request com evidência suficiente gera pelo menos um `mechanismCandidate`;
- o sistema escolhe um mecanismo recomendado com justificativa;
- `mechanismSpec` é persistido com lineage até as evidências de origem;
- limitações e nível de confiança são preservados.

### Pendências esperadas para próxima sprint
- versão prática para downstream ainda pode estar parcial;
- relatório final ainda pode estar incompleto.

### Registro do Codex ao final da sprint
**Status:** `CONCLUIDO`

**O que foi concluído:**
- Implementados `ActiveComponentExtractor` e `MechanismCandidateBuilder` no módulo `mds/`, cobrindo extração de componentes ativos, agrupamento recorrente e separação de componentes essenciais/opcionais.
- O pipeline do MDS passou a gerar e publicar `mechanismCandidate` com justificativa explícita, riscos/limitações e nível de confiança consolidado a partir de `evidenceItem`.
- O pipeline passou a gerar e publicar `mechanismSpec` com seleção de mecanismo recomendado e lineage para o `mechanismCandidate` escolhido e evidências de suporte.
- Backend recebeu endpoint interno para leitura do mecanismo recomendado por request (`GET /api/internal/mds/requests/{id}/recommended-mechanism`), baseado no artefato `mechanismSpec`.
- Testes unitários e de contrato foram atualizados para cobrir publicação de `mechanismCandidate`, publicação de `mechanismSpec` e leitura de mecanismo final no backend.

**O que ficou pendente para a próxima sprint:**
- Geração de `practicalKnowledgePack` para consumo downstream ainda não foi implementada nesta sprint.
- Publicação de `mechanismDiscoveryReport` final ainda não foi implementada nesta sprint.
- Consolidação de relatório operacional fim-a-fim (com pacote prático + spec + métricas finais) permanece para a Sprint 6.

**Riscos / observações:**
- A extração de componentes ativos nesta sprint é heurística e lexical; agrupamento semântico mais robusto pode melhorar precisão em corpora heterogêneos.
- Em requests com baixa densidade textual nos abstracts, a cobertura de componentes pode ficar limitada e reduzir qualidade da justificativa.
- A leitura de mecanismo recomendado no backend depende da publicação prévia de `mechanismSpec`; requests sem artefato retornam `404`.

**Arquivos alterados/criados:**
- mds/src/main/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineService.java
- mds/src/main/java/com/marketinghub/mds/search/ActiveComponentExtractor.java
- mds/src/main/java/com/marketinghub/mds/search/MechanismCandidateBuilder.java
- mds/src/test/java/com/marketinghub/mds/service/MechanismDiscoveryPipelineServiceTest.java
- mds/src/test/java/com/marketinghub/mds/search/ActiveComponentExtractorTest.java
- mds/src/test/java/com/marketinghub/mds/search/MechanismCandidateBuilderTest.java
- backend/ads-service/src/main/java/com/marketinghub/mds/web/MdsInternalController.java
- backend/ads-service/src/main/java/com/marketinghub/mds/service/MdsArtifactService.java
- backend/ads-service/src/main/java/com/marketinghub/mds/repository/MdsArtifactRecordRepository.java
- backend/ads-service/src/main/java/com/marketinghub/mds/dto/MdsRecommendedMechanismResponse.java
- backend/ads-service/src/test/java/com/marketinghub/mds/web/MdsInternalControllerContractTest.java
- docs/novos-modulos/MDS/plano_implementacao_mds_baseado_na_especificacao.md
- docs/novos-modulos/MDS/protocolo_historico_implantacao_mds.md

---

## Sprint 6 — `practicalKnowledgePack`, relatório final e publicação completa

### Objetivo
Fechar a V1 com artefatos realmente reutilizáveis pelo restante do Marketing Hub.

### O que precisa ser construído

#### No módulo `mds/`
- `PracticalKnowledgePackBuilder`;
- `DiscoveryReportBuilder`;
- versões de saída:
  - técnica
  - executiva
  - prática para design de produto
  - simplificada para consumidor final
- publicação final em lote dos artefatos restantes;
- montagem do `mechanismDiscoveryReport`;
- marcação final de sucesso/erro por request.

#### No backend
- suporte a persistência de `practicalKnowledgePack`;
- suporte a persistência de `mechanismDiscoveryReport`;
- endpoint `GET /api/internal/mds/reports/{id}`;
- endpoint de listagem de artefatos por request.

### Critério de aceite
- uma request completa gera `mechanismSpec` e `practicalKnowledgePack`;
- existe relatório final consultável;
- artefatos ficam disponíveis para reuso posterior;
- o estado da request reflete com clareza sucesso ou falha.

### Pendências esperadas para próxima sprint
- observabilidade avançada;
- retries mais sofisticados;
- enriquecimento com mais fontes.

### Registro do Codex ao final da sprint
**Status:** `CONCLUIDO`

**O que foi concluído:**
- Implementado `PracticalKnowledgePackBuilder` no módulo `mds/`, com montagem das quatro saídas exigidas nesta sprint: técnica, executiva, prática para design de produto e simplificada para consumidor final.
- Implementado `DiscoveryReportBuilder` no módulo `mds/` para montar e publicar `mechanismDiscoveryReport` com métricas operacionais e referências dos artefatos finais da request.
- Pipeline principal (`MechanismDiscoveryPipelineService`) atualizado para publicar, em sequência, `mechanismSpec`, `practicalKnowledgePack` e `mechanismDiscoveryReport`, além de heartbeats das etapas finais de `pack-building` e `reporting`.
- Backend atualizado com os endpoints de consulta previstos para Sprint 6: `GET /api/internal/mds/reports/{id}` e `GET /api/internal/mds/requests/{id}/artifacts`.
- Testes de contrato do backend e testes unitários do pipeline do `mds/` atualizados para validar publicação completa de Sprint 6.

**O que ficou pendente para a próxima sprint:**
- Observabilidade avançada por estágio (métricas detalhadas e painéis operacionais).
- Estratégia de retries/retomada para falhas transitórias em integrações externas.
- Hardening operacional e cobertura adicional de testes de integração ponta a ponta.

**Riscos / observações:**
- O `practicalKnowledgePack` inicial usa regras determinísticas para compor as quatro versões de saída; refinamento semântico mais profundo fica para hardening posterior.
- O endpoint de relatório retorna a versão mais recente de `mechanismDiscoveryReport` por request; políticas adicionais de auditoria histórica podem ser evoluídas na Sprint 7.
- A etapa de publicação final depende da geração prévia de `mechanismSpec`; requests sem mecanismo recomendado continuam sem relatório final.

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

---

## Sprint 7 — Testes, observabilidade e hardening operacional

### Objetivo
Estabilizar a V1 para que ela seja operável no ecossistema do Marketing Hub.

### O que precisa ser construído

#### Testes
- contract tests backend ↔ MDS;
- testes de integração do pipeline básico;
- testes de falha em fonte externa;
- testes de publicação de artefatos;
- testes de lineage.

#### Observabilidade
- logs mínimos com:
  - `requestId`
  - `market`
  - `problem`
  - `desiredOutcome`
  - `searchSources`
  - `selectedEvidenceCount`
  - `mechanismCandidateCount`
  - `chosenMechanismId`
  - `confidenceLevel`
- métricas por etapa;
- healthcheck;
- eventos operacionais básicos.

#### Hardening
- retries controlados;
- timeouts por fonte;
- classificação clara de falhas recuperáveis vs não recuperáveis;
- documentação final da V1;
- atualização do histórico de implantação.

### Critério de aceite
- contratos principais estão testados;
- pipeline falha de forma previsível;
- logs e métricas permitem diagnóstico básico;
- backlog da V2 fica explícito.

### Pendências esperadas para próxima fase
- UI do MDS;
- mais fontes;
- ranking mais sofisticado;
- revisão humana opcional;
- embeddings e busca semântica;
- object storage/blobs grandes;
- dashboards operacionais.

### Registro do Codex ao final da sprint
**Status:** `NAO_INICIADA`

**O que foi concluído:**
- 

**O que ficou pendente para a próxima sprint ou fase:**
- 

**Riscos / observações:**
- 

**Arquivos alterados/criados:**
- 

---

## Sequência recomendada de implementação pelo Codex

1. implementar primeiro o **backend mínimo**;
2. só depois subir o **módulo `mds/`**;
3. colocar uma **fonte prioritária real** funcionando de ponta a ponta;
4. publicar primeiro `mechanismEvidenceSearch` e `sourceDocument`;
5. depois publicar `evidenceItem`;
6. depois fechar `mechanismCandidate` e `mechanismSpec`;
7. por fim, fechar `practicalKnowledgePack` e relatório;
8. só então endurecer com testes e observabilidade.

---

## Itens explicitamente fora da V1

- UI do MDS no frontend;
- integração com dezenas de bases científicas;
- scoring estatístico avançado;
- revisão humana assistida;
- workflow editorial completo;
- automação ampla de benchmark competitivo;
- integração profunda com object storage;
- embeddings/pgvector/busca semântica sofisticada.

---

## Regra final de uso pelo Codex

Ao final de cada sprint, o Codex deve:

1. atualizar o bloco da sprint correspondente;
2. registrar o que foi realmente entregue;
3. registrar o que ficou pendente para a sprint seguinte;
4. atualizar o `protocolo_historico_implantacao_mds.md`;
5. não marcar como concluído o que ainda estiver apenas desenhado;
6. manter alinhamento estrito com o documento de especificação do módulo e com o contrato operacional do repositório.
