# OPRM NichoCNAE v2 — contrato de pending inicial do executor

## Objetivo

Este documento define o que cada etapa do pipeline OPRM NichoCNAE v2 precisa receber no `pending` inicial consumido pelo executor `oprm-coletor-mei`.

A finalidade é impedir perda de contexto entre backend e executor, evitar valores nulos inesperados e garantir que cada etapa tenha dados suficientes para executar, auditar, reportar resultado e avançar o pipeline sem depender de logs técnicos.

## Regra geral do pending de qualquer etapa

Todo endpoint `pending` da v2 deve entregar ao executor um envelope comum e um payload funcional da etapa.

### Envelope comum obrigatório

| Campo | Obrigatório | Origem canônica | Uso no executor |
| --- | --- | --- | --- |
| `stageExecutionId` | Sim | `oprm_nichocnae_v2_stage_execution.id` | Identificar a execução ao registrar `complete` ou `fail`. |
| `jobId` | Sim | `oprm_nichocnae_v2_stage_execution.job_id` | Rastrear o fluxo ponta a ponta. |
| `researchCycleId` | Sim, quando já existir ciclo funcional | Ciclo/execução funcional do OPRM | Montar relatório, preservar aprendizado e reprocessar sem perder histórico. |
| `sourceNicheId` | Sim | `oprm_niche_candidate.id` | Identificar o subnicho/candidato de origem. |
| `cnaeCode` | Sim | `oprm_niche_candidate.cnae_code` | Contextualizar a etapa e o relatório. |
| `cnaeDescription` | Sim | `oprm_niche_candidate.cnae_description` | Evitar geração genérica e manter contexto comercial do CNAE. |
| `attemptNumber` | Sim | `oprm_nichocnae_v2_stage_execution.attempt_number` | Separar tentativa cognitiva de retry técnico. |
| `technicalRetryNumber` | Sim | `oprm_nichocnae_v2_stage_execution.technical_retry_number` | Bloquear loop de retry técnico. |
| `knowledgeVersion` | Sim | `oprm_nichocnae_v2_stage_execution.knowledge_version` | Controlar evolução do aprendizado entre reprocessamentos. |
| `materializationEnabled` | Sim | Configuração operacional persistida no backend | Impedir materialização automática quando a v2 estiver em calibração. |
| `inputPayload` | Sim para etapas após `candidate-generator` | Saída funcional estruturada da etapa anterior + contexto preservado | Entrada funcional da etapa atual. |

### Regra de preservação de contexto

A cada criação de pendência da próxima etapa, o executor deve preservar no `inputPayload`:

1. Identificadores funcionais: `jobId`, `researchCycleId`, `sourceNicheId`, `cnaeCode`, `cnaeDescription`, `attemptNumber`, `technicalRetryNumber`, `knowledgeVersion` e `materializationEnabled`.
2. Saída funcional da etapa anterior.
3. Artefatos necessários para auditoria e relatório do usuário.
4. Evidências, riscos, rejeições e motivos de decisão quando existirem.

O backend deve devolver esses mesmos identificadores no envelope do `pending`, mesmo quando também estiverem no `inputPayload`, para que o executor não dependa de JSON textual para rastreabilidade básica.

## Etapa 1 — `candidate-generator`

### Pending inicial necessário

| Campo | Obrigatório | Observação |
| --- | --- | --- |
| `stageExecutionId` | Sim | Identificador da execução inicial. |
| `jobId` | Sim | Deve nascer no backend ao criar o job v2. |
| `sourceNicheId` | Sim | Candidato OPRM selecionado para a v2. |
| `cnaeCode` | Sim | CNAE do candidato. |
| `cnaeDescription` | Sim | Descrição do CNAE usada para gerar candidatos neutros. |
| `attemptNumber` | Sim | Inicialmente `1`. |
| `technicalRetryNumber` | Sim | Inicialmente `0`. |
| `knowledgeVersion` | Sim | Inicialmente `1`. |
| `materializationEnabled` | Sim | Deve vir do backend. |
| `researchCycleId` | Condicional | Se a v2 estiver vinculada a ciclo funcional, deve nascer já nesta etapa; se ainda não existir ciclo, a ausência deve ser explícita e documentada. |

### Payload funcional esperado

A etapa inicial pode receber `inputPayload` vazio, desde que o envelope entregue `cnaeCode` e `cnaeDescription`. Quando o job nasce do levantamento MEI, este fluxo v2 deve focar MEI/autônomo/dono-operador brasileiro, tratando o CNAE como ponto de partida e não como público final. O executor deve gerar:

- `candidates`: candidatos de subnicho/job operacional centrados em MEI/autônomo/dono-operador, preferencialmente entre 8 e 12 recortes para CNAEs amplos.
- `candidateUrls`: URLs iniciais para validação de segurança.
- `candidateCount`.
- `audienceFocus = MEI_AUTONOMO_DONO_OPERADOR`.
- `nextStageCode = source-safety-filter`.

## Etapa 2 — `source-safety-filter`

### Pending inicial necessário

| Campo | Obrigatório | Observação |
| --- | --- | --- |
| Envelope comum completo | Sim | Inclui `researchCycleId` e `cnaeDescription`, mesmo que a etapa filtre apenas URLs. |
| `inputPayload.candidateUrls` ou `inputPayload.urls` | Sim | Lista textual de URLs candidatas. |
| `inputPayload.candidates` | Sim | Candidatos gerados na etapa anterior para preservar rastreabilidade. |
| `inputPayload.candidateCount` | Sim | Permite relatório e validação de consistência. |
| `inputPayload.nextStageCode` | Opcional | Quando ausente, o executor usa `adaptive-query-planner` como próxima etapa padrão. |

### Saída que deve ser preservada para a próxima etapa

- `allowedUrls`.
- `rejectedSources`.
- `allowedUrlCount`.
- `rejectedUrlCount`.
- `safetyDecision`.
- Candidatos originais e contexto do CNAE.

## Etapa 3 — `adaptive-query-planner`

### Pending inicial necessário

| Campo | Obrigatório | Observação |
| --- | --- | --- |
| Envelope comum completo | Sim | O planejamento precisa ser rastreável ao job, CNAE e versão de conhecimento. |
| `inputPayload.evidenceGaps` ou `inputPayload.gaps` | Sim | Lacunas reais que justificam novas buscas. |
| `inputPayload.audience`, `targetAudience` ou `neutralCandidateName` | Sim | Público/ator usado para compor queries. |
| `inputPayload.jobContext`, `operationalJob` ou `context` | Sim | Contexto operacional do subnicho. |
| `inputPayload.previousQueryHashes` | Recomendado | Evita repetir pesquisa sem ganho informacional. |
| `inputPayload.allowedUrls` | Recomendado | Memória das fontes já aprovadas pela etapa 2. |
| `inputPayload.rejectedSources` | Recomendado | Evita reutilizar fonte rejeitada. |

### Saída que deve ser preservada para a próxima etapa

- `plannedQueries`.
- `plannedQueryCount`.
- `reusedQueryCount`.
- `skippedQueryCount`.
- `earlyStopping`.
- `nextStageCode`.

> Observação: quando `plannedQueries` não estiver vazio, o código atual aponta `nextStageCode = source-searcher`; essa etapa precisa ter contrato v2 próprio antes de o avanço ficar completo.

## Etapa 4 — `candidate-tournament`

### Pending inicial necessário

| Campo | Obrigatório | Observação |
| --- | --- | --- |
| Envelope comum completo | Sim | Necessário para explicar por que um candidato venceu ou foi eliminado. |
| `inputPayload.candidates` ou `inputPayload.candidateEvidence` | Sim | Lista de candidatos com evidências e penalidades. |
| `inputPayload.operator`/`executor`, `job`/`operationalJob` e `operationalContext` por candidato | Sim | Sustenta seleção por clareza operacional antes de validar dor. |
| `inputPayload.directEvidenceCount`, `acceptedClaimCount` ou `evidenceCount` por candidato | Recomendado | Complementa o score do torneio sem ser obrigatório para descoberta inicial. |
| `inputPayload.independentSourceCount` ou `sourceCount` por candidato | Recomendado | Mede força de prova independente quando já existir evidência. |
| `inputPayload.rejectedSourceCount` ou `unsafeSourceCount` por candidato | Recomendado | Penaliza risco de fonte. |
| `inputPayload.contradictionCount` por candidato | Recomendado | Penaliza contradições abertas. |

### Saída que deve ser preservada para a próxima etapa

- `rankedCandidates`.
- `finalists`, com até três recortes operacionais para pesquisa posterior.
- `candidateCount`.
- `finalistCount`.
- `tournamentDecision`.

## Etapa 5 — `source-fetcher-reranker`

### Pending inicial necessário

| Campo | Obrigatório | Observação |
| --- | --- | --- |
| Envelope comum completo | Sim | Necessário para auditar fontes e decisões de coleta. |
| `inputPayload.sources` ou `inputPayload.sourceCandidates` | Sim | Lista de fontes candidatas para priorização/coleta. |
| `inputPayload.fetchedContentHashes` | Recomendado | Evita coletar conteúdo já visto. |
| `inputPayload.supportedGoals` por fonte | Recomendado | Ajuda a ranquear fonte conforme objetivo de evidência. |
| `inputPayload.domain` ou URL por fonte | Sim | Necessário para independência e deduplicação. |
| `inputPayload.evidenceLevel` ou sinais de prova por fonte | Recomendado | Ajuda a priorizar fontes mais úteis. |

