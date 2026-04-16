# OPRM — Implementation History

## 2026-04-15 — fase 1: resolução ocupacional e intake estruturado

**Status:** concluído

**Resumo:**  
Foi criada a implementação inicial da fase 1 do módulo OPRM com estrutura Spring Boot dedicada, resolução ocupacional para o conjunto MVP e geração do artefato canônico `occupationProfileSnapshot` com envelope padrão e lineage mínimo.

**O que foi implementado:**  
- criação do módulo `oprm` com estrutura Java 21 + Spring Boot
- implementação de catálogo estruturado com suporte às 6 ocupações do MVP
- implementação do `Occupation Resolver` com normalização de aliases e validação de ocupações suportadas
- implementação da geração do artefato `occupationProfileSnapshot` com campos de envelope (`artifact_type`, `artifact_version`, `source_refs`, `input_refs`, `status`, `confidence_score`)
- disponibilização de endpoints da fase 1 para ocupações suportadas e resolução de intake
- criação de testes unitários da resolução e tratamento de ocupação não suportada

**Arquivos principais alterados:**  
- `oprm/pom.xml`
- `oprm/src/main/java/com/marketinghub/oprm/OprmApplication.java`
- `oprm/src/main/java/com/marketinghub/oprm/application/OccupationResolverService.java`
- `oprm/src/main/java/com/marketinghub/oprm/api/Phase1Controller.java`
- `oprm/src/main/java/com/marketinghub/oprm/infra/StructuredOccupationCatalog.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/ArtifactEnvelope.java`
- `oprm/src/test/java/com/marketinghub/oprm/application/OccupationResolverServiceTest.java`
- `oprm/README.md`
- `oprm/Dockerfile`
- `oprm/docker-compose.yml`

**Contratos / artefatos afetados:**  
- `occupationAliasResolution`
- `occupationProfileSnapshot`
- nenhum contrato HTTP externo ao módulo foi versionado nesta etapa

**Testes executados:**  
- `cd oprm && mvn test` — **passou**

**Limitações ou pendências:**  
- intake estruturado ainda está em fonte local em memória, sem integração com backend principal
- não há persistência remota dos artefatos no backend nesta fase
- não há enriquecimento web nesta etapa

**Próximo passo sugerido:**  
- implementar fase 2 com captura web por allowlist e `occupationWebSourceSnapshot`
- definir contrato explícito de troca entre OPRM e backend para jobs e publicação de artefatos

## 2026-04-15 — fase 2: enriquecimento web

**Status:** concluído

**Resumo:**  
Foi implementada a fase 2 do OPRM com pipeline inicial de enriquecimento web por allowlist, captura de páginas públicas por ocupação do MVP e publicação do artefato canônico `occupationWebSourceSnapshot` com lineage e sinais semânticos para apoiar as próximas fases.

**O que foi implementado:**  
- criação do serviço `WebEnrichmentService` para resolver ocupação, aplicar política de fontes e capturar sementes públicas
- implementação de `OccupationSourcePolicyProfile` com allowlist, blocklist e metadados de risco para governar coleta
- implementação de fetch HTTP inicial (`HttpWebPageFetcher`) com extração de título/conteúdo, hash e classificação de tipo de fonte
- criação do endpoint `POST /api/oprm/phase2/enrich` para disparar enriquecimento web por ocupação
- criação do artefato `occupationWebSourceSnapshot` com fontes capturadas, sinais semânticos e resumo de enriquecimento
- adição de testes unitários da fase 2 para geração do artefato e validação de ocupação não suportada

**Arquivos principais alterados:**  
- `oprm/src/main/java/com/marketinghub/oprm/application/WebEnrichmentService.java`
- `oprm/src/main/java/com/marketinghub/oprm/api/Phase2Controller.java`
- `oprm/src/main/java/com/marketinghub/oprm/api/Phase2EnrichRequest.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/OccupationWebSourceSnapshotPayload.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/OccupationSourcePolicyProfile.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/CapturedWebSource.java`
- `oprm/src/main/java/com/marketinghub/oprm/infra/enrichment/HttpWebPageFetcher.java`
- `oprm/src/main/java/com/marketinghub/oprm/infra/enrichment/OccupationSourcePolicyRegistry.java`
- `oprm/src/main/java/com/marketinghub/oprm/infra/enrichment/OccupationWebSeedRegistry.java`
- `oprm/src/test/java/com/marketinghub/oprm/application/WebEnrichmentServiceTest.java`
- `oprm/README.md`

**Contratos / artefatos afetados:**  
- `occupationSourcePolicyProfile`
- `occupationWebSourceSnapshot`
- endpoint interno do módulo: `POST /api/oprm/phase2/enrich`

**Testes executados:**  
- `cd oprm && mvn test` — **passou**

**Limitações ou pendências:**  
- pipeline de enriquecimento ainda não publica artefatos no backend principal do Marketing Hub
- seeds de URL estão em registro local estático e ainda não possuem gestão dinâmica por backend
- crawler atual não implementa persistência de cache transitório nem retry/backoff avançado

**Próximo passo sugerido:**  
- implementar fase 3 com inferência de rotina (`routineTaskPattern`, `routineConstraintSignal`, `routinePainSignal`, `routineWorkaroundSignal`)
- definir contrato explícito de jobs/publicação com backend para persistir snapshots e lineage end-to-end

## 2026-04-15 — fase 3: inferência de rotina

**Status:** concluído

**Resumo:**  
Foi implementada a fase 3 do OPRM com inferência de rotina operacional a partir dos artefatos das fases 1 e 2, incluindo geração de padrões de tarefa, sinais de restrição/dor/workaround e publicação do artefato principal `occupationPersonaRoutineCard`.

**O que foi implementado:**  
- criação do serviço `RoutineInferenceService` para orquestrar as etapas de inferência com base em `occupationProfileSnapshot` e `occupationWebSourceSnapshot`
- implementação dos artefatos de interpretação `routineTaskPattern`, `routineConstraintSignal`, `routinePainSignal` e `routineWorkaroundSignal` como estruturas de domínio explícitas
- implementação do payload `OccupationPersonaRoutineCardPayload` para consolidar o card principal da rotina ocupacional inferida
- criação do endpoint `POST /api/oprm/phase3/infer` para execução da fase 3 por ocupação
- criação de testes unitários da fase 3 para validar geração do artefato `occupationPersonaRoutineCard`

