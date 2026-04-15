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
