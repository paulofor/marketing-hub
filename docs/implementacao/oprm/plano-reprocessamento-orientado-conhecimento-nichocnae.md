# Plano de reprocessamento orientado por conhecimento do pipeline OPRM NichoCNAE

## 1. Relação com o plano principal

Este documento complementa:

`docs/implementacao/oprm/plano-melhoria-qualidade-pipeline-nichocnae.md`

O plano principal já determina:

- idempotência por estágio;
- retomada no ponto de falha;
- reutilização de resultados aprovados;
- pesquisa adaptativa orientada por gaps;
- separação entre falha técnica e decisão de mercado.

Entretanto, esses pontos ainda não especificam integralmente **como o conhecimento acumulado será versionado, reaproveitado, invalidado e fornecido a uma nova tentativa dentro do mesmo job**.

Este plano torna esse comportamento explícito e obrigatório.

---

## 2. Objetivo

Permitir que um mesmo ciclo de pesquisa:

1. execute uma sequência inicial de estágios;
2. identifique, em um gate posterior, que o resultado ainda é inadequado;
3. retorne somente ao estágio mínimo necessário;
4. reutilize todo conhecimento válido produzido anteriormente;
5. gere uma nova tentativa com gaps, restrições e aprendizados adicionais;
6. preserve a auditoria completa das versões;
7. evite repetir queries, fontes, erros e custos sem necessidade;
8. encerre de forma segura quando não houver ganho informacional suficiente.

O comportamento desejado não é um reinício amplo do pipeline. É um **rewind seletivo orientado por conhecimento validado**.

---

## 3. Princípio central

O pipeline deve deixar de ser apenas linear:

```text
A → B → C → D → E → fim
```

E passar a operar como um grafo controlado:

```text
A → B → C → D → E → F
        ↑         ↓
        └── reprocessamento orientado por gaps
```

A regra canônica é:

> Voltar à menor etapa necessária, levando somente conhecimento ainda válido, preservando artefatos reutilizáveis e transformando o diagnóstico anterior em novas restrições de execução.

---

## 4. Distinções obrigatórias

## 4.1 Retry técnico

O estágio não concluiu por causa de infraestrutura:

- timeout;
- SSL handshake;
- broken pipe;
- HTTP 429, 502, 503 ou 504;
- indisponibilidade transitória do provedor;
- falha de conexão com banco ou serviço interno.

Características:

- mesma entrada lógica;
- mesmo estágio;
- mesma intenção;
- nenhuma nova conclusão de mercado;
- nenhuma alteração do snapshot de conhecimento;
- apenas incremento de tentativa técnica.

Exemplo:

```json
{
  "executionMode": "TECHNICAL_RETRY",
  "stage": "AUDIENCE_PROFILE_SYNTHESIZER",
  "reasonCode": "OPENAI_BROKEN_PIPE",
  "reuseInputArtifactVersion": 4,
  "knowledgeVersion": 3
}
```

## 4.2 Reprocessamento cognitivo

O estágio concluiu tecnicamente, mas o resultado não atingiu o gate de qualidade.

Exemplos:

- faltam tarefas concretas do executor;
- fontes pertencem a ocupação adjacente;
- não há impacto econômico demonstrado;
- ator e contexto não coincidem;
- síntese ficou genérica;
- a pesquisa foi contaminada por solução;
- há evidências contraditórias;
- o candidato escolhido perdeu para outro candidato após pesquisa.

Características:

- novo plano de execução;
- novo conjunto de gaps;
- restrições adicionais;
- possível retorno a estágio anterior;
- incremento de versão de conhecimento;
- preservação seletiva de artefatos válidos.

Exemplo:

```json
{
  "executionMode": "COGNITIVE_REPROCESS",
  "reasonCode": "MISSING_EXECUTOR_ROUTINE_EVIDENCE",
  "rewindToStage": "ADAPTIVE_QUERY_PLANNER",
  "knowledgeVersionFrom": 3,
  "knowledgeVersionTo": 4
}
```

