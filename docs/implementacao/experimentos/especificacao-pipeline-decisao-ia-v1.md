# Especificação — pipeline de apoio à decisão por IA `experimentdecision.v1`

## 1. Objetivo

Criar um pipeline versionado que use modelos de IA para interpretar evidências de experimentos, explicar gargalos, propor alternativas e recomendar o próximo teste, sem transferir ao modelo a responsabilidade por:

- calcular métricas oficiais;
- validar integridade dos dados;
- decidir se uma execução chegou validamente ao mercado;
- declarar significância ou nível de evidência;
- executar ações comerciais;
- substituir a decisão humana.

A sequência obrigatória é:

```text
Snapshot determinístico do backend
  → análise estruturada do modelo
    → revisão crítica do modelo
      → validação de contrato/evidências pelo backend
        → recomendação apresentada ao usuário
          → decisão humana
            → comando explícito do backend
```

---

## 2. Problema que o pipeline resolve

O aprendizado automático atual consolida métricas e ativos em uma única leitura. Isso é útil, mas insuficiente para decisões comerciais confiáveis porque:

- prompts e contratos precisam estar versionados fora das classes;
- o modelo não deve receber apenas `status=INVALIDATED`;
- execuções técnicas inválidas precisam ser excluídas do aprendizado de mercado;
- toda afirmação precisa apontar para evidências persistidas;
- alternativas precisam ser consideradas antes da recomendação;
- recomendação, decisão e ação precisam ser entidades separadas;
- request, response, tokens, custo e modelo precisam estar vinculados ao job;
- falha da IA não pode bloquear a leitura determinística do experimento.

O pipeline v1 não substitui imediatamente `ExperimentLearning`. O fluxo legado permanece disponível até a nova implementação ser validada e migrada.

---

## 3. Princípios obrigatórios

## 3.1 Cálculos pertencem ao backend

O backend calcula e persiste:

- métricas;
- taxas;
- margem;
- intervalos de incerteza;
- nível de evidência;
- qualidade/freshness;
- comparabilidade;
- validade do `ExperimentRun`;
- políticas de parada;
- comandos permitidos.

O modelo recebe esses valores e não os substitui por cálculos próprios.

## 3.2 Evidência antes de narrativa

Toda afirmação material da IA deve citar `evidenceIds` existentes no snapshot.

Exemplo válido:

```json
{
  "claim": "A captura é o maior gargalo observado",
  "evidenceIds": [
    "metric:run-123:form-views",
    "metric:run-123:form-submissions",
    "gate:run-123:functional-e2e"
  ]
}
```

Afirmação sem referência é rejeitada ou rebaixada para hipótese explícita.

## 3.3 Confiança do modelo não é evidência estatística

Separar:

```text
evidenceLevel        → calculado pelo backend
modelConfidence      → autopercepção limitada do modelo
recommendationStatus → validado pelo backend
humanDecision        → decisão do usuário autorizado
```

O frontend nunca apresenta `modelConfidence` como probabilidade de sucesso.

## 3.4 Ausência de dado é informação

O modelo deve declarar:

- dados ausentes;
- fontes atrasadas;
- runs inválidos;
- comparabilidade comprometida;
- hipóteses não testadas;
- limites da recomendação.

## 3.5 Sem PII desnecessária

O snapshot deve ser pseudonimizado. Não enviar ao modelo:

- nome do lead;
- e-mail;
- telefone;
- endereço;
- conteúdo de upload pessoal;
- tokens/magic links;
- payload bruto de webhook com PII.

Pode enviar agregados e exemplos sanitizados quando indispensáveis e autorizados.

## 3.6 Backend controla o avanço

O AI Worker:

- busca execução pendente;
- executa uma etapa;
- valida schema localmente;
- reporta resultado ao backend.

O backend:

- valida o resultado;
- persiste o artefato;
- decide se cria a próxima execução;
- encerra ou bloqueia o pipeline.