**Arquivos principais alterados:**  
- `oprm/src/main/java/com/marketinghub/oprm/application/RoutineInferenceService.java`
- `oprm/src/main/java/com/marketinghub/oprm/api/Phase3Controller.java`
- `oprm/src/main/java/com/marketinghub/oprm/api/Phase3InferRequest.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/RoutineTaskPattern.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/RoutineConstraintSignal.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/RoutinePainSignal.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/RoutineWorkaroundSignal.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/OccupationPersonaRoutineCardPayload.java`
- `oprm/src/test/java/com/marketinghub/oprm/application/RoutineInferenceServiceTest.java`
- `oprm/README.md`

**Contratos / artefatos afetados:**  
- `routineTaskPattern`
- `routineConstraintSignal`
- `routinePainSignal`
- `routineWorkaroundSignal`
- `occupationPersonaRoutineCard`
- endpoint interno do módulo: `POST /api/oprm/phase3/infer`

**Testes executados:**  
- `cd oprm && mvn test` — **passou**

**Limitações ou pendências:**  
- inferência inicial usa heurísticas determinísticas e ainda não aplica modelo de IA para síntese semântica avançada
- artefatos de interpretação ainda não são publicados separadamente no backend principal; permanecem encapsulados no card de saída
- ainda não há contrato de persistência end-to-end entre OPRM e backend para lineage completo

**Próximo passo sugerido:**  
- implementar fase 4 com `desiredOutcomeSignal`, `mechanismOpportunitySignal` e `dorResultadoOfertaMecanismoProvaInput`
- definir contrato explícito de publicação de artefatos do OPRM no backend principal

## 2026-04-15 — fase 4: integração com o framework

**Status:** concluído

**Resumo:**  
Foi implementada a fase 4 do OPRM para transformar a rotina inferida em insumo direto do framework dor→resultado→oferta→mecanismo→prova, incluindo geração estruturada de `desiredOutcomeSignal`, `mechanismOpportunitySignal` e publicação do artefato `dorResultadoOfertaMecanismoProvaInput`.

**O que foi implementado:**  
- criação do serviço `FrameworkIntegrationService` para orquestrar a integração a partir do `occupationPersonaRoutineCard` da fase 3
- implementação dos artefatos de domínio `DesiredOutcomeSignal`, `MechanismOpportunitySignal` e `DorResultadoOfertaMecanismoProvaInputPayload`
- criação do endpoint `POST /api/oprm/phase4/integrate` para execução da integração do framework por ocupação
- atualização da documentação do módulo com endpoint e exemplo de payload da fase 4
- adição de teste unitário dedicado da fase 4 para validar geração do artefato de integração

**Arquivos principais alterados:**  
- `oprm/src/main/java/com/marketinghub/oprm/application/FrameworkIntegrationService.java`
- `oprm/src/main/java/com/marketinghub/oprm/api/Phase4Controller.java`
- `oprm/src/main/java/com/marketinghub/oprm/api/Phase4IntegrateRequest.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/DesiredOutcomeSignal.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/MechanismOpportunitySignal.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/DorResultadoOfertaMecanismoProvaInputPayload.java`
- `oprm/src/test/java/com/marketinghub/oprm/application/FrameworkIntegrationServiceTest.java`
- `oprm/README.md`

**Contratos / artefatos afetados:**  
- `desiredOutcomeSignal`
- `mechanismOpportunitySignal`
- `dorResultadoOfertaMecanismoProvaInput`
- endpoint interno do módulo: `POST /api/oprm/phase4/integrate`

**Testes executados:**  
- `cd oprm && mvn test` — **passou**

**Limitações ou pendências:**  
- os sinais da fase 4 ainda são derivados por heurísticas determinísticas e não usam calibração com feedback downstream
- publicação dos artefatos continua local ao módulo, sem persistência end-to-end no backend principal
- faltam contratos HTTP versionados entre backend principal e OPRM para ingestão/persistência dos novos artefatos

**Próximo passo sugerido:**  
- implementar fase 5 com feedback loop para recalibrar scores por ocupação
- definir contrato de publicação/persistência com backend para lineage completo dos artefatos da fase 4

## 2026-04-15 — fase 5: feedback loop

**Status:** concluído

**Resumo:**  
Foi implementada a fase 5 do OPRM com feedback loop para recalibração dos sinais e scores a partir da performance de hipóteses, geração de histórico incremental por ocupação e publicação do artefato `occupationFeedbackLoopSnapshot`.

**O que foi implementado:**  
- criação do serviço `FeedbackLoopService` para orquestrar inferência de rotina, integração com framework e recalibração baseada em feedback downstream
- implementação de comparação estruturada entre rotina inferida e performance de hipóteses (`HypothesisRoutineFit`)
- implementação de reponderação de sinais (`RoutinePainSignal` e `MechanismOpportunitySignal`) e de confiança agregada do ciclo
- implementação de histórico incremental em memória por ocupação para registrar recalibrações sucessivas da fase 5
- criação do endpoint `POST /api/oprm/phase5/feedback` com payload para snapshots de performance de hipóteses
- atualização do cânone de artefatos e do README do módulo para incluir o artefato e fluxo da fase 5
- criação de testes unitários da fase 5 para validar geração do artefato e acumulação de histórico por ocupação

**Arquivos principais alterados:**  
- `oprm/src/main/java/com/marketinghub/oprm/application/FeedbackLoopService.java`
- `oprm/src/main/java/com/marketinghub/oprm/api/Phase5Controller.java`
- `oprm/src/main/java/com/marketinghub/oprm/api/Phase5FeedbackRequest.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/OccupationFeedbackLoopPayload.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/OccupationFeedbackHistoryEntry.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/HypothesisPerformanceSnapshot.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/HypothesisRoutineFit.java`
- `oprm/src/test/java/com/marketinghub/oprm/application/FeedbackLoopServiceTest.java`
- `oprm/README.md`
- `docs/oprm_canonico_artefatos.md`