Retry técnico e reprocessamento cognitivo não podem compartilhar a mesma classificação ou regra de contagem.

---

## 5. Unidade de identidade

## 5.1 Mesmo job

O `researchCycleId` deve continuar o mesmo quando:

- o CNAE permanece o mesmo;
- o problema de pesquisa permanece o mesmo;
- o candidato ainda é potencialmente válido;
- faltam apenas evidências;
- uma etapa precisa ser refeita;
- houve falha técnica;
- uma síntese ou gate precisa ser recalculado.

## 5.2 Nova versão de candidato no mesmo ciclo

Quando o candidato principal for abandonado, o ciclo pode continuar, mas deve criar uma nova versão ou ramificação:

```text
researchCycleId: 80
├── candidateVersion: 1 — REJECTED
├── candidateVersion: 2 — FINALIST
└── candidateVersion: 3 — MATERIALIZED
```

Não é permitido sobrescrever o candidato anterior e fingir que sempre foi o mesmo recorte.

## 5.3 Novo job

Um novo `researchCycleId` deve ser criado apenas quando houver mudança material de escopo, como:

- CNAE diferente;
- objetivo de pesquisa diferente;
- execução solicitada como experimento independente;
- mudança incompatível de versão canônica;
- necessidade explícita de comparar dois ciclos isolados.

---

## 6. Novos componentes arquiteturais

## 6.1 Knowledge Accumulator

Responsável por produzir uma visão consolidada e versionada do conhecimento disponível no ciclo.

Deve reunir:

- candidatos e suas versões;
- hipóteses ainda não provadas;
- fontes aprovadas;
- fontes rejeitadas e seus motivos;
- claims aceitos;
- claims rejeitados;
- contradições;
- queries já executadas;
- hashes de queries;
- URLs e hashes de conteúdo;
- gaps de evidência;
- scores calculados;
- decisões de gates;
- orçamento já consumido;
- assinaturas de falha anteriores.

O acumulador não cria fatos novos. Ele consolida o que foi produzido pelos estágios e classifica o estado epistêmico de cada artefato.

## 6.2 Reprocess Controller

Responsável por transformar o diagnóstico do gate em um plano de reprocessamento.

Deve decidir:

1. se a falha é técnica ou cognitiva;
2. qual é o menor estágio de retorno;
3. quais artefatos serão preservados;
4. quais artefatos serão invalidados;
5. quais novos gaps serão pesquisados;
6. quais restrições serão adicionadas;
7. qual orçamento adicional será permitido;
8. quando interromper tentativas;
9. quando escalar para revisão humana;
10. quando encerrar como falta de evidência pública.

## 6.3 Artifact Dependency Graph

Representa as dependências entre artefatos.

Exemplo:

```text
Candidate
  └── QueryPlan
       └── SearchResults
            └── SourceEvaluations
                 └── SourceSnapshots
                      └── EvidenceClaims
                           └── CorroboratedClaims
                                └── Synthesis
                                     └── QualityGate
                                          └── MaterializedNiche
```

Ao invalidar um artefato, o controlador deve invalidar apenas os descendentes afetados.

Exemplo:

- rejeição de uma fonte invalida os claims derivados dela;
- invalidação de claims pode invalidar a síntese;
- invalidação da síntese invalida o gate;
- fontes e queries não relacionadas permanecem válidas.

---

## 7. Snapshot de conhecimento

Cada tentativa cognitiva deve receber um snapshot estruturado.