### Saída que deve ser preservada para a próxima etapa

- `selectedSources`.
- `fetchedSnapshots`.
- `rejectedSources`.
- `selectedSourceCount`.
- `fetchedSnapshotCount`.
- `rejectedSourceCount`.
- `nextStageCode`.

> Observação: o código atual aponta `nextStageCode = signal-extractor` quando há fontes úteis; essa etapa precisa ter contrato v2 próprio antes de o avanço ficar completo.

## Etapa 6 — `knowledge-accumulator`

### Pending inicial necessário

| Campo | Obrigatório | Observação |
| --- | --- | --- |
| Envelope comum completo | Sim | Especialmente `researchCycleId` e `knowledgeVersion`. |
| `inputPayload.candidates` | Sim | Candidatos que receberão conhecimento acumulado. |
| `inputPayload.validatedClaims`, `claims` ou fontes com claims | Sim | Fatos/evidências aceitos pela etapa anterior. |
| `inputPayload.evidenceGaps` | Recomendado | Mantém lacunas ainda não resolvidas. |
| `inputPayload.artifactLineage` | Recomendado | Permite rastrear de qual artefato veio cada fato. |
| `inputPayload.candidateId` | Recomendado | Identifica candidato principal quando o payload estiver focado em um finalista. |
| `inputPayload.candidateVersion` | Recomendado | Controla evolução do candidato. |
| `inputPayload.budgetConsumed` | Recomendado | Permite relatório de custo quando aplicável. |

### Saída que deve ser preservada para a próxima etapa

- `knowledgeVersion` atualizado.
- `validatedFacts`.
- `validatedFactCount`.
- `acceptedSources`.
- `acceptedSourceCount`.
- `rejectedSourceCount`.
- `evidenceGaps` remanescentes.

## Etapa 7 — `commercial-evidence-gate`

### Pending inicial necessário

| Campo | Obrigatório | Observação |
| --- | --- | --- |
| Envelope comum completo | Sim | Necessário para decisão auditável de avanço/materialização. |
| `inputPayload.claims` ou `inputPayload.validatedClaims` | Sim | Base para calcular nível de evidência. |
| `inputPayload.materializationEnabled` | Sim | Bloqueia materialização automática se a flag estiver desligada. |
| `inputPayload.previousEvidenceLevel` | Recomendado | Permite medir ganho informacional. |
| `inputPayload.acceptedSources` | Recomendado | Ajuda a explicar independência de fontes. |
| `inputPayload.contradictions` ou contadores equivalentes | Recomendado | Impede aprovação quando houver contradição relevante. |

### Saída que deve ser preservada para a próxima etapa

- `gateDecision`.
- `evidenceLevel`.
- `confidence`.
- `informationGain`.
- `automaticMaterializationAllowed`.
- `humanReviewRequired`.
- `nextStageCode`.
- Motivos de reprovação ou lacunas que expliquem reprocessamento.

## Etapa 8 — `reprocess-controller`

### Pending inicial necessário

| Campo | Obrigatório | Observação |
| --- | --- | --- |
| Envelope comum completo | Sim | Necessário para controlar nova tentativa sem perder histórico. |
| `inputPayload.failureType` | Sim quando acionado por falha | Diferencia falha técnica, validação e falha cognitiva. |
| `inputPayload.reasonCode` | Sim | Motivo operacional da decisão. |
| `inputPayload.gateDecision` | Sim quando vier do gate | Explica por que reprocessar, revisar ou encerrar. |
| `inputPayload.informationGain` | Sim | Decide se vale continuar. |
| `inputPayload.maxCognitiveAttempts` | Sim | Limita loops de tentativa cognitiva. |
| `inputPayload.failedStageCode` ou `stageCode` | Recomendado | Indica ponto de retorno. |
| `inputPayload.evidenceGaps` ou `missingEvidence` | Recomendado | Orienta a próxima tentativa. |
| `inputPayload.preservedArtifacts` | Recomendado | Evita perder aprendizado válido. |
| `inputPayload.candidateId` e `candidateVersion` | Recomendado | Mantém rastreabilidade do candidato em evolução. |

### Saída que deve ser preservada para a próxima etapa

- `executionMode`.
- `rewindToStage`.
- `knowledgeVersionTo`.
- `reprocessPlan`.
- `preservedArtifacts`.
- `newEvidenceGaps`.

## Etapa 9 — `enriched-niche-materializer`

### Pending inicial necessário

