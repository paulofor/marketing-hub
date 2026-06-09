# Diagnóstico — Etapa 2 do redirecionamento OPRM NichoCNAE para MEI/autônomo

Data: 2026-06-09

## Objetivo

Mapear onde o pipeline OPRM NichoCNAE gera, transporta, valida, sintetiza e materializa informações para preparar o redirecionamento ao público-alvo de profissionais MEI/autônomos brasileiros, sem executar alteração funcional nesta etapa.

## Resumo executivo

O pipeline atual já possui separação clara por etapa entre backend, coletor OPRM, banco e frontend. A mudança para MEI/autônomo deve concentrar-se em quatro pontos: contrato persistente/DTOs, prompts e motores do coletor, regras de qualidade/materialização e apresentação da tela `/oprm/pipeline`.

O principal acoplamento semântico atual é que várias estruturas ainda representam `nicho/CNAE` como unidade principal e a etapa final materializa em `market_niche_enrichment_profile`. Para suportar público-alvo MEI/autônomo sem misturar produto/oferta, a próxima etapa deve definir contrato explícito para perfil de público-alvo, mantendo vínculo com ciclo, cartão de rotina, CNAE e candidato original.

## Mapa do backend por etapa

Todos os contratos operacionais do backend ficam no pacote `com.marketinghub.oprm.nichocnae`, com controllers OPRM e endpoints internos para o coletor.

| Etapa | Responsabilidade atual | Arquivos/classes principais |
| --- | --- | --- |
| `routine-research-orchestrator` | Seleciona/reprocessa candidatos e abre novos ciclos. | `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/routineresearchorchestrator/service/BackendRoutineResearchOrchestratorService.java`; DTOs em `service/pending`, `service/recent`, `service/reprocess`, `service/runNext`; controller `web/BackendRoutineResearchOrchestratorController.java`. |
| `routine-research-cycle` | Expõe ciclos e detalhes do ciclo pai de pesquisa. | `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/routineresearchcycle/service/BackendRoutineResearchCycleService.java`; DTOs `RecordRoutineResearchCyclePending`, `RoutineResearchCycleExecutionSummaryResponse`, `RecordBackendRoutineResearchCycleDetalheDto`; controller `web/BackendRoutineResearchCycleController.java`. |
| `niche-research-seed-builder` | Persiste seed e queries planejadas pela IA. | `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/nicheresearchseedbuilder/service/BackendNicheResearchSeedBuilderService.java`; DTOs de conclusão, detalhe, falha e pendência; controller `web/BackendNicheResearchSeedBuilderController.java`. |
| `source-searcher` | Entrega queries pendentes ao coletor e grava fontes candidatas. | `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/sourcesearcher/service/BackendSourceSearcherService.java`; DTOs `SourceCandidateRequest/Response`, detalhe, pendência e falha; controller `web/BackendSourceSearcherController.java`. |
| `source-fetcher` | Entrega fontes candidatas ao coletor e grava snapshots leves. | `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/sourcefetcher/service/BackendSourceFetcherService.java`; DTOs `SourceSnapshotResponse`, detalhe, pendência e falha; controller `web/BackendSourceFetcherController.java`. |
| `signal-extractor` | Grava sinais estruturados extraídos dos snapshots. | `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/signalextractor/service/BackendSignalExtractorService.java`; DTOs `SignalExtractionItemRequest`, `ExtractedSignalResponse`, detalhe, pendência e falha; controller `web/BackendSignalExtractorController.java`. |
| `routine-synthesizer` | Consolida sinais em cartão de rotina. | `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/routinesynthesizer/service/BackendRoutineSynthesizerService.java`; DTOs `RoutineCardResponse`, `SignalForRoutineSynthesis`, detalhe, pendência e falha; controller `web/BackendRoutineSynthesizerController.java`. |
| `routine-quality-gate` | Decide se o cartão pode avançar ou se exige nova pesquisa. | `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/routinequalitygate/service/BackendRoutineQualityGateService.java`; DTOs de decisão, detalhe, pendência e falha; controller `web/BackendRoutineQualityGateController.java`. |
| `enriched-niche-materializer` | Materializa cartão aprovado em `market_niche` e `market_niche_enrichment_profile`. | `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/enrichednichematerializer/service/BackendEnrichedNicheMaterializerService.java`; DTOs de detalhe, diagnóstico, pendência, conclusão e falha; controller `web/BackendEnrichedNicheMaterializerController.java`. |

### Entidades e repositories do backend