```json
{
  "researchCycleId": 72,
  "candidateId": 31,
  "candidateVersion": 2,
  "knowledgeVersion": 4,
  "validatedFacts": [
    {
      "canonicalClaimId": 101,
      "claim": "O serviço inclui coleta e entrega física de documentos",
      "supportingClaimIds": [201, 202],
      "confidence": 0.91
    }
  ],
  "tentativeHypotheses": [
    {
      "hypothesisCode": "WHATSAPP_RECURRING_CLIENTS",
      "status": "TENTATIVE",
      "reason": "Há páginas comerciais, mas falta perspectiva direta do executor"
    }
  ],
  "rejectedKnowledge": [
    {
      "artifactType": "SOURCE",
      "artifactId": 66,
      "reasonCode": "DELIVERY_APP_CONTEXT_MISMATCH"
    }
  ],
  "contradictions": [],
  "evidenceGaps": [
    "EXECUTOR_ROUTINE",
    "FAILED_DELIVERY_REWORK",
    "ECONOMIC_IMPACT"
  ],
  "executedQueryHashes": [
    "sha256:abc123",
    "sha256:def456"
  ],
  "fetchedContentHashes": [
    "sha256:source-content-1"
  ],
  "qualityProblems": [
    "ROUTINE_TOO_GENERIC",
    "SOURCE_MIX_DOMINATED_BY_DELIVERY_APPS"
  ],
  "budget": {
    "queriesExecuted": 17,
    "sourcesFetched": 8,
    "aiCost": 0.11,
    "remainingQueryBudget": 12,
    "remainingAiCost": 0.19
  }
}
```

---

## 8. Estados epistêmicos dos artefatos

Todo conhecimento deve possuir um estado explícito:

```text
TENTATIVE
VALIDATED
REJECTED
CONTRADICTED
SUPERSEDED
STALE
```

## 8.1 TENTATIVE

Hipótese plausível ainda sem evidência mínima suficiente.

Pode orientar pesquisa, mas não pode aparecer como fato materializado.

## 8.2 VALIDATED

Claim sustentado por evidência aprovada e pelos hard gates de ator, contexto e entailment.

Pode ser reutilizado em novas tentativas.

## 8.3 REJECTED

Artefato considerado inadequado.

Deve ser reaproveitado apenas como conhecimento negativo, para impedir repetição.

## 8.4 CONTRADICTED

Há fontes válidas em conflito.

Não pode ser promovido automaticamente sem nova pesquisa ou revisão.

## 8.5 SUPERSEDED

Era válido para uma versão anterior, mas foi substituído por artefato mais novo ou candidato diferente.

Mantido somente para auditoria.

## 8.6 STALE

Pode ter perdido validade por tempo, mudança de contexto ou versão canônica.

Requer revalidação antes de ser reutilizado.

---

## 9. O que pode ser reaproveitado

- nome neutro ainda válido;
- candidato e versão ainda ativos;
- queries já executadas;
- hashes de queries;
- resultados de busca relevantes;
- URLs canônicas;
- snapshots de fontes;
- hashes de conteúdo;
- fontes aprovadas;
- motivos de rejeição de fontes;
- claims aprovados com trecho exato;
- claims rejeitados como conhecimento negativo;
- clusters de corroboração;
- contradições encontradas;
- gaps de evidência;
- métricas e custo já consumido;
- classificações de safety;
- decisões e diagnósticos de gates.

---

## 10. O que não pode ser reaproveitado como fato

- síntese de IA sem claim aprovado;
- dor apenas embutida no nome do candidato;
- score comercial especulativo do seed;
- fonte rejeitada;
- claim sem trecho exato;
- conteúdo de ocupação adjacente;
- inferência com ator invertido;
- resultado derivado de fonte safety hard reject;
- conclusão de candidato que foi substituído;
- artefato incompatível com a versão atual do contrato;
- aprendizado textual não estruturado cuja origem não possa ser rastreada.

Esses itens podem ser mantidos para auditoria ou como restrição negativa, mas não devem compor `validatedFacts`.

---

## 11. Matriz de rewind seletivo