**Contratos / artefatos afetados:**  
- `occupationFeedbackLoopSnapshot`
- endpoint interno do módulo: `POST /api/oprm/phase5/feedback`
- `HypothesisPerformanceSnapshot` (payload de entrada da fase 5)

**Testes executados:**  
- `cd oprm && mvn test` — **passou**

**Limitações ou pendências:**  
- histórico por ocupação está em memória local do processo e ainda não foi persistido no backend principal
- cálculo de aderência entre hipótese e rotina usa heurística textual simples e precisa de evolução para comparação semântica mais robusta
- ainda não existe contrato HTTP versionado com backend principal para ingestão automática de métricas de hipótese

**Próximo passo sugerido:**  
- implementar fase 6 de hardening operacional (persistência remota do feedback loop, observabilidade e integração contínua com backend)
- versionar contrato HTTP entre backend principal e OPRM para ingestão/publicação completa dos artefatos da fase 5

## 2026-04-15 — fase de finalização: containerização e publicação operacional

**Status:** parcial

**Resumo:**  
Foi concluída a preparação de finalização operacional do OPRM para publicação em container no host `177.153.62.107`, incluindo artefatos YML e script de apply dedicado para atualização isolada do módulo sem reiniciar os demais serviços.

**O que foi implementado:**  
- criação do arquivo `oprm/docker-compose.deploy.yml` para execução do módulo por imagem publicada
- adição do serviço `oprm-worker` no `deploy/docker-compose.yml` com variáveis de ambiente e porta dedicadas
- criação do script `deploy/bin/apply-oprm-only.sh` para carregar imagem tar, atualizar tag `latest` e aplicar somente o serviço OPRM
- atualização da documentação de deploy com passo a passo específico do host `177.153.62.107`
- atualização do README do módulo com os arquivos de deploy e fluxo resumido de publicação

**Arquivos principais alterados:**  
- `oprm/docker-compose.deploy.yml`
- `deploy/docker-compose.yml`
- `deploy/bin/apply-oprm-only.sh`
- `deploy/README.md`
- `oprm/README.md`

**Contratos / artefatos afetados:**  
- nenhum contrato de API novo
- artefatos operacionais de deploy do container OPRM (`docker-compose` e script de apply)

**Testes executados:**  
- `cd oprm && mvn test` — **passou**
- `docker compose -f deploy/docker-compose.yml config` — **bloqueado por ambiente** (`docker` indisponível neste runner)

**Limitações ou pendências:**  
- publicação remota no host `177.153.62.107` depende de credenciais de SSH e execução no ambiente de infraestrutura
- integração ativa com backend principal segue como próxima etapa funcional (o módulo ainda não consome endpoint remoto real)

**Próximo passo sugerido:**  
- executar o fluxo de cópia e apply no host `177.153.62.107` com usuário de infraestrutura
- versionar contrato HTTP backend↔OPRM para jobs e publicação de artefatos end-to-end

## 2026-04-16 — ajuste da tela inicial do workspace OPRM

**Status:** concluído

**Resumo:**  
Foi ajustada a tela inicial do módulo OPRM no frontend para reduzir a sensação de incompletude no primeiro acesso, adicionando entrada explícita para disparar processamento e um estado vazio guiado para o fluxo de negócio.

**O que foi implementado:**  
- inclusão de bloco dedicado `Rodar OPRM` com campo para ocupação/persona e ação de submit
- implementação de disparo de job OPRM diretamente pela tela inicial usando a mutação já existente
- inclusão de indicador de carregamento e bloqueio do botão durante a requisição assíncrona
- substituição do alerta simples de vazio por estado guiado com próximos passos (rodar processamento, aguardar status e navegar para rotina/oferta)

**Arquivos principais alterados:**  
- `frontend/src/pages/oprm/OprmWorkspacePage.tsx`
- `docs/history/oprm-implementation-history.md`

**Contratos / artefatos afetados:**  
- nenhum contrato novo

**Testes executados:**  
- `cd frontend && npm run test -- OprmNavigation.test.tsx` — **passou**
- `cd frontend && npm run build` — **passou**

**Limitações ou pendências:**  
- campos adicionais da tabela de ocupações previstos na especificação (nicho, confiança/dor/oportunidade reais e origem) continuam dependentes de expansão do payload do backend
- filtro de confiança permanece como placeholder de sprint futura

**Próximo passo sugerido:**  
- evoluir endpoint `/api/oprm/jobs/workspace/occupations` para retornar metadados completos da ocupação previstos na especificação
- implementar filtro de confiança real no workspace usando dados persistidos do card de rotina e artefatos correlatos

## 2026-04-15 — sprint 1: contrato oficial backend ↔ OPRM

**Status:** concluído

**Resumo:**  
Foi consolidado o contrato oficial de integração da Sprint 1 entre backend principal e OPRM com OpenAPI v1, DTOs comuns de troca e política inicial de versionamento/erros HTTP para remover ambiguidades de comunicação entre os módulos.

**O que foi implementado:**  
- criação do documento OpenAPI v1 para endpoints de claim, detail, status, artifacts, feedback e heartbeat
- criação de DTOs comuns do contrato em `oprm` para job, artifact, status, feedback, heartbeat e erro de API
- criação de enums canônicos de `jobStatus`, `jobType` e `artifactStatus` para alinhamento semântico
- criação da documentação de versionamento do contrato com regras de compatibilidade e política de erro HTTP
- atualização do README do módulo com links oficiais de contrato da Sprint 1