| Campo | Obrigatório | Observação |
| --- | --- | --- |
| Envelope comum completo | Sim | Necessário para materializar com rastreabilidade. |
| `inputPayload.materializationEnabled` | Sim | Deve ser verdadeiro para materialização automática. |
| `inputPayload.gateDecision` | Sim | Deve autorizar materialização. |
| `inputPayload.validationLevel` ou `evidenceLevel` | Sim | Define nível de validação. |
| `inputPayload.confidence` | Sim | Confiança da decisão. |
| `inputPayload.executor` | Recomendado | Ator/persona operacional do nicho. |
| `inputPayload.jobContext` | Recomendado | Contexto operacional do subnicho. |
| `inputPayload.pain` | Recomendado | Dor principal validada. |
| `inputPayload.desiredResult` | Recomendado | Resultado desejado pelo mercado. |
| `inputPayload.plausibleMechanism` | Recomendado | Mecanismo plausível sem virar oferta prematura. |
| `inputPayload.supportingClaimIds` | Recomendado | Evidências que sustentam a materialização. |
| `inputPayload.sourceDomains` | Recomendado | Domínios/fontes usados como prova. |

### Saída esperada

- `materializationDecision`.
- `validationLevel`.
- `confidence`.
- `materializedNicheId` quando houver materialização.
- `enrichedNiche` com campos funcionais do nicho enriquecido.

## Etapas v2 referenciadas mas ainda sem contrato completo no executor atual

### `source-searcher`

O `adaptive-query-planner` pode apontar `nextStageCode = source-searcher` quando há queries planejadas. Para funcionar como v2 completa, essa etapa precisa receber no pending:

- Envelope comum completo.
- `inputPayload.plannedQueries`.
- `inputPayload.previousQueryHashes`.
- `inputPayload.allowedDomains` ou fontes já aprovadas.
- `inputPayload.rejectedSources`.
- Limites de busca por query e orçamento operacional.

### `signal-extractor`

O `source-fetcher-reranker` pode apontar `nextStageCode = signal-extractor` quando há fontes úteis. Para funcionar como v2 completa, essa etapa precisa receber no pending:

- Envelope comum completo.
- `inputPayload.selectedSources`.
- `inputPayload.fetchedSnapshots`.
- `inputPayload.extractionGoals`.
- `inputPayload.evidenceGaps`.
- Política de contaminação e rejeição de linguagem de solução/oferta.

### `routine-synthesizer`

Existe processor no executor, mas a etapa ainda não está cadastrada na lista canônica de varredura da v2. Para entrar no fluxo, ela precisa receber no pending:

- Envelope comum completo.
- `inputPayload.acceptedClaims` ou `validatedClaims`.
- `inputPayload.executor`.
- `inputPayload.jobContext`.
- Fontes aceitas e rejeitadas.
- Lacunas ainda abertas.

## Checklist antes de criar ou alterar um pending v2

1. O envelope comum está completo?
2. `researchCycleId` está sendo criado ou a ausência dele está explicitamente justificada?
3. `cnaeDescription` está disponível para qualquer etapa que precise de contexto de CNAE?
4. O `inputPayload` contém apenas JSON estruturado, sem JSON serializado dentro de campo textual funcional?
5. A etapa recebe evidências e rejeições necessárias para explicar a decisão ao usuário?
6. A próxima etapa receberá todo o contexto necessário sem consultar logs técnicos?
7. O contrato permite reprocessamento sem perder aprendizado?
8. O backend continua apenas lendo/escrevendo estado, sem executar regra operacional da etapa?

## Regra de relatório ao usuário e custo de IA por job/CNAE

Todo resumo administrativo de jobs da v2 deve expor a decisão funcional final persistida pela última etapa, e não apenas o status técnico `COMPLETED`/`FAILED`. Quando uma etapa encerrar o fluxo sem próxima etapa, o backend deve retornar um rótulo e motivo de negócio suficientes para a tela explicar o encerramento ao usuário, por exemplo `NO_VIABLE_SUBNICHE` como "Encerrado sem subnicho viável" com contagens de candidatos e finalistas.

Quando houver uso de IA em qualquer etapa do job, o custo deve ser contabilizado no resumo do próprio job e também agregado no resumo do CNAE. A fonte canônica imediata para essa contabilização é o `outputPayload` estruturado persistido pela própria etapa que consumiu IA, com campos explícitos de custo ou uso de IA; contratos novos devem preferir campos numéricos em USD (`aiCostUsd`, `totalAiCostUsd` ou equivalente canônico) e evitar JSON dentro de JSON. O backend não deve somar custo carregado em `inputPayload`, porque esse payload pode conter a saída de etapa anterior e causaria dupla contagem.

A tela administrativa deve mostrar esses valores recebidos do backend, sem inferir estado de negócio localmente: decisão final, motivo, se houve IA e custo acumulado por job/CNAE.