| Diagnóstico | Retornar para | Preservar | Invalidar |
|---|---|---|---|
| Faltam evidências de uma dor | `ADAPTIVE_QUERY_PLANNER` | candidato, queries anteriores, fontes e claims válidos | síntese e gate posteriores |
| Muitas queries sem resultado | `QUERY_PLANNER` | candidato, gaps e evidências existentes | queries ineficazes não executadas novamente |
| Ocupação adjacente dominou resultados | `SOURCE_JUDGE` ou `QUERY_PLANNER` | fontes válidas de outros objetivos | avaliações e claims contaminados |
| Fontes boas não foram coletadas | `SOURCE_RERANKER` / `SOURCE_FETCHER` | resultados de busca | seleção e snapshots ausentes |
| Claims não são sustentados | `CLAIM_EXTRACTOR` | snapshots das fontes | claims, corroboração, síntese e gate |
| Ator ou causalidade estão invertidos | `CLAIM_ENTAILMENT_VALIDATOR` | fontes e trechos | claims reprovados e descendentes |
| Síntese ficou genérica | `EVIDENCE_GROUNDED_SYNTHESIZER` | claims aprovados | somente síntese e gate |
| Candidato perdeu aderência | `CANDIDATE_TOURNAMENT` | conhecimento dos demais candidatos | artefatos descendentes do candidato rejeitado |
| Nome está contaminado por hipótese | `NAME_REPAIR` | candidato estrutural e evidências | nome enriquecido anterior |
| Falha SSL, timeout ou broken pipe | mesmo estágio | todos os artefatos concluídos | somente tentativa técnica incompleta |
| Gate já aprovou materialização | `MATERIALIZER` | tudo que foi aprovado | nenhuma pesquisa deve ser repetida |

---

## 12. Contrato de decisão de reprocessamento

```json
{
  "researchCycleId": 72,
  "candidateId": 31,
  "decision": "REPROCESS",
  "executionMode": "COGNITIVE_REPROCESS",
  "reasonCode": "MISSING_EXECUTOR_ROUTINE_EVIDENCE",
  "rewindToStage": "ADAPTIVE_QUERY_PLANNER",
  "retainArtifacts": [
    "CANDIDATE_V2",
    "ACCEPTED_SOURCE_EVALUATIONS",
    "ACCEPTED_SOURCE_SNAPSHOTS",
    "ACCEPTED_CLAIMS"
  ],
  "invalidateArtifacts": [
    "ROUTINE_SYNTHESIS_V1",
    "QUALITY_GATE_V1"
  ],
  "newEvidenceGaps": [
    "EXECUTOR_ROUTINE",
    "FAILED_DELIVERY_REWORK"
  ],
  "newConstraints": [
    "PRIORITIZE_FIRST_PERSON_EXECUTOR_SOURCES",
    "EXCLUDE_DELIVERY_APP_CONTEXT",
    "SEARCH_PROTOCOL_SIGNATURE_WAITING_AND_RETURN"
  ],
  "knowledgeVersionFrom": 3,
  "knowledgeVersionTo": 4,
  "additionalBudget": {
    "maxQueries": 12,
    "maxFetches": 6,
    "maxAiCost": 0.19
  }
}
```

---

## 13. Versionamento das execuções

Cada execução de estágio deve ser imutável.

```json
{
  "stageExecutionId": 884,
  "researchCycleId": 72,
  "candidateId": 31,
  "candidateVersion": 2,
  "stageCode": "ADAPTIVE_QUERY_PLANNER",
  "attemptNumber": 2,
  "technicalRetryNumber": 0,
  "parentStageExecutionId": 830,
  "triggeringGateExecutionId": 878,
  "knowledgeVersion": 4,
  "executionMode": "COGNITIVE_REPROCESS",
  "reprocessReasonCode": "MISSING_EXECUTOR_ROUTINE_EVIDENCE",
  "status": "COMPLETED"
}
```

Regras:

- nunca sobrescrever saída anterior;
- nova tentativa cria novo `stageExecutionId`;
- `attemptNumber` representa tentativa cognitiva;
- `technicalRetryNumber` representa retry da mesma tentativa;
- `parentStageExecutionId` preserva linhagem;
- toda saída informa a versão de conhecimento consumida;
- todo gate informa quais versões avaliou.