**Arquivos principais alterados:**  
- `docs/novos-modulos/OPRM/contracts/oprm-backend-integration-openapi.v1.yaml`
- `docs/novos-modulos/OPRM/contracts/oprm-contrato-versionamento.md`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmContractVersion.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmJobClaimRequest.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmJobClaimResponse.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmJobDetailResponse.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmArtifactEnvelopeDto.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmArtifactPublishRequest.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmArtifactPublishResponse.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmJobStatusUpdateRequest.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmFeedbackPublishRequest.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmHeartbeatRequest.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmApiErrorResponse.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmJobType.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmJobStatus.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmArtifactStatus.java`
- `oprm/README.md`

**Contratos / artefatos afetados:**  
- contrato HTTP versionado v1: `/api/oprm/jobs/claim`, `/api/oprm/jobs/{jobId}`, `/api/oprm/jobs/{jobId}/status`, `/api/oprm/artifacts`, `/api/oprm/feedback`, `/api/oprm/heartbeat`
- DTOs comuns: `OprmJobClaimRequest`, `OprmJobDetailResponse`, `OprmArtifactPublishRequest`, `OprmJobStatusUpdateRequest`, `OprmFeedbackPublishRequest`, `OprmHeartbeatRequest`
- política de versionamento: `contractVersion` série `1.x`

**Testes executados:**  
- `cd oprm && mvn test` — **passou**

**Limitações ou pendências:**  
- contrato está publicado e documentado, mas endpoints backend ainda não foram implementados
- ainda não existe contract testing automatizado em CI para validar compatibilidade backend ↔ OPRM
- integração runtime com claim real de jobs será tratada na Sprint 2

**Próximo passo sugerido:**  
- implementar Sprint 2 com modelo de job no backend e endpoints reais de claim/detail/status
- criar clients HTTP no OPRM usando os DTOs comuns da Sprint 1

## 2026-04-15 — sprint 2: job orchestration no backend + consumo real no OPRM

**Status:** concluído

**Resumo:**  
Foi implementada a base operacional da Sprint 2 para integrar OPRM e backend com modelo de jobs persistido no backend, endpoints reais de claim/detail/status e loop agendado no worker OPRM consumindo jobs reais e reportando transições de status.

**O que foi implementado:**  
- criação do modelo de orquestração no backend com `oprm_job`, `oprm_job_input` e `oprm_job_event`
- implementação dos endpoints `POST /api/oprm/jobs`, `POST /api/oprm/jobs/claim`, `GET /api/oprm/jobs/{jobId}` e `POST /api/oprm/jobs/{jobId}/status`
- implementação de lock lógico no claim via update condicional de status (`PENDING` → `CLAIMED`) para evitar duplicidade de claim
- implementação no OPRM dos clients HTTP `BackendJobClient` e `BackendStatusClient`
- implementação de loop agendado do worker (`OprmWorkerJobProcessor`) com claim, detail, execução de `OCCUPATION_MAPPING` e atualização de status `RUNNING`/`SUCCEEDED`/`FAILED`
- atualização da documentação do OPRM e do checklist da Sprint 2 no plano de integração

**Arquivos principais alterados:**  
- `backend/ads-service/src/main/resources/db/changelog/changesets/2026-04-15-oprm-job-orchestration.yaml`
- `backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/OprmJob.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/OprmJobInput.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/OprmJobEvent.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/repository/OprmJobRepository.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/service/OprmJobOrchestrationService.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/web/OprmJobController.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/client/BackendJobClient.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/client/BackendStatusClient.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/worker/OprmWorkerJobProcessor.java`
- `oprm/src/main/java/com/marketinghub/oprm/OprmApplication.java`
- `oprm/src/main/resources/application.properties`
- `oprm/README.md`
- `docs/novos-modulos/OPRM/oprm_plano_unico_desenvolvimento_integracao.md`

**Contratos / artefatos afetados:**  
- endpoints de orquestração de job: `POST /api/oprm/jobs/claim`, `GET /api/oprm/jobs/{jobId}`, `POST /api/oprm/jobs/{jobId}/status`
- endpoint auxiliar de criação de job no backend para orquestração operacional: `POST /api/oprm/jobs`
- estados de job canônicos da integração: `PENDING`, `CLAIMED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `RETRY_WAIT`, `CANCELLED`

**Testes executados:**  
- `cd backend/ads-service && mvn -q -DskipTests compile` — **passou**
- `cd oprm && mvn -q test` — **passou**

**Limitações ou pendências:**  
- execução do worker na Sprint 2 atual processa somente o tipo `OCCUPATION_MAPPING`; `FEEDBACK_RECALIBRATION` ainda não foi implementado no loop
- o timeout de lease foi incluído no modelo, mas a rotina automática de requeue por lease expirado ainda não foi entregue
- publicação remota de artefatos no backend permanece para Sprint 3

**Próximo passo sugerido:**  
- executar Sprint 3 com endpoint de publish artifact e persistência dos envelopes canônicos com lineage
- adicionar consulta operacional de jobs/eventos para observabilidade de fila e diagnóstico de retry

## 2026-04-15 — sprint 3: publicação remota de artefatos + persistência end-to-end

**Status:** concluído

**Resumo:**  
Foi concluída a Sprint 3 da integração OPRM com publicação remota de artefatos para o backend principal, persistência de envelope canônico com lineage e vínculo operacional entre job e artefatos gerados no ciclo do worker.

**O que foi implementado:**  
- criação do endpoint backend `POST /api/oprm/artifacts` com persistência de envelope, lineage e controle de idempotência
- criação da consulta de artefatos no backend com filtros por `correlationId`, `occupationSeedRef` e `status`
- criação do modelo persistente `oprm_artifact` com vínculo ao `oprm_job` e índices para consulta operacional
- implementação no worker OPRM de publicação remota de artefatos após processamento do job
- implementação do pipeline de artefatos da Sprint 3 no worker publicando: `occupationProfileSnapshot`, `occupationWebSourceSnapshot`, `occupationPersonaRoutineCard`, `desiredOutcomeSignal`, `mechanismOpportunitySignal` e `dorResultadoOfertaMecanismoProvaInput`
- atualização do checklist da Sprint 3 no plano único de integração

**Arquivos principais alterados:**  
- `backend/ads-service/src/main/resources/db/changelog/changesets/2026-04-15-oprm-artifact-publication.yaml`
- `backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/OprmArtifact.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/OprmArtifactStatus.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/repository/OprmArtifactRepository.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/service/OprmArtifactService.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/web/OprmArtifactController.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/dto/OprmArtifactEnvelopeDto.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/dto/OprmArtifactPublishRequestDto.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/dto/OprmArtifactPublishResponseDto.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/dto/OprmArtifactSummaryDto.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/client/BackendArtifactPublishClient.java`
- `oprm/src/main/java/com/marketinghub/oprm/application/OprmArtifactPipelineService.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/worker/OprmWorkerJobProcessor.java`
- `docs/novos-modulos/OPRM/oprm_plano_unico_desenvolvimento_integracao.md`