Uma etapa concreta não chama outra etapa concreta.

---

## 4. Pré-condições para solicitar análise

O backend só cria `ExperimentDecisionRun` quando:

- existe ao menos um `ExperimentRun` relevante;
- runs usados possuem validade conhecida;
- métricas oficiais foram materializadas;
- snapshot de qualidade foi calculado;
- não existe decisão run ativa equivalente;
- o usuário possui permissão;
- a feature flag está ativa.

### Análise de experimento individual

Permitida para:

- diagnosticar bloqueio;
- interpretar funil;
- sugerir correção ou próximo experimento;
- classificar se deve coletar mais dados.

### Análise de comparação

Exige:

- comparação existente;
- braços definidos;
- comparabilidade calculada;
- runs comercialmente válidos selecionados;
- mesmo snapshot temporal para todos os braços.

A análise pode ocorrer com evidência insuficiente, mas a única recomendação de conclusão permitida será continuar, corrigir qualidade ou marcar inconclusivo.

---

## 5. Pipeline versionado

Código lógico:

```text
experimentdecision.v1
```

Executor:

```text
ai-worker
```

Pacote do executor:

```text
com.marketinghub.worker.experimentdecisionv1.pipeline
```

Pacote do backend:

```text
com.marketinghub.experiment.decision.v1
```

## 5.1 Etapas

```text
1. SIGNAL_DIAGNOSIS
2. ALTERNATIVE_GENERATION
3. RECOMMENDATION_REVIEW
```

O backend realiza antes e depois das etapas:

```text
SNAPSHOT_ASSEMBLY          — determinístico, backend
FINAL_CONTRACT_VALIDATION  — determinístico, backend
```

### Etapa 1 — `SIGNAL_DIAGNOSIS`

Objetivo:

- interpretar o funil;
- localizar gargalos;
- separar fatos, inferências e incógnitas;
- classificar causas prováveis;
- verificar consistência com o histórico;
- identificar o que não pode ser concluído.

Não deve propor ainda uma única ação final.

### Etapa 2 — `ALTERNATIVE_GENERATION`

Objetivo:

- gerar de duas a quatro alternativas;
- manter uma variável comercial principal por próximo teste;
- estimar benefícios, riscos e requisitos;
- indicar se é correção técnica, novo run ou novo experimento;
- excluir alternativas incompatíveis com os gates.

### Etapa 3 — `RECOMMENDATION_REVIEW`

Objetivo:

- revisar criticamente diagnóstico e alternativas;
- procurar contradições;
- verificar evidências;
- rejeitar causalidade não sustentada;
- escolher recomendação principal ou declarar que não há base;
- produzir pacote final para decisão humana.

Preferencialmente, a revisão usa um contexto limpo contendo os artefatos aprovados e o snapshot, não a cadeia de raciocínio privada do modelo anterior.

---

## 6. Modelo de dados do backend

## 6.1 `experiment_decision_run`

```text
experiment_decision_run
- id BINARY(16) PK
- experiment_id BIGINT nullable
- comparison_id BIGINT nullable
- pipeline_version VARCHAR(64) NOT NULL
- snapshot_id BINARY(16) NOT NULL
- status VARCHAR(32) NOT NULL
- requested_by VARCHAR(191) nullable
- requested_at DATETIME(6) NOT NULL
- started_at DATETIME(6) nullable
- completed_at DATETIME(6) nullable
- failure_classification VARCHAR(64) nullable
- failure_reason LONGTEXT nullable
- created_at DATETIME(6) NOT NULL
- updated_at DATETIME(6) NOT NULL
```

Regra XOR:

- exatamente um entre `experiment_id` e `comparison_id` deve estar preenchido.

Status:

```text
PENDING
PROCESSING
READY
FAILED
CANCELLED
SUPERSEDED
```

## 6.2 `experiment_decision_snapshot`