---

## 14. Invalidação seletiva

A invalidação deve ser transitiva, mas limitada ao grafo de dependências afetado.

Exemplo:

```text
Source 44 é rejeitada
→ Claims 301 e 302 são invalidados
→ Cluster de corroboração 88 é recalculado
→ Síntese operacional V3 é invalidada
→ Quality Gate V3 é invalidado
```

Permanecem válidos:

```text
Candidate V2
Query Plan V2
Demais resultados de busca
Demais fontes aprovadas
Claims 303–340 não dependentes da Source 44
```

Uma implementação que apague ou recrie todo o ciclo não atende este plano.

---

## 15. Query memory e prevenção de repetição

Antes de emitir uma nova query, o planejador deve consultar:

- hash normalizado da query;
- similaridade semântica com queries anteriores;
- objetivo da query;
- resultado anterior;
- quantidade e qualidade dos resultados;
- assinatura do erro de busca.

Estados sugeridos:

```text
USEFUL
ZERO_RESULT
LOW_RELEVANCE
CONTAMINATED
REDUNDANT
TECHNICALLY_FAILED
```

Regras:

- `USEFUL`: não repetir; reutilizar resultados;
- `ZERO_RESULT`: só repetir após relaxamento explícito;
- `LOW_RELEVANCE`: reformular com novos termos ou domínio;
- `CONTAMINATED`: adicionar exclusões obrigatórias;
- `REDUNDANT`: não executar;
- `TECHNICALLY_FAILED`: pode executar novamente sem contar como repetição cognitiva.

---

## 16. Source memory

A memória de fontes deve impedir:

- novo fetch da mesma URL canônica;
- novo fetch de conteúdo com mesmo hash;
- uso de fonte rejeitada como evidência em tentativa posterior;
- contagem de espelhos como fontes independentes;
- reintrodução de domínio safety hard reject.

Uma fonte rejeitada pode ser reconsiderada somente se:

- a regra de avaliação mudou de versão;
- o conteúdo da página mudou;
- houve erro técnico na coleta anterior;
- revisão humana autorizou reavaliação.

---

## 17. Aprendizado negativo estruturado

O pipeline deve aprender também com o que deu errado.

Exemplo:

```json
{
  "failureSignature": "ACTOR_INVERSION:AIRLINE_CANCELS_FLIGHT->PASSENGER_NO_SHOW_DRIVER",
  "scope": "CANDIDATE",
  "status": "ACTIVE",
  "constraints": [
    "REQUIRE_DRIVER_AS_AFFECTED_ACTOR",
    "EXCLUDE_PASSENGER_RIGHTS_CONTENT",
    "PRESERVE_CAUSAL_DIRECTION"
  ]
}
```

Esse aprendizado deve entrar no `Source Judge`, no `Claim Entailment Validator` e no próximo `Query Planner`.

Não deve ser enviado apenas como um texto longo e livre dentro do prompt.

---

## 18. Controle de loops e orçamento

Configuração inicial recomendada:

```json
{
  "maxCognitiveAttemptsPerStage": 3,
  "maxTechnicalRetriesPerAttempt": 3,
  "maxTotalCognitiveReprocess": 8,
  "maxCandidateVersions": 3,
  "minimumEvidenceGain": 0.10,
  "maxConsecutiveNoGainAttempts": 2,
  "maximumCycleAiCost": 1.50,
  "maximumCycleQueries": 80,
  "maximumCycleFetches": 30,
  "rejectRepeatedQueryHash": true,
  "stopOnRepeatedFailureSignature": true
}
```

O valor final deve ser calibrado com dados reais, mas a existência dos limites é obrigatória.

---

## 19. Cálculo de ganho informacional

Após cada tentativa cognitiva, calcular:

```text
informationGain =
  weightNewAcceptedClaims
  + weightNewIndependentDomains
  + weightGapReduction
  + weightValidationLevelIncrease
  + weightActorContextImprovement
  - weightNewContamination
  - weightContradictions
  - weightCost
```