- Entidades do pipeline: `OprmRoutineResearchCycle`, `OprmNicheResearchSeed`, `OprmResearchQuery`, `OprmSourceCandidate`, `OprmSourceSnapshot`, `OprmExtractedSignal` e `OprmNicheRoutineCard`.
- Normalização semântica de nome: `RoutineResearchNicheNameNormalizer`.
- Repositories OPRM: `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/oprm/nichocnae/OprmRoutineResearchCycleRepository.java`, `OprmNicheResearchSeedRepository.java`, `OprmResearchQueryRepository.java`, `OprmSourceCandidateRepository.java`, `OprmSourceSnapshotRepository.java`, `OprmExtractedSignalRepository.java` e `OprmNicheRoutineCardRepository.java`.
- Repositories acoplados à materialização final: `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/oprm/cnae/OprmNicheCandidateRepository.java`, `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/niche/MarketNicheRepository.java` e `backend/ads-service/src/main/java/com/marketinghub/repository/jpa/niche/MarketNicheEnrichmentProfileRepository.java`.

## Mapa do coletor OPRM por etapa

O coletor usa o pacote `com.marketinghub.nichocnae` e mantém uma estrutura repetida por etapa: `Scheduler`, `Service`, `Processor`, `BackendClient`, contratos de entrada/saída e, quando necessário, `Engine`, `PromptBuilder`, `Validator` ou provedor externo.

| Etapa | Processors/clients | Prompts, validators e engines |
| --- | --- | --- |
| `routine-research-orchestrator` | `RoutineResearchOrchestratorProcessor`, `RoutineResearchOrchestratorService`, `RoutineResearchOrchestratorBackendClient`, `RoutineResearchOrchestratorInitialScheduler`. | Etapa determinística, sem prompt OpenAI. |
| `routine-research-cycle` | `RoutineResearchCycleProcessor`, `RoutineResearchCycleService`, `RoutineResearchCycleBackendClient`. | Etapa determinística de leitura/detalhe do ciclo, sem prompt OpenAI. |
| `niche-research-seed-builder` | `NicheResearchSeedBuilderProcessor`, `NicheResearchSeedBuilderService`, `NicheResearchSeedBuilderBackendClient`, `NicheResearchSeedBuilderScheduler`. | `NicheResearchSeedBuilderPromptBuilder`, `OpenAiNicheResearchSeedBuilderClient`, `NicheResearchSeedBuilderSchema`, `NicheResearchSeedBuilderValidator`, `NicheResearchSeedBuilderOpenAiProperties`. Este é o primeiro ponto crítico para redirecionar queries a MEI/autônomo. |
| `source-searcher` | `SourceSearcherProcessor`, `SourceSearcherService`, `SourceSearcherBackendClient`, `SourceSearcherScheduler`. | `DuckDuckGoHtmlSourceSearchProvider`, `PublicSourceSearchProvider`, `SourceIntentClassifier`. Deve reforçar Brasil-first, intenção de fonte e risco de páginas comerciais/solução. |
| `source-fetcher` | `SourceFetcherProcessor`, `SourceFetcherService`, `SourceFetcherBackendClient`, `SourceFetcherScheduler`. | `JsoupPublicSourceFetcher`, `PublicSourceFetcher`. Coleta snapshots leves e precisa preservar metadados de fonte recente quando disponíveis. |
| `signal-extractor` | `SignalExtractorProcessor`, `SignalExtractorService`, `SignalExtractorBackendClient`, `SignalExtractorScheduler`. | `SignalExtractorEngine`. Deve ganhar categorias/heurísticas voltadas a rotina real, aquisição de clientes, dores emocionais, sonhos, medos, linguagem e canais usados por MEI/autônomo. |
| `routine-synthesizer` | `RoutineSynthesizerProcessor`, `RoutineSynthesizerService`, `RoutineSynthesizerBackendClient`, `RoutineSynthesizerScheduler`. | `RoutineSynthesizerEngine`. Deve sintetizar perfil de público-alvo, não hipótese de produto/oferta. |
| `routine-quality-gate` | `RoutineQualityGateProcessor`, `RoutineQualityGateService`, `RoutineQualityGateBackendClient`, `RoutineQualityGateScheduler`. | `RoutineQualityGateEngine`. Deve avaliar aderência a MEI/autônomo, evidência comportamental, atualidade e risco de desvio para empresa estruturada ou solução. |
| `enriched-niche-materializer` | `EnrichedNicheMaterializerProcessor`, `EnrichedNicheMaterializerService`, `EnrichedNicheMaterializerBackendClient`, `EnrichedNicheMaterializerScheduler`. | `EnrichedNicheMaterializerEngine`, `EnrichedNicheProfileDraft`. É o ponto de maior acoplamento com o contrato final atual de nicho enriquecido. |