```text
experiment_decision_snapshot
- id BINARY(16) PK
- scope_type VARCHAR(32) NOT NULL
- scope_id VARCHAR(64) NOT NULL
- schema_version VARCHAR(64) NOT NULL
- canonical_input_hash VARCHAR(64) NOT NULL
- payload_json LONGTEXT NOT NULL
- quality_status VARCHAR(32) NOT NULL
- assembled_at DATETIME(6) NOT NULL
- created_at DATETIME(6) NOT NULL
```

Snapshot é imutável.

## 6.3 `experiment_decision_stage_execution`

```text
experiment_decision_stage_execution
- id BINARY(16) PK
- decision_run_id BINARY(16) NOT NULL FK
- stage_code VARCHAR(96) NOT NULL
- stage_position INT NOT NULL
- attempt_number INT NOT NULL
- technical_retry_number INT NOT NULL
- status VARCHAR(32) NOT NULL
- prompt_version VARCHAR(96) NOT NULL
- schema_version VARCHAR(96) NOT NULL
- model VARCHAR(120) nullable
- provider VARCHAR(64) nullable
- service_tier VARCHAR(32) nullable
- request_payload LONGTEXT nullable
- raw_response LONGTEXT nullable
- structured_output LONGTEXT nullable
- input_tokens INT nullable
- output_tokens INT nullable
- cost_usd DECIMAL(12,6) nullable
- requested_at DATETIME(6) NOT NULL
- started_at DATETIME(6) nullable
- completed_at DATETIME(6) nullable
- error_code VARCHAR(96) nullable
- error_message LONGTEXT nullable
- created_at DATETIME(6) NOT NULL
- updated_at DATETIME(6) NOT NULL
```

Restrições:

```text
UNIQUE (decision_run_id, stage_code, attempt_number, technical_retry_number)
```

## 6.4 Artefatos

```text
experiment_decision_artifact
- id BINARY(16) PK
- decision_run_id BINARY(16) NOT NULL
- stage_execution_id BINARY(16) nullable
- artifact_type VARCHAR(96) NOT NULL
- schema_version VARCHAR(96) NOT NULL
- content_json LONGTEXT NOT NULL
- content_hash VARCHAR(64) NOT NULL
- status VARCHAR(32) NOT NULL
- created_at DATETIME(6) NOT NULL
```

Tipos iniciais:

```text
SIGNAL_DIAGNOSIS_V1
DECISION_ALTERNATIVES_V1
RECOMMENDATION_REVIEW_V1
FINAL_RECOMMENDATION_V1
```

## 6.5 Decisão humana

```text
experiment_human_decision
- id BINARY(16) PK
- decision_run_id BINARY(16) NOT NULL
- recommendation_artifact_id BINARY(16) NOT NULL
- decision VARCHAR(48) NOT NULL
- rationale LONGTEXT NOT NULL
- selected_action_code VARCHAR(96) nullable
- risk_acknowledged BOOLEAN NOT NULL
- decided_by VARCHAR(191) NOT NULL
- decided_at DATETIME(6) NOT NULL
- supersedes_decision_id BINARY(16) nullable
- created_at DATETIME(6) NOT NULL
```

Decisões:

```text
ACCEPT_RECOMMENDATION
REJECT_RECOMMENDATION
MODIFY_RECOMMENDATION
DEFER_DECISION
```

## 6.6 Comando separado

```text
experiment_decision_command
- id BINARY(16) PK
- human_decision_id BINARY(16) NOT NULL
- command_code VARCHAR(96) NOT NULL
- command_payload LONGTEXT nullable
- status VARCHAR(32) NOT NULL
- requested_by VARCHAR(191) NOT NULL
- requested_at DATETIME(6) NOT NULL
- completed_at DATETIME(6) nullable
- result_summary LONGTEXT nullable
- failure_reason LONGTEXT nullable
```

Registrar decisão não cria comando automaticamente.

---

## 7. Snapshot canônico

## 7.1 Estrutura