Sinais mínimos de ganho:

- novos claims aceitos;
- novos domínios independentes;
- redução de gaps;
- melhora de `actorMatch`;
- melhora de `contextMatch`;
- avanço de nível E0–E5;
- redução de dependência em inferências indiretas.

Se duas tentativas cognitivas consecutivas não produzirem ganho mínimo:

```text
MANUAL_REVIEW_REQUIRED
```

ou:

```text
NO_PUBLIC_EVIDENCE_AVAILABLE
```

---

## 20. Novos estados e reason codes

Estados adicionais:

```text
REPROCESS_PLANNED
REPROCESSING
NO_PUBLIC_EVIDENCE_AVAILABLE
REPROCESS_BUDGET_EXHAUSTED
REPROCESS_NO_INFORMATION_GAIN
```

Reason codes iniciais:

```text
MISSING_EXECUTOR_ROUTINE_EVIDENCE
MISSING_OPERATIONAL_PAIN_EVIDENCE
MISSING_ECONOMIC_IMPACT
MISSING_ACQUISITION_BEHAVIOR
SOURCE_ACTOR_MISMATCH
SOURCE_CONTEXT_MISMATCH
SOURCE_MIX_CONTAMINATED
CLAIM_NOT_ENTAILED
SYNTHESIS_TOO_GENERIC
CANDIDATE_NO_LONGER_VIABLE
NAME_REQUIRES_REPAIR
TECHNICAL_PROVIDER_FAILURE
REPEATED_FAILURE_SIGNATURE
NO_INFORMATION_GAIN
BUDGET_EXHAUSTED
```

---

## 21. Persistência recomendada

Avaliar aderência às tabelas atuais antes de criar novas estruturas. Caso necessário, considerar:

## 21.1 `oprm_knowledge_snapshot`

Campos:

- `id`;
- `research_cycle_id`;
- `candidate_id`;
- `candidate_version`;
- `knowledge_version`;
- `validated_facts_json`;
- `tentative_hypotheses_json`;
- `rejected_knowledge_json`;
- `contradictions_json`;
- `evidence_gaps_json`;
- `constraints_json`;
- `budget_snapshot_json`;
- `created_at`.

## 21.2 `oprm_reprocess_plan`

Campos:

- `id`;
- `research_cycle_id`;
- `candidate_id`;
- `triggering_gate_execution_id`;
- `execution_mode`;
- `reason_code`;
- `rewind_to_stage`;
- `retain_artifacts_json`;
- `invalidate_artifacts_json`;
- `new_gaps_json`;
- `new_constraints_json`;
- `additional_budget_json`;
- `status`;
- `created_at`;
- `completed_at`.

## 21.3 `oprm_artifact_lineage`

Campos:

- `id`;
- `research_cycle_id`;
- `artifact_type`;
- `artifact_id`;
- `artifact_version`;
- `parent_artifact_type`;
- `parent_artifact_id`;
- `dependency_kind`;
- `status`;
- `created_at`.

## 21.4 `oprm_failure_signature`

Campos:

- `id`;
- `research_cycle_id`;
- `candidate_id`;
- `signature_code`;
- `scope`;
- `constraints_json`;
- `occurrence_count`;
- `status`;
- `created_at`;
- `last_seen_at`.

Todas as estruturas devem ser implementadas via backend e Liquibase, seguindo os padrões arquiteturais existentes e compatibilidade com MySQL 5.7.

---

## 22. Fluxo operacional

```text
1. Estágio produz artefato imutável
2. Knowledge Accumulator atualiza snapshot
3. Gate avalia snapshot e artefatos atuais
4. Gate retorna PASS, FAIL_FINAL ou REPROCESS
5. Reprocess Controller classifica motivo
6. Controller calcula menor estágio de rewind
7. Artifact Dependency Graph calcula invalidação
8. Controller cria Reprocess Plan
9. Estágio-alvo recebe snapshot + gaps + constraints
10. Nova tentativa gera novos artefatos
11. Knowledge Accumulator cria nova versão
12. Gate mede ganho informacional
13. Processo continua, materializa ou encerra
```