## Mapa do banco via MCP

Consulta realizada pelo MCP Server em `https://mcpserverdigi.shop/mcp`, usando `db_query` sobre `INFORMATION_SCHEMA` do schema `marketinghubdb`.

### Tabelas identificadas

- Tabelas OPRM relacionadas ao pipeline: `oprm_routine_research_cycle`, `oprm_niche_research_seed`, `oprm_research_query`, `oprm_source_candidate`, `oprm_source_snapshot`, `oprm_extracted_signal`, `oprm_niche_routine_card` e `oprm_niche_candidate`.
- Tabelas de materialização final e legado de nicho: `market_niche` e `market_niche_enrichment_profile`.
- Outras tabelas OPRM do domínio que aparecem no schema e podem ser fonte/contexto indireto: `oprm_cnae_opportunity_score`, `oprm_cnae_processing_cycle`, `oprm_cnae_enrichment_artifact`, `oprm_market_size_by_cnae`, `oprm_cnpj_cnae_dim`, `oprm_niche_catalog`, `oprm_niche_snapshot`, `oprm_occupation`, `oprm_artifact`, `oprm_job`, `oprm_job_event`, `oprm_job_input`, `oprm_feedback_history` e `oprm_feedback_snapshot`.

### Campos de maior acoplamento semântico

- `oprm_routine_research_cycle`: mantém `source_niche_id`, `cnae_code`, `cnae_description`, `niche_name`, `original_niche_name`, `neutral_niche_name`, `research_mode`, status, contadores e `solution_language_risk_score`.
- `oprm_niche_research_seed`: mantém `business_type`, `operation_type`, `customer_type`, `commercial_objects`, `initial_assumptions`, `confidence_level` e queries associadas.
- `oprm_research_query`: transporta texto, objetivo, grupo de fonte, prioridade, status e contagem de resultados.
- `oprm_source_candidate`: transporta URL, título, snippet, domínio, grupo, provedor, posição, intenção da fonte, evidência de rotina e riscos comerciais/solução.
- `oprm_source_snapshot`: preserva metadados e trecho curto da fonte para extração.
- `oprm_extracted_signal`: armazena sinais classificados, texto, evidência e score.
- `oprm_niche_routine_card`: consolida rotina, tarefas, dores, resultados desejados, oportunidades de mecanismo, evidências, decisão do gate e scores.
- `market_niche_enrichment_profile`: recebe a materialização final atual; precisa ser avaliada na etapa 3 para decidir se comporta o novo perfil MEI/autônomo sem perda semântica.

## Mapa do frontend

### Rotas e telas

- Tela principal do pipeline: `frontend/src/pages/oprm/OprmPipelinePage.tsx`, rota `/oprm/pipeline` registrada em `frontend/src/App.tsx`.
- Detalhe do seed: `frontend/src/pages/oprm/OprmNicheResearchSeedBuilderDetailPage.tsx`, rota `/oprm/pipeline/niche-research-seed-builder/:researchCycleId`.
- Detalhe do nicho enriquecido: `frontend/src/pages/oprm/OprmEnrichedNicheDetailPage.tsx`, rota `/oprm/enriched-niches/profile/:profileId`.
- Navegação OPRM: `frontend/src/pages/oprm/OprmModuleNavigation.tsx`.

### Hooks/endpoints consumidos pela tela de pipeline e detalhes

- `useOprmRoutineResearchOrchestratorRecent`: `/api/oprm/nichocnae/routine-research-orchestrator/recent-processed` e reprocessamento em `/api/oprm/nichocnae/routine-research-orchestrator/recent-processed/{researchCycleId}/reprocess`.
- `useOprmNicheResearchSeedBuilderDetail`: `/api/oprm/nichocnae/niche-research-seed-builder/stage-executions/{researchCycleId}`.
- `useOprmSourceSearcherDetail`: `/api/oprm/nichocnae/source-searcher/stage-executions/{researchCycleId}`.
- `useOprmSourceFetcherDetail`: `/api/oprm/nichocnae/source-fetcher/stage-executions/{researchCycleId}`.
- `useOprmSignalExtractorDetail`: `/api/oprm/nichocnae/signal-extractor/stage-executions/{researchCycleId}`.
- `useOprmRoutineSynthesizerDetail`: `/api/oprm/nichocnae/routine-synthesizer/stage-executions/{researchCycleId}`.
- `useOprmRoutineQualityGateDetail`: `/api/oprm/nichocnae/routine-quality-gate/stage-executions/{researchCycleId}`.
- `useOprmEnrichedNicheMaterializerDetail`: `/api/oprm/nichocnae/enriched-niche-materializer/stage-executions/{researchCycleId}` e `/api/oprm/nichocnae/enriched-niche-materializer/profiles/{profileId}`.