**Contratos / artefatos afetados:**  
- endpoint `POST /api/oprm/artifacts` implementado no backend com persistência de envelope canônico
- endpoint `GET /api/oprm/artifacts` implementado para consulta operacional por correlação/ocupação/status
- artefatos publicados no fluxo do worker: `occupationProfileSnapshot`, `occupationWebSourceSnapshot`, `occupationPersonaRoutineCard`, `desiredOutcomeSignal`, `mechanismOpportunitySignal`, `dorResultadoOfertaMecanismoProvaInput`

**Testes executados:**  
- `cd backend/ads-service && mvn -q -DskipTests compile` — **passou**
- `cd oprm && mvn -q test` — **passou**

**Limitações ou pendências:**  
- o endpoint de consulta retorna resumo operacional do artefato; leitura completa do payload persistido ainda não foi exposta
- validação de contrato automatizada em CI (Spring Cloud Contract) permanece como item da Sprint 5
- o loop de job ainda mantém suporte funcional principal para `OCCUPATION_MAPPING`

**Próximo passo sugerido:**  
- executar Sprint 4 para persistir `occupationFeedbackLoopSnapshot` e histórico de recalibração por ocupação no backend
- adicionar ingestão de snapshots downstream no fluxo do backend para retroalimentar o OPRM com estado persistido

## 2026-04-15 — sprint 4: feedback loop persistido + histórico por ocupação

**Status:** concluído

**Resumo:**  
Foi implementada a Sprint 4 com persistência do feedback loop no backend, histórico por ocupação consultável e integração do worker para recalibração usando estado persistido, eliminando dependência exclusiva de histórico em memória local.

**O que foi implementado:**  
- criação dos modelos persistentes no backend `oprm_feedback_snapshot` e `oprm_feedback_history` com changelog incremental para MySQL
- implementação do endpoint `POST /api/oprm/feedback` para persistir snapshot de recalibração vindo do worker OPRM
- implementação do endpoint `GET /api/oprm/feedback/history` para carregar histórico por ocupação/persona e suportar reprocessamento com estado persistido
- criação de `BackendFeedbackClient` no OPRM para publicar feedback e recuperar histórico persistido do backend
- extensão do `OprmWorkerJobProcessor` para suportar `FEEDBACK_RECALIBRATION` com publicação de `occupationFeedbackLoopSnapshot` e persistência de feedback no backend
- refatoração de `FeedbackLoopService` para receber histórico persistido como entrada e não depender de mapa em memória para manter histórico entre execuções
- atualização do checklist da Sprint 4 no plano único de integração

**Arquivos principais alterados:**  
- `backend/ads-service/src/main/resources/db/changelog/changesets/2026-04-15-oprm-feedback-loop.yaml`
- `backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/OprmFeedbackSnapshot.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/OprmFeedbackHistory.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/repository/OprmFeedbackSnapshotRepository.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/repository/OprmFeedbackHistoryRepository.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/dto/OprmFeedbackPublishRequestDto.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/dto/OprmFeedbackHistoryEntryDto.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/service/OprmFeedbackService.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/web/OprmFeedbackController.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/client/BackendFeedbackClient.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/contract/OprmFeedbackHistoryEntryResponse.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/worker/OprmWorkerJobProcessor.java`
- `oprm/src/main/java/com/marketinghub/oprm/application/FeedbackLoopService.java`
- `docs/novos-modulos/OPRM/oprm_plano_unico_desenvolvimento_integracao.md`

**Contratos / artefatos afetados:**  
- endpoint `POST /api/oprm/feedback` implementado no backend para persistência do feedback loop
- endpoint `GET /api/oprm/feedback/history` implementado para recuperar histórico persistido por ocupação/persona
- artefato `occupationFeedbackLoopSnapshot` passa a ser publicado com histórico consolidado e reprocessável

**Testes executados:**  
- `cd backend/ads-service && mvn -q -DskipTests compile` — **passou**
- `cd oprm && mvn -q test` — **passou**

**Limitações ou pendências:**  
- ingestão de `HypothesisPerformanceSnapshot` downstream no worker ainda usa lista vazia por ausência de fonte operacional integrada no payload de job
- cobertura de contract tests e observabilidade completa continua planejada para Sprint 5

**Próximo passo sugerido:**  
- executar Sprint 5 com contract testing (Spring Cloud Contract), health/readiness e métricas operacionais
- integrar fonte downstream de `HypothesisPerformanceSnapshot` no payload dos jobs de `FEEDBACK_RECALIBRATION`


## 2026-04-15 — sprint 5: contract testing, observabilidade e hardening operacional

**Status:** concluído

**Resumo:**  
Foi concluída a Sprint 5 com contract tests de integração backend↔OPRM, instrumentação operacional no worker (health/readiness, métricas e tracing por `correlationId`) e hardening de deploy para uso de tags imutáveis no fluxo de compose/apply.

**O que foi implementado:**  
- criação de contract tests de integração no OPRM com Spring Cloud Contract WireMock para os fluxos de `claim`, `detail`, `status`, `publish artifact` e `heartbeat`
- inclusão de actuator + Prometheus no módulo OPRM e exposição das métricas mínimas (`oprm.jobs.*`, `oprm.artifacts.published`, `oprm.backend.publish.failures`, `oprm.loop.duration`, `oprm.phase.duration`)
- criação de componente de métricas operacionais do worker com counters/timers e snapshot para heartbeat
- extensão do `OprmWorkerJobProcessor` com tracing básico via MDC (`correlationId`), medição de duração de loop/fase e publicação periódica de heartbeat
- implementação do endpoint backend `POST /api/oprm/heartbeat` para receber heartbeat operacional do worker
- atualização do deploy (`docker-compose` + `apply-oprm-only.sh` + documentação) para adoção de tags imutáveis por variável de ambiente e healthcheck de readiness no serviço OPRM
- atualização do checklist da Sprint 5 no plano único de integração