---

## 23. Pseudocódigo do controlador

```text
handleGateResult(gateResult):
    if gateResult.status == PASS:
        schedule(MATERIALIZER)
        return

    if gateResult.failureType == INFRASTRUCTURE:
        scheduleTechnicalRetry(gateResult.failedStage)
        return

    snapshot = knowledgeAccumulator.buildSnapshot(gateResult.researchCycleId)
    reason = classifyReason(gateResult, snapshot)

    if budgetExceeded(snapshot) or repeatedFailureWithoutGain(snapshot, reason):
        finishAsManualReviewOrNoEvidence(reason)
        return

    rewindStage = findMinimumRewindStage(reason, snapshot)
    invalidations = dependencyGraph.calculateInvalidations(reason, snapshot)
    constraints = buildNewConstraints(reason, snapshot)
    gaps = buildEvidenceGaps(reason, snapshot)

    plan = createReprocessPlan(
        rewindStage,
        invalidations,
        constraints,
        gaps,
        nextKnowledgeVersion(snapshot)
    )

    schedule(plan)
```

---

## 24. Alterações nos prompts

Os prompts não devem receber um bloco livre gigantesco com todo o histórico.

Cada estágio deve receber apenas:

- fatos validados relevantes;
- hipóteses tentativas relevantes;
- gaps que o estágio deve resolver;
- restrições negativas aplicáveis;
- queries ou fontes que não devem ser repetidas;
- orçamento disponível;
- versão do candidato;
- versão do conhecimento.

Exemplo para o query planner:

```json
{
  "validatedContext": {
    "executor": "Motoboy autônomo de documentos empresariais",
    "validatedJobs": ["COLETA_DOCUMENTO", "ENTREGA_DOCUMENTO"]
  },
  "evidenceGaps": [
    "FAILED_DELIVERY_REWORK",
    "ECONOMIC_IMPACT"
  ],
  "negativeConstraints": [
    "EXCLUDE_FOOD_DELIVERY_APP",
    "EXCLUDE_GENERIC_EMPLOYMENT_RIGHTS",
    "DO_NOT_REPEAT_QUERY_HASHES"
  ],
  "remainingBudget": {
    "queries": 12
  }
}
```

---

## 25. Testes de regressão

## 25.1 Ciclo 70

Dado:

```text
previousQualityStatus = SOLUTION_CONTAMINATED
previousNextMoveCode = REFAZER_BUSCA_SEM_SOLUCAO
```

Esperado:

- manter candidato se ainda viável;
- preservar fontes e claims não contaminados;
- invalidar somente artefatos derivados de solução;
- retornar ao query planner;
- adicionar exclusões de software, curso, app e automação;
- não recriar todo o seed sem necessidade.

## 25.2 Ciclo 72

Dado:

```text
previousQualityStatus = NEEDS_EXECUTOR_ROUTINE_EVIDENCE
previousNextMoveCode = BUSCAR_TAREFAS_REAIS_EXECUTOR
```

Esperado:

- manter claims já aprovados de existência do serviço;
- pesquisar apenas rotina e falhas operacionais faltantes;
- excluir contexto de delivery por aplicativo;
- não repetir queries úteis já concluídas;
- recalcular apenas claims, síntese e gate afetados.

## 25.3 Ciclo 74 — gate aprovado

Dado:

```text
previousQualityStatus = MEI_AUDIENCE_READY
previousNextMoveCode = MATERIALIZAR_NICHO
```

Esperado:

- executar diretamente o materializador;
- não gerar novo seed;
- não executar nova busca;
- não consumir novo orçamento de pesquisa.

## 25.4 Ciclo 74 — broken pipe

Dado:

```text
falha técnica no audience-profile-synthesizer
```