```json
{
  "schemaVersion": "experiment-decision-snapshot.v1",
  "scope": {},
  "businessQuestion": {},
  "strategy": {},
  "validRuns": [],
  "excludedRuns": [],
  "upstreamQuality": {},
  "assets": {},
  "audience": {},
  "funnel": {},
  "economics": {},
  "comparison": {},
  "dataQuality": {},
  "evidenceLevel": {},
  "stopPolicy": {},
  "historicalLearnings": [],
  "evidenceCatalog": []
}
```

## 7.2 Runs excluídos

Cada exclusão deve informar:

```json
{
  "runId": 12,
  "evidenceValidity": "TECHNICALLY_INVALID",
  "reasonCodes": ["FORM_FUNCTIONAL_FAILURE"],
  "allowedUse": "OPERATIONAL_LEARNING_ONLY"
}
```

O modelo pode usar esses runs para sugerir correções operacionais, mas não para concluir demanda.

## 7.3 Catálogo de evidências

```json
{
  "evidenceId": "metric:run-123:form-submit-rate",
  "type": "METRIC",
  "label": "Taxa de envio do formulário",
  "value": 0.0,
  "unit": "PERCENT",
  "period": {},
  "source": "EXPERIMENT_COMMERCIAL_EVENT",
  "freshness": "CURRENT",
  "quality": "VALID",
  "drillDownReference": "funnel-stage:FORM_SUBMITTED"
}
```

Tipos:

```text
METRIC
GATE
EVENT_COUNT
COST
REVENUE
QUALITY_FLAG
ASSET_DIAGNOSTIC
AUDIENCE_DIAGNOSTIC
HISTORICAL_DECISION
SOURCE_EVIDENCE
```

---

## 8. Contrato da etapa `SIGNAL_DIAGNOSIS`

Arquivo de prompt:

```text
ai-worker/src/main/resources/prompts/experiment-decision/v1/signal-diagnosis.md
```

Schema:

```text
ai-worker/src/main/resources/prompts/experiment-decision/v1/signal-diagnosis-schema.json
```

Saída:

```json
{
  "schemaVersion": "experiment-signal-diagnosis.v1",
  "executiveSummary": "",
  "observedFacts": [
    {
      "statement": "",
      "evidenceIds": []
    }
  ],
  "primaryBottleneck": {
    "code": "CAPTURE",
    "statement": "",
    "evidenceIds": []
  },
  "probableCauses": [
    {
      "classification": "FORM_FUNCTIONAL_FAILURE",
      "statement": "",
      "supportingEvidenceIds": [],
      "contradictingEvidenceIds": [],
      "confidence": "LOW|MEDIUM|HIGH",
      "nature": "FACT|INFERENCE|HYPOTHESIS"
    }
  ],
  "unknowns": [],
  "invalidConclusions": [],
  "dataRequests": [],
  "commercialLearningAllowed": true
}
```

Regras:

- fatos exigem evidência;
- inferências precisam ser rotuladas;
- hipótese comercial não pode ser reprovada se `commercialLearningAllowed=false`;
- não recalcular métricas;
- não recomendar ação final.

---

## 9. Contrato da etapa `ALTERNATIVE_GENERATION`

Prompt:

```text
ai-worker/src/main/resources/prompts/experiment-decision/v1/alternative-generation.md
```

Schema:

```text
ai-worker/src/main/resources/prompts/experiment-decision/v1/alternative-generation-schema.json
```

Saída:

```json
{
  "schemaVersion": "experiment-decision-alternatives.v1",
  "alternatives": [
    {
      "alternativeId": "ALT-1",
      "actionType": "CREATE_NEW_RUN|CREATE_DERIVED_EXPERIMENT|KEEP_COLLECTING|PAUSE_AND_FIX|STOP",
      "title": "",
      "rationale": "",
      "primaryVariableToChange": "",
      "controlledVariables": [],
      "requiredCorrections": [],
      "expectedLearning": "",
      "successMetric": "",
      "risks": [],
      "prerequisites": [],
      "supportingEvidenceIds": [],
      "tradeoffs": []
    }
  ],
  "rejectedOptions": []
}
```