**Arquivos principais alterados:**  
- `oprm/pom.xml`
- `oprm/src/main/resources/application.properties`
- `oprm/src/main/java/com/marketinghub/oprm/integration/worker/OprmWorkerMetrics.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/worker/OprmWorkerJobProcessor.java`
- `oprm/src/main/java/com/marketinghub/oprm/integration/client/BackendHeartbeatClient.java`
- `oprm/src/test/java/com/marketinghub/oprm/integration/contract/OprmBackendContractTest.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/dto/OprmHeartbeatRequestDto.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/web/OprmHeartbeatController.java`
- `deploy/docker-compose.yml`
- `deploy/bin/apply-oprm-only.sh`
- `deploy/README.md`
- `oprm/README.md`
- `docs/novos-modulos/OPRM/oprm_plano_unico_desenvolvimento_integracao.md`

**Contratos / artefatos afetados:**  
- endpoint de heartbeat operacional implementado no backend: `POST /api/oprm/heartbeat`
- contract tests de integração backend↔OPRM para endpoints principais do worker (`/api/oprm/jobs/claim`, `/api/oprm/jobs/{jobId}`, `/api/oprm/jobs/{jobId}/status`, `/api/oprm/artifacts`, `/api/oprm/heartbeat`)
- observabilidade operacional do worker com métricas canônicas da Sprint 5

**Testes executados:**  
- `cd oprm && mvn -q test` — **passou**
- `cd backend/ads-service && mvn -q -DskipTests compile` — **passou**
- `docker compose -f deploy/docker-compose.yml config` — **bloqueado por ambiente** (`docker` indisponível no runner)

**Limitações ou pendências:**  
- tracing atual é básico por MDC/logging e ainda não inclui exportador distribuído (OTel/Zipkin)
- endpoint de heartbeat no backend registra eventos em log; retenção histórica persistida não foi adicionada nesta sprint

**Próximo passo sugerido:**  
- adicionar exportação de tracing distribuído (OpenTelemetry) com propagação de `traceparent` backend↔worker
- evoluir heartbeat para persistência agregada por worker/janela de tempo para dashboards operacionais

## 2026-04-15 — hardening de migração: correção de preConditions Liquibase (MySQL 5.7)

**Status:** concluído

**Resumo:**  
Foi corrigido o changelog incremental da fase 5 do OPRM para resolver falha de parsing no Liquibase durante o `validate` do GitHub Actions, ajustando a estrutura YAML de `preConditions` para o formato aceito no parser usado com MySQL 5.7.

**O que foi implementado:**  
- correção da sintaxe YAML de `preConditions` no changeset de criação de `oprm_feedback_snapshot`
- correção da sintaxe YAML de `preConditions` no changeset de criação de `oprm_feedback_history`
- manutenção das condições canônicas para MySQL (`dbms: mysql` + `not tableExists`) sem alterar a intenção funcional da migração

**Arquivos principais alterados:**  
- `backend/ads-service/src/main/resources/db/changelog/changesets/2026-04-15-oprm-feedback-loop.yaml`
- `docs/history/oprm-implementation-history.md`

**Contratos / artefatos afetados:**  
- nenhum contrato novo
- artefato técnico afetado: changelog Liquibase incremental da fase 5 do OPRM

**Testes executados:**  
- `ruby -e "require 'yaml'; YAML.load_file('backend/ads-service/src/main/resources/db/changelog/changesets/2026-04-15-oprm-feedback-loop.yaml'); puts 'YAML OK'"` — **passou**
- `cd backend/ads-service && mvn -s ../settings.xml -DskipTests liquibase:validate` — **falhou** por indisponibilidade de rede para resolver dependências Maven no ambiente local

**Limitações ou pendências:**  
- validação completa do Liquibase no Maven ficou pendente localmente por indisponibilidade de rede para baixar dependências
- recomendada revalidação no pipeline GitHub Actions após merge

**Próximo passo sugerido:**  
- reexecutar a action de backend para confirmar `liquibase:validate` sem erro de parsing
- manter o padrão de `preConditions` em lista nos próximos changesets YAML para evitar regressão de parser

## 2026-04-15 — hardening de migração: alinhamento preConditions do cluster de jobs do OPRM

**Status:** concluído

**Resumo:**  
Foram alinhados os changeSets YAML do cluster de orquestração de jobs e publicação de artefatos do OPRM para remover a mistura inválida de mapas e listas em `preConditions`, eliminando o erro de parsing visto no `liquibase:validate` do GitHub Actions.

**O que foi implementado:**  
- normalização dos blocos `preConditions` para `oprm_job`, `oprm_job_input` e `oprm_job_event` no arquivo `2026-04-15-oprm-job-orchestration.yaml`
- ajuste do changeSet `2026-04-15-oprm-artifact-publication.yaml` para o mesmo formato aceito pelo parser do Liquibase 4.26.0
- harmonização do `2026-04-15-oprm-feedback-loop.yaml` para compartilhar o padrão e evitar regressões futuras

**Arquivos principais alterados:**  
- `backend/ads-service/src/main/resources/db/changelog/changesets/2026-04-15-oprm-job-orchestration.yaml`
- `backend/ads-service/src/main/resources/db/changelog/changesets/2026-04-15-oprm-artifact-publication.yaml`
- `backend/ads-service/src/main/resources/db/changelog/changesets/2026-04-15-oprm-feedback-loop.yaml`

**Contratos / artefatos afetados:**  
- tabelas: `oprm_job`, `oprm_job_input`, `oprm_job_event`, `oprm_artifact`, `oprm_feedback_snapshot`, `oprm_feedback_history`
- nenhum contrato HTTP novo

**Testes executados:**  
- `cd backend/ads-service && mvn -DskipTests org.liquibase:liquibase-maven-plugin:4.26.0:validate -Dliquibase.url=offline:mysql -Dliquibase.username=dummy -Dliquibase.password=dummy -Dliquibase.changeLogFile=src/main/resources/db/changelog/db.changelog-master.yaml` — **passou** (modo offline, validação sintática do changelog)

**Limitações ou pendências:**  
- validação ocorreu em URL offline; ainda é necessário confirmar a execução conectada ao MySQL 5.7 real no pipeline
- não foi executado `liquibase update` contra banco físico, então as tabelas ainda não existem fora do changelog

**Próximo passo sugerido:**  
- reexecutar o workflow de backend no GitHub Actions para confirmar `liquibase:validate` com o banco real
- planejar o `liquibase updateSQL`/`update` direcionado para staging antes da sincronização com o worker OPRM

## 2026-04-16 — sprint UI-1: menu principal OPRM + workspace de ocupações