Esperado:

- retry no mesmo estágio;
- mesma versão de conhecimento;
- mesma entrada lógica;
- incremento apenas de `technicalRetryNumber`;
- nenhum novo seed, query, fetch ou claim.

## 25.5 Alteração de candidato

Dado:

```text
candidateVersion 1 foi rejeitada no torneio após pesquisa exploratória
```

Esperado:

- preservar histórico da versão 1;
- criar candidateVersion 2;
- impedir que claims da versão 1 sejam promovidos para a versão 2 sem revalidação de contexto;
- reutilizar apenas fontes explicitamente compatíveis com ambos os candidatos.

---

## 26. Observabilidade

Métricas adicionais:

```text
cognitive_reprocess_count
technical_retry_count
artifact_reuse_rate
artifact_invalidation_rate
query_reuse_avoided_count
source_refetch_avoided_count
knowledge_version_count
information_gain_per_attempt
cost_per_information_gain
reprocess_no_gain_count
reprocess_budget_exhausted_count
manual_review_after_reprocess_count
```

Logs obrigatórios:

- motivo do rewind;
- estágio de retorno;
- artefatos preservados;
- artefatos invalidados;
- versão de conhecimento anterior e nova;
- ganho informacional;
- orçamento anterior e remanescente;
- assinatura de falha;
- razão de encerramento.

---

## 27. Ordem de implementação

### P0

1. tornar execuções de estágio imutáveis e versionadas;
2. separar tentativa cognitiva de retry técnico;
3. implementar Artifact Dependency Graph mínimo;
4. implementar invalidação seletiva;
5. impedir reinício amplo após falha posterior;
6. implementar transição direta para materialização após gate aprovado.

### P1

1. implementar Knowledge Accumulator;
2. implementar snapshot estruturado;
3. implementar Reprocess Controller;
4. implementar matriz de rewind;
5. implementar query memory e source memory;
6. persistir failure signatures estruturadas.

### P2

1. implementar cálculo de ganho informacional;
2. calibrar budgets e limites;
3. adicionar revisão humana seletiva;
4. criar dashboard de versões, tentativas e artefatos reutilizados;
5. avaliar promoção de conhecimento validado entre ciclos diferentes do mesmo CNAE.

---

## 28. Critérios de aceite

A implementação será aceita quando:

- um gate puder solicitar reprocessamento no mesmo `researchCycleId`;
- o controlador retornar ao menor estágio necessário;
- artefatos aprovados não forem recriados sem necessidade;
- fontes rejeitadas não reaparecerem como evidência;
- queries úteis não forem repetidas;
- retry técnico não alterar a versão de conhecimento;
- tentativa cognitiva criar nova versão de conhecimento;
- toda invalidação possuir linhagem auditável;
- o pipeline distinguir `TENTATIVE`, `VALIDATED`, `REJECTED`, `CONTRADICTED`, `SUPERSEDED` e `STALE`;
- ciclos sem ganho sejam encerrados com motivo explícito;
- o ciclo 74 aprovado vá diretamente à materialização;
- o broken pipe do ciclo 74 repita somente a etapa técnica;
- o ciclo 72 reutilize conhecimento válido e pesquise apenas os gaps;
- o relatório final mostre todas as tentativas e o reaproveitamento realizado.

---

## 29. Resultado esperado

Com esta alteração, um resultado inadequado deixa de provocar um novo processamento quase completo. O pipeline passa a:

- lembrar o que já provou;
- lembrar o que já rejeitou;
- saber exatamente o que ainda falta;
- retornar somente ao ponto necessário;
- usar erros anteriores como restrições;
- medir se a nova tentativa realmente adicionou conhecimento;
- controlar custo e loops;
- preservar a auditoria completa do job.

A nova regra operacional é:

> O pipeline não recomeça; ele evolui o conhecimento do mesmo ciclo até materializar, concluir que não há evidência pública suficiente ou encaminhar para revisão humana.