### Pontos de tela a alterar nas próximas etapas

- Textos da tela `/oprm/pipeline` ainda falam majoritariamente em `nicho`, `CNAE`, `rotina`, `dificuldades` e `nicho enriquecido`; devem passar a explicar que o produto intermediário é o perfil de público-alvo MEI/autônomo.
- Cards de seed, extração, síntese, gate e materialização devem mostrar scores e alertas ligados a aderência MEI/autônomo, atualidade, comportamento real, risco de empresa estruturada e risco de linguagem de solução.
- Detalhes devem permitir auditar fontes e evidências por público-alvo, não apenas por CNAE/nicho.

## Diagnóstico de mudanças necessárias

### Backend

1. Criar ou adaptar contrato persistente para perfil de público-alvo MEI/autônomo, com rastreabilidade para ciclo, cartão, CNAE e candidato.
2. Atualizar DTOs públicos e internos apenas no pacote OPRM/NichoCNAE, sem permitir consumo direto entre módulos.
3. Ajustar services de seed, sinais, síntese, gate e materialização para transportar campos de comportamento, rotina, dores emocionais, sonhos, medos, linguagem, canais e scores de aderência.
4. Preservar bloqueio de produto/oferta/campanha na fase de pesquisa, principalmente no materializador e no download consolidado.
5. Revisar Swagger e testes unitários dos services alterados.

### Coletor OPRM

1. Alterar o prompt/schema/validator do `niche-research-seed-builder` para gerar queries de MEI/autônomo brasileiro.
2. Reforçar `source-searcher` e `source-fetcher` para favorecer fonte brasileira recente e evidência comportamental.
3. Expandir `SignalExtractorEngine` para sinais específicos de MEI/autônomo: aquisição de clientes, modo de trabalho, rotina diária, retrabalho, cobrança, agenda, compra de materiais, entrega, linguagem, sonhos, medos e canais.
4. Expandir `RoutineSynthesizerEngine` para sintetizar perfil de público-alvo sem criar oferta.
5. Expandir `RoutineQualityGateEngine` com scores de aderência a autônomo, evidência comportamental, atualidade, risco de fonte antiga, risco de empresa estruturada e risco de solução.
6. Ajustar `EnrichedNicheMaterializerEngine` para materializar o contrato correto sem contaminar o artefato final com metadados técnicos ou solução.

### Banco

1. Na etapa 3, decidir se `market_niche_enrichment_profile` é suficiente ou se será criada tabela nova, como `oprm_mei_audience_profile`.
2. Se houver tabela nova, usar Liquibase YAML compatível com MySQL 5.7, com `databaseChangeLog`, `preConditions` para MySQL, `splitStatements: true` e `stripComments: true`.
3. Declarar `@Column(name = "...")` em campos JPA com risco de divergência de nomenclatura.
4. Evitar `UPDATE`/`DELETE` com subconsulta lendo a mesma tabela-alvo para não cair no erro MySQL 5.7 1093.

### Frontend

1. Atualizar `/oprm/pipeline` para mostrar que o foco operacional é público-alvo MEI/autônomo, não apenas CNAE/nicho.
2. Ajustar nomes de cards, descrições, saídas e alertas para comportamento real, rotina, dores, sonhos, medos, linguagem e canais.
3. Atualizar telas de detalhe para expor scores e evidências do perfil de público-alvo.
4. Manter comandos essenciais: auditar, reprocessar pelo front-end quando permitido e abrir detalhe/download quando houver perfil materializado.

### Testes

1. Backend: testes dos services de seed, busca, coleta, extração, síntese, gate, materialização e normalizador quando os contratos mudarem.
2. Coletor: testes de prompt/schema/validator, engines, clients, processors, schedulers e arquitetura do pacote `com.marketinghub.nichocnae`.
3. Frontend: testes de `/oprm/pipeline`, detalhe do seed e detalhe do perfil enriquecido.
4. Se a etapa 3 criar entidade/tabela nova, adicionar teste de repository/integração conforme padrão do backend.

## Conclusão

A arquitetura atual permite a mudança de direção sem refatoração estrutural ampla. O risco principal é semântico: continuar materializando `nicho enriquecido` como se fosse perfil de público-alvo pode misturar CNAE, empresa, produto e oferta. A causa-raiz desse risco deve ser tratada na etapa 3 com contrato de dados explícito para MEI/autônomo antes de mudar prompts, engines e tela.