**Status:** concluído

**Resumo:**  
Foi entregue a Sprint UI-1 do OPRM no frontend do Marketing Hub com novo item de menu principal, rota dedicada do módulo e tela inicial de Ocupações consumindo dados reais do backend por endpoint de workspace, incluindo filtros básicos e ações de abrir rotina e reprocessar ocupação.

**O que foi implementado:**  
- inclusão do item de menu principal `OPRM` na navegação lateral
- criação das rotas `/oprm` (workspace de ocupações) e `/oprm/routine/:occupationSeedRef` (placeholder da Sprint UI-2)
- implementação da tela `Ocupações` com busca, filtro por status, estado de loading/erro/vazio e tabela com ações de `Ver rotina` e `Reprocessar`
- implementação do reprocessamento com `POST /api/oprm/jobs`, botão com `spinner` e estado desabilitado durante requisição assíncrona
- criação do endpoint backend `GET /api/oprm/jobs/workspace/occupations` para listar ocupações reais do workspace com base nos jobs mais recentes por `occupationSeedRef`
- inclusão de teste de navegação garantindo rota e link do menu para o OPRM

**Arquivos principais alterados:**  
- `frontend/src/components/MainNavigation.tsx`
- `frontend/src/App.tsx`
- `frontend/src/pages/oprm/OprmWorkspacePage.tsx`
- `frontend/src/pages/oprm/OprmRoutinePlaceholderPage.tsx`
- `frontend/src/api/oprm/useOprmWorkspaceOccupations.ts`
- `frontend/src/api/oprm/useCreateOprmJob.ts`
- `frontend/src/__tests__/OprmNavigation.test.tsx`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/web/OprmJobController.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/service/OprmJobOrchestrationService.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/repository/OprmJobRepository.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/dto/OprmWorkspaceOccupationSummaryDto.java`
- `docs/history/oprm-implementation-history.md`

**Contratos / artefatos afetados:**  
- endpoint novo: `GET /api/oprm/jobs/workspace/occupations`
- endpoint reutilizado: `POST /api/oprm/jobs` para ação de reprocessamento no workspace
- nenhum artefato canônico novo

**Testes executados:**  
- `cd frontend && npm run test -- --run src/__tests__/OprmNavigation.test.tsx` — **passou**
- `cd frontend && npm run build` — **passou**
- `cd backend/ads-service && mvn -s ../settings.xml -DskipTests compile` — **passou**

**Limitações ou pendências:**  
- colunas de confiança, quantidade de dores e oportunidades permanecem como placeholder nesta sprint por ausência de endpoint consolidado com esses agregados
- rota de rotina está como placeholder até a Sprint UI-2

**Próximo passo sugerido:**  
- implementar Sprint UI-2 com tela de rotina consumindo `occupationPersonaRoutineCard` e sinais derivados
- evoluir endpoint de workspace para retornar agregados de confiança/dor/oportunidade a partir dos artefatos publicados

## 2026-04-16 — sprint UI-2: rotina da persona + builder de oferta

**Status:** concluído

**Resumo:**  
Foi entregue a Sprint UI-2 do OPRM no Marketing Hub com tela de Rotina e tela de Oferta conectadas a dados reais publicados no backend, incluindo navegação interna do módulo, seleção de dor/resultado/mecanismo e preview estruturado do framework dor → resultado → oferta → mecanismo → prova.

**O que foi implementado:**  
- criação do endpoint backend `GET /api/oprm/workspace/routine/{occupationSeedRef}` para consolidar os artefatos publicados (`occupationPersonaRoutineCard` e `dorResultadoOfertaMecanismoProvaInput`) e expor payloads/sinais para consumo da UI
- implementação da tela `Rotina` com resumo executivo, tarefas, restrições, workarounds e blocos de sinais (dores, resultados e mecanismos)
- implementação da tela `Oferta` com seleção guiada de dor/resultado/mecanismo, campos obrigatórios de prova e oferta e preview estruturado
- adição da rota `/oprm/offer/:occupationSeedRef` e substituição do placeholder da rotina por página funcional
- extração da navegação interna do OPRM para componente reutilizável nas telas do módulo
- atualização da tela de `Ocupações` para incluir ação direta `Ir para oferta`

**Arquivos principais alterados:**  
- `frontend/src/App.tsx`
- `frontend/src/pages/oprm/OprmWorkspacePage.tsx`
- `frontend/src/pages/oprm/OprmModuleNavigation.tsx`
- `frontend/src/pages/oprm/OprmRoutinePage.tsx`
- `frontend/src/pages/oprm/OprmOfferPage.tsx`
- `frontend/src/api/oprm/useOprmRoutineWorkspaceData.ts`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/web/OprmWorkspaceController.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/service/OprmArtifactService.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/repository/OprmArtifactRepository.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/dto/OprmRoutineWorkspaceResponseDto.java`
- `docs/history/oprm-implementation-history.md`

**Contratos / artefatos afetados:**  
- endpoint novo: `GET /api/oprm/workspace/routine/{occupationSeedRef}`
- artefatos consumidos na UI: `occupationPersonaRoutineCard` e `dorResultadoOfertaMecanismoProvaInput`
- nenhum artefato canônico novo

**Testes executados:**  
- `cd frontend && npx vitest run src/__tests__/OprmNavigation.test.tsx` — **passou**
- `cd frontend && npm run build` — **passou**
- `cd backend/ads-service && mvn -s ../settings.xml -DskipTests compile` — **falhou** por indisponibilidade de rede no ambiente para resolver dependências Maven

**Limitações ou pendências:**  
- ações de exportação na tela de oferta redirecionam para os módulos de destino e não persistem payload de exportação dedicado nesta sprint
- não foi possível validar compilação do backend localmente por bloqueio de rede para download de dependências Maven

**Próximo passo sugerido:**  
- implementar endpoints de exportação explícitos do builder de oferta para hipótese/landing/experimento com persistência do payload selecionado
- iniciar Sprint UI-3 com telas de `Evidências` e `Feedback` usando lineage e histórico persistido

## 2026-04-16 — sprint UI-3: evidências e feedback

**Status:** concluído