Regras:

- duas a quatro alternativas;
- próximo experimento muda uma variável principal;
- correção técnica usa novo run;
- não sugerir elevar orçamento com validade ou qualidade bloqueada;
- não trocar simultaneamente dor, oferta, público, landing e métrica como se fosse teste causal;
- pode propor reconstrução ampla, mas deve classificá-la como exploração, não comparação causal.

---

## 10. Contrato da etapa `RECOMMENDATION_REVIEW`

Prompt:

```text
ai-worker/src/main/resources/prompts/experiment-decision/v1/recommendation-review.md
```

Schema:

```text
ai-worker/src/main/resources/prompts/experiment-decision/v1/recommendation-review-schema.json
```

Saída:

```json
{
  "schemaVersion": "experiment-recommendation-review.v1",
  "reviewStatus": "APPROVED|APPROVED_WITH_LIMITATIONS|REJECTED",
  "detectedContradictions": [],
  "unsupportedClaims": [],
  "evidenceCoverage": {
    "status": "COMPLETE|PARTIAL|INSUFFICIENT",
    "missingEvidenceIds": []
  },
  "recommendedAlternativeId": "ALT-1",
  "recommendationCode": "KEEP_COLLECTING_DATA",
  "recommendationSummary": "",
  "whyNow": "",
  "evidenceIds": [],
  "limitations": [],
  "risks": [],
  "nextDecisionCondition": "",
  "modelConfidence": "LOW|MEDIUM|HIGH"
}
```

Regras:

- pode rejeitar todas as alternativas;
- `recommendedAlternativeId` precisa existir;
- referências precisam existir no snapshot;
- recomendação deve ser compatível com `allowedRecommendationCodes` fornecidos pelo backend;
- `DECLARE_*_WINNER` só é permitido quando o backend marcar `evidenceLevel=DECISION_READY`.

---

## 11. Validação final no backend

O backend deve rejeitar ou marcar como falha funcional quando:

- schema inválido;
- evidence ID inexistente;
- recomendação fora da lista permitida;
- alternativa escolhida inexistente;
- conclusão comercial baseada somente em run excluído;
- mudança proposta viola regra de uma variável;
- declaração de vencedor sem evidência suficiente;
- saída contém PII;
- resposta contém campos não contratuais críticos;
- artifact hash ou snapshot hash diverge.

Validação semântica mínima:

```text
RecommendationPolicyValidator
EvidenceReferenceValidator
CommercialValidityValidator
SinglePrimaryVariableValidator
PiiLeakValidator
AllowedActionValidator
```

---

## 12. Códigos de recomendação

```text
KEEP_COLLECTING_DATA
PAUSE_FOR_DATA_QUALITY
CREATE_NEW_TECHNICAL_RUN
STOP_BY_STOP_LOSS
ITERATE_CAPTURE_ROUTE
ITERATE_PROFILE_ENRICHMENT
ITERATE_SAMPLE
ITERATE_OFFER
ITERATE_PRICE
ITERATE_CREATIVE
ITERATE_AUDIENCE
DECLARE_CONTROL_WINNER
DECLARE_CHALLENGER_WINNER
MARK_INCONCLUSIVE
CREATE_FOLLOW_UP_TEST
DO_NOT_INTERPRET_MARKET
```

O backend envia ao modelo apenas os códigos permitidos no contexto atual.

---

## 13. Configuração de modelos

Cada etapa deve usar configuração administrativa já persistida no backend ou contrato equivalente:

- provider;
- model;
- prompt version;
- schema version;
- reasoning configuration;
- timeout;
- limite de tokens;
- ativo/inativo.

Quando OpenAI:

```json
{
  "service_tier": "flex"
}
```

Exceção exige justificativa funcional persistida.

