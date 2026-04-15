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