**Resumo:**  
Foi implementada a Sprint UI-3 do módulo OPRM no Marketing Hub, adicionando as telas de Evidências e Feedback com leitura de dados reais via backend, timeline de artefatos por ocupação e comparativo de recalibração de confiança.

**O que foi implementado:**  
- criação do endpoint backend `GET /api/oprm/workspace/insights/{occupationSeedRef}` para consolidar timeline, lineage, fontes, excerpts e snapshots de feedback
- implementação da tela `OPRM · Evidências` com timeline de geração, lista de fontes e painel de excerpts
- implementação da tela `OPRM · Feedback` com histórico por ocupação e comparativo antes/depois de confiança
- atualização da navegação interna do OPRM para incluir links ativos de `Evidências` e `Feedback` quando uma ocupação está selecionada
- inclusão das novas rotas do frontend para as telas de sprint UI-3

**Arquivos principais alterados:**  
- `backend/ads-service/src/main/java/com/marketinghub/oprm/service/OprmArtifactService.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/web/OprmWorkspaceController.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/dto/OprmInsightsWorkspaceResponseDto.java`
- `frontend/src/api/oprm/useOprmInsightsWorkspaceData.ts`
- `frontend/src/pages/oprm/OprmEvidencePage.tsx`
- `frontend/src/pages/oprm/OprmFeedbackPage.tsx`
- `frontend/src/pages/oprm/OprmModuleNavigation.tsx`
- `frontend/src/App.tsx`

**Contratos / artefatos afetados:**  
- endpoint backend: `GET /api/oprm/workspace/insights/{occupationSeedRef}`
- leitura dos artefatos `occupationPersonaRoutineCard` e `occupationFeedbackLoopSnapshot` na composição de workspace
- nenhum artefato canônico novo

**Testes executados:**  
- `cd frontend && npm run test -- --run src/__tests__/OprmNavigation.test.tsx` — **passou**
- `cd frontend && npm run build` — **passou**
- `cd backend/ads-service && mvn -DskipTests compile` — **passou**

**Limitações ou pendências:**  
- o painel de excerpts depende da presença de `evidenceExcerpts` no payload persistido de rotina
- a comparação de feedback usa os dois snapshots mais recentes da ocupação; não há configuração de janela temporal customizada nesta sprint

**Próximo passo sugerido:**  
- implementar Sprint UI-4 com tela de Operações completa (jobs, heartbeat, métricas e falhas)
- padronizar payload de evidências no pipeline para enriquecer a profundidade de auditoria na UI

## 2026-04-16 — sprint ui-4: operações e hardening da UI

**Status:** concluído

**Resumo:**  
Foi concluída a Sprint UI-4 do módulo OPRM no frontend com a nova tela de Operações, busca por `correlationId`, visão de falhas recentes e refinamento da navegação interna para manter consistência entre as telas do módulo.

**O que foi implementado:**  
- criação da tela `OPRM · Operações` com cards operacionais, tabela de jobs por ocupação e painel de falhas recentes
- adição de busca por `correlationId` com listagem de artefatos correlacionados
- adição da rota `/oprm/operations` e ativação do item `Operações` na navegação interna do OPRM
- inclusão de reexecução de job com estado assíncrono (botão desabilitado + spinner)
- adição de hook de dados para operações consumindo `/api/oprm/artifacts` com filtros de status e correlação

**Arquivos principais alterados:**  
- `frontend/src/pages/oprm/OprmOperationsPage.tsx`
- `frontend/src/api/oprm/useOprmOperationsWorkspaceData.ts`
- `frontend/src/pages/oprm/OprmModuleNavigation.tsx`
- `frontend/src/App.tsx`
- `frontend/src/__tests__/OprmNavigation.test.tsx`

**Contratos / artefatos afetados:**  
- consumo de contrato existente `GET /api/oprm/artifacts` com filtros `status` e `correlationId`
- consumo de contrato existente `GET /api/oprm/jobs/workspace/occupations`
- nenhum contrato novo

**Testes executados:**  
- `cd frontend && npm run test -- OprmNavigation.test.tsx` — **passou**
- `cd frontend && npm run build` — **passou**

**Limitações ou pendências:**  
- heartbeat exibido via proxy do último `lastUpdatedAt` dos jobs; não há endpoint de leitura dedicado de heartbeat no backend atual
- tabela operacional de jobs usa resumo por ocupação, pois não existe endpoint de listagem completa de jobs no workspace

**Próximo passo sugerido:**  
- expor endpoint de observabilidade dedicado para heartbeat e métricas do worker no backend
- adicionar endpoint de listagem operacional de jobs com paginação para detalhamento técnico completo

## 2026-04-16 — ajuste da tela inicial do workspace OPRM

**Status:** concluído

**Resumo:**  
Foi ajustada a tela inicial do módulo OPRM no frontend para reduzir a sensação de incompletude no primeiro acesso, adicionando entrada explícita para disparar processamento e um estado vazio guiado para o fluxo de negócio.

**O que foi implementado:**  
- inclusão de bloco dedicado `Rodar OPRM` com campo para ocupação/persona e ação de submit
- implementação de disparo de job OPRM diretamente pela tela inicial usando a mutação já existente
- inclusão de indicador de carregamento e bloqueio do botão durante a requisição assíncrona
- substituição do alerta simples de vazio por estado guiado com próximos passos (rodar processamento, aguardar status e navegar para rotina/oferta)

**Arquivos principais alterados:**  
- `frontend/src/pages/oprm/OprmWorkspacePage.tsx`
- `docs/history/oprm-implementation-history.md`

**Contratos / artefatos afetados:**  
- nenhum contrato novo

**Testes executados:**  
- `cd frontend && npm run test -- --run OprmNavigation.test.tsx` — **passou**
- `cd frontend && npm run build` — **passou**

**Limitações ou pendências:**  
- campos adicionais da tabela de ocupações previstos na especificação (nicho, confiança/dor/oportunidade reais e origem) continuam dependentes de expansão do payload do backend
- filtro de confiança permanece como placeholder de sprint futura

**Próximo passo sugerido:**  
- evoluir endpoint `/api/oprm/jobs/workspace/occupations` para retornar metadados completos da ocupação previstos na especificação
- implementar filtro de confiança real no workspace usando dados persistidos do card de rotina e artefatos correlatos