Prompts, schemas e contratos não podem ficar hardcoded em classes Java.

---

## 14. Estrutura no AI Worker

```text
com.marketinghub.worker.experimentdecisionv1.pipeline
  PipelineWorker
  StageProcessor
  StageContext
  StageResult
  StageArtifact
  BackendPort

com.marketinghub.worker.experimentdecisionv1.pipeline.signaldiagnosis
com.marketinghub.worker.experimentdecisionv1.pipeline.alternativegeneration
com.marketinghub.worker.experimentdecisionv1.pipeline.recommendationreview
```

O núcleo genérico não conhece:

- etapas concretas;
- OpenAI;
- WebClient específico;
- DTO tecnológico;
- regras comerciais específicas de uma etapa.

Cada etapa mantém dentro do próprio pacote:

- processor;
- client;
- propriedades;
- prompt loader;
- schema loader;
- mapper;
- validator local.

Etapas não importam umas às outras.

---

## 15. Contratos internos

Padrão inicial:

```text
GET /api/internal/experiment-decision/v1/signal-diagnosis/stage-executions/pending
PATCH /api/internal/experiment-decision/v1/signal-diagnosis/stage-executions/{executionId}

GET /api/internal/experiment-decision/v1/alternative-generation/stage-executions/pending
PATCH /api/internal/experiment-decision/v1/alternative-generation/stage-executions/{executionId}

GET /api/internal/experiment-decision/v1/recommendation-review/stage-executions/pending
PATCH /api/internal/experiment-decision/v1/recommendation-review/stage-executions/{executionId}
```

O payload `pending` inclui:

- execution ID;
- decision run ID;
- stage;
- attempt;
- snapshot;
- artefatos anteriores aprovados;
- modelo/configuração;
- prompt variables;
- allowed recommendation codes;
- canonical input hash.

Não incluir acesso direto ao banco ou URLs administrativas para descoberta de trabalho.

---

## 16. Retries, reprocessamento e idempotência

### Retry técnico

- repete a mesma execução lógica;
- incrementa `technical_retry_number`;
- não muda snapshot;
- não muda artefatos aprovados;
- usado para timeout, 429, erro transitório ou indisponibilidade.

### Nova tentativa cognitiva

- incrementa `attempt_number`;
- preserva execução anterior;
- pode trocar modelo/prompt version mediante comando explícito;
- mantém ou cria novo snapshot conforme a causa.

### Novo decision run

Criar quando:

- métricas mudaram materialmente;
- janela avançou;
- run comercial novo foi concluído;
- estratégia mudou;
- comparação mudou;
- decisão anterior foi superada por novos dados.

Chave de deduplicação:

```text
(scope, canonical_input_hash, pipeline_version, status ativo)
```

---

## 17. Segurança e privacidade

- whitelist de campos enviados;
- mascaramento antes da persistência auditável quando necessário;
- detector de PII antes da request;
- detector de PII na response;
- acesso restrito aos payloads brutos;
- nenhum secret em prompt ou artefato;
- URLs privadas convertidas em referências internas;
- retenção definida para request/response;
- trilha de quem solicitou e visualizou detalhes sensíveis.

---

## 18. Frontend

O painel deve mostrar:

- status do decision run;
- etapas concluídas;
- snapshot/data analisada;
- recomendação;
- evidências;
- limitações;
- alternativas;
- revisão crítica;
- modelo e versão;
- custo;
- decisão humana;
- ação posterior.

Não mostrar cadeia de raciocínio privada. Mostrar justificativa estruturada, evidências e critérios de decisão.

---

## 19. Feedback e calibração

Criar feedback após decisão e resultado posterior:

```text
experiment_decision_feedback
- decision_run_id
- human_decision_id
- recommendation_followed
- result_observed_at
- outcome_code
- outcome_metrics_snapshot_id
- evaluator
- notes
```

Métricas de qualidade do sistema:

- taxa de recomendação aceita;
- taxa de modificação;
- taxa de rejeição;
- evidence refs inválidas;
- recomendações bloqueadas por política;
- resultado posterior alinhado;
- custo por análise;
- tempo por etapa;
- falhas por modelo/prompt version.

Não otimizar apenas para aceitação humana. O objetivo é melhoria posterior das métricas comerciais e redução de decisões inválidas.

---

## 20. Migração do aprendizado legado

1. manter endpoints e painel atuais;
2. marcar `ExperimentLearning` como fluxo legado após v1 estar disponível;
3. não migrar resultados antigos automaticamente para recomendação v1;
4. permitir exibi-los como histórico não estruturado;
5. mover o prompt hardcoded para recurso versionado se o legado continuar ativo;
6. comparar v1 e legado em modo sombra;
7. desligar criação nova no legado somente após critérios de qualidade.

Modo sombra:

- decisão v1 roda sem aparecer como recomendação principal;
- equipe compara saída com leitura humana;
- nenhuma ação automática;
- resultados registrados para calibração.

---

## 21. Testes obrigatórios

### Backend

- snapshot imutável;
- exclusão de runs inválidos;
- hash canônico;
- transições por etapa;
- evidence refs;
- recomendação permitida;
- bloqueio de vencedor;
- decisão humana versionada;
- comando separado;
- deduplicação de decision run.

### AI Worker

- carregamento de prompts/schemas;
- modo Flex;
- parse de saída;
- schema inválido;
- resposta vazia;
- timeout/retry;
- auditoria de request/response;
- PII bloqueada;
- independência entre etapas;
- núcleo sem dependência tecnológica.

### Contrato

Fixtures dos experimentos históricos:

1. experimento 37: formulário não validado → `DO_NOT_INTERPRET_MARKET`;
2. experimento 38: publicação/configuração inválida → novo run técnico;
3. experimento 39: targeting/publicação e upstream frágeis → pausa para qualidade;
4. experimento 40: contaminação de evidência → reprocessar upstream;
5. comparação válida com amostra insuficiente → `KEEP_COLLECTING_DATA`;
6. comparação pronta → recomendação de vencedor permitida;
7. evidence ID inventado → execução rejeitada.

### Frontend

- etapas em processamento;
- falha parcial;
- recomendação bloqueada;
- limitações;
- decisão humana;
- detalhes de auditoria autorizados.

---

## 22. Sequência de PRs

### PR 1 — domínio e snapshot

- tabelas;
- serviços;
- snapshot;
- catálogo de evidências;
- APIs administrativas.

### PR 2 — estágio signal diagnosis

- backend pending/callback;
- worker;
- prompt/schema;
- auditoria;
- testes.

### PR 3 — alternative generation

- contrato;
- etapa;
- regra de variável única;
- testes.

### PR 4 — recommendation review

- revisão;
- validadores finais;
- recomendação final;
- testes.

### PR 5 — frontend e decisão humana

- painel;
- evidências;
- alternativas;
- decisão;
- histórico.

### PR 6 — modo sombra e calibração

- comparação com legado/humano;
- feedback;
- métricas operacionais.

---

## 23. Critérios de aceite

- [ ] snapshot é imutável e rastreável;
- [ ] runs inválidos não sustentam conclusão comercial;
- [ ] métricas vêm do backend;
- [ ] toda afirmação material cita evidência;
- [ ] prompts e schemas são arquivos versionados;
- [ ] cada etapa é independente;
- [ ] backend controla o avanço;
- [ ] OpenAI usa Flex por padrão;
- [ ] request, response, modelo, tokens e custo são auditáveis;
- [ ] PII desnecessária não é enviada;
- [ ] recomendação passa por revisão crítica;
- [ ] recomendação não executa comando;
- [ ] decisão humana é persistida;
- [ ] ação exige contrato separado;
- [ ] falha da IA não impede leitura determinística;
- [ ] fixtures históricas previnem repetição dos erros 37–40.
