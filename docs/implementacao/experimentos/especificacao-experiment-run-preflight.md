# Especificação — ExperimentRun, preflight e validade da evidência

## 1. Objetivo

Separar a hipótese comercial testada da tentativa operacional de colocá-la no mercado.

O `Experiment` continua representando uma materialização comercial atômica. O novo `ExperimentRun` representa cada tentativa de teste, publicação ou execução dessa materialização.

Essa separação resolve a causa-raiz observada nos experimentos 37–40: uma falha de formulário, publicação, targeting, analytics ou integração não pode invalidar automaticamente a hipótese comercial.

---

## 2. Regra de domínio

### 2.1 Quando criar novo run

Criar novo `ExperimentRun` quando a correção não altera a pergunta comercial principal:

- corrigir formulário;
- corrigir botão/submit;
- corrigir integração Meta;
- republicar campanha;
- corrigir asset inacessível;
- corrigir tracking;
- corrigir targeting tecnicamente inválido sem mudar o público estratégico;
- reiniciar após falha de infraestrutura;
- repetir a mesma estratégia com ativos equivalentes e mesma variável primária.

### 2.2 Quando criar novo experimento

Criar novo `Experiment` quando mudar qualquer alavanca comercial principal:

- dor de entrada;
- promessa;
- isca;
- rota de captura;
- amostra genérica versus personalizada;
- produto;
- oferta;
- preço;
- CTA principal;
- público estratégico;
- criativo principal quando ele for a variável do teste;
- métrica primária;
- pergunta de negócio.

### 2.3 Regra de aprendizado

Somente runs com `evidenceValidity=COMMERCIALLY_VALID` podem alimentar:

- comparação de braços;
- declaração de vencedor;
- aprendizado de mercado;
- decisão sobre escala;
- reprovação da materialização comercial;
- recomendação de próximo teste baseada em resultado.

Runs inválidos continuam alimentando aprendizado operacional e prevenção de recorrência.

---

## 3. Modelo de dados

## 3.1 `experiment_run`

```text
experiment_run
- id BIGINT PK
- experiment_id BIGINT NOT NULL FK
- run_number INT NOT NULL
- mode VARCHAR(24) NOT NULL
- status VARCHAR(32) NOT NULL
- evidence_validity VARCHAR(32) NOT NULL
- strategy_version INT nullable
- asset_bundle_version INT nullable
- audience_version INT nullable
- stop_policy VARCHAR(48) NOT NULL
- stop_reason VARCHAR(96) nullable
- failure_classification VARCHAR(64) nullable
- failure_detail LONGTEXT nullable
- data_quality_status VARCHAR(32) NOT NULL
- requested_at DATETIME(6) NOT NULL
- preflight_started_at DATETIME(6) nullable
- preflight_completed_at DATETIME(6) nullable
- publication_requested_at DATETIME(6) nullable
- published_at DATETIME(6) nullable
- first_verified_impression_at DATETIME(6) nullable
- commercial_window_started_at DATETIME(6) nullable
- ended_at DATETIME(6) nullable
- created_by VARCHAR(191) nullable
- created_at DATETIME(6) NOT NULL
- updated_at DATETIME(6) NOT NULL
```

Restrições:

```text
UNIQUE (experiment_id, run_number)
INDEX (experiment_id, status)
INDEX (status, requested_at)
INDEX (evidence_validity)
```

## 3.2 Enums

### `ExperimentRunMode`

```text
TEST
PRODUCTION
```

### `ExperimentRunStatus`

```text
DRAFT
PREFLIGHT_PENDING
PREFLIGHT_RUNNING
PREFLIGHT_FAILED
READY_TO_PUBLISH
PUBLICATION_PENDING
PUBLISHING
PUBLISHED_AWAITING_EXPOSURE
RUNNING
PAUSE_REQUESTED
PAUSED
STOP_REQUESTED
COMPLETED
FAILED
CANCELLED
```

### `ExperimentEvidenceValidity`

```text
NOT_EVALUATED
TECHNICALLY_INVALID
MEASUREMENT_INVALID
STRATEGICALLY_INVALID
INSUFFICIENT_DATA
COMMERCIALLY_VALID
```

### `ExperimentRunDataQualityStatus`

```text
UNKNOWN
VALID
WARNING
BLOCKED
STALE
```

### `ExperimentRunFailureClassification`

```text
INTEGRATION_FAILURE
PUBLICATION_FAILURE
MEASUREMENT_FAILURE
FORM_FUNCTIONAL_FAILURE
LANDING_QUALITY_FAILURE
UPSTREAM_DATA_QUALITY_FAILURE
STRATEGY_CONFIGURATION_FAILURE
AUDIENCE_FAILURE
CREATIVE_FAILURE
PROMISE_OR_LEAD_MAGNET_FAILURE
OFFER_FAILURE
PRICE_OR_CHECKOUT_FAILURE
INSUFFICIENT_DATA
COMMERCIAL_HYPOTHESIS_FAILURE
USER_STOPPED
```

## 3.3 `experiment_run_gate_result`

```text
experiment_run_gate_result
- id BIGINT PK
- experiment_run_id BIGINT NOT NULL FK
- gate_code VARCHAR(96) NOT NULL
- gate_group VARCHAR(48) NOT NULL
- status VARCHAR(24) NOT NULL
- severity VARCHAR(24) NOT NULL
- summary VARCHAR(512) NOT NULL
- evidence_reference VARCHAR(512) nullable
- remediation_code VARCHAR(96) nullable
- evaluated_at DATETIME(6) NOT NULL
- evaluator_type VARCHAR(24) NOT NULL
- evaluator_version VARCHAR(64) nullable
- created_at DATETIME(6) NOT NULL
```

Restrições:

```text
UNIQUE (experiment_run_id, gate_code)
```

Status:

```text
PASS
WARNING
FAIL
NOT_APPLICABLE
PENDING
```

Evaluator type:

```text
DETERMINISTIC
HUMAN
EXTERNAL_PROVIDER
```

Modelos de IA não devem marcar gate técnico como aprovado. Eles podem oferecer explicação auxiliar, persistida em outra estrutura.

## 3.4 `experiment_run_step`

Tabela auditável de passos do run:

```text
experiment_run_step
- id BIGINT PK
- experiment_run_id BIGINT NOT NULL FK
- job_id BINARY(16) NOT NULL
- step_code VARCHAR(96) NOT NULL
- module VARCHAR(96) NOT NULL
- status VARCHAR(24) NOT NULL
- attempt_number INT NOT NULL
- technical_retry_number INT NOT NULL
- action VARCHAR(191) nullable
- endpoint VARCHAR(512) nullable
- request_payload LONGTEXT nullable
- response_payload LONGTEXT nullable
- error_code VARCHAR(96) nullable
- error_message LONGTEXT nullable
- started_at DATETIME(6) nullable
- completed_at DATETIME(6) nullable
- created_at DATETIME(6) NOT NULL
```

Não serializar JSON dentro de outro JSON. Payloads estruturados devem permanecer como documentos JSON simples e validados.

---

## 4. Grupos de preflight

## 4.1 `UPSTREAM_QUALITY`

Gates mínimos:

```text
NICHE_ARTIFACT_APPROVED
HYPOTHESIS_ARTIFACT_APPROVED
UPSTREAM_SOURCE_QUALITY_ACCEPTABLE
UPSTREAM_CLAIM_LINEAGE_AVAILABLE
PERSONA_MINIMUM_COMPLETE
DRPO_FRAMEWORK_COMPLETE
```

Bloqueios:

- artefato upstream reprovado;
- claims sem evidência;
- fonte insegura/irrelevante;
- persona placeholder;
- ausência de dor, resultado, mecanismo, prova ou oferta.

## 4.2 `EXPERIMENT_DESIGN`

```text
BUSINESS_QUESTION_DEFINED
PRIMARY_VARIABLE_DEFINED
PRIMARY_METRIC_DEFINED
CONTROLLED_VARIABLES_DEFINED
ENTRY_ROUTE_DEFINED
OFFER_ASSIGNED
PRICE_DEFINED_WHEN_PAID
STOP_POLICY_DEFINED
DECISION_WINDOW_DEFINED
EXPECTED_COST_DEFINED
```

Valores como métrica nula ou KPI igual a zero devem aparecer como `FAIL`, não como configuração válida silenciosa.

## 4.3 `ASSET_QUALITY`

```text
CREATIVE_APPROVED
CREATIVE_TEXT_NOT_TRUNCATED
MESSAGE_MATCH_VALID
LANDING_APPROVED
LANDING_QUALITY_REVIEW_APPROVED
FORM_SCHEMA_VALID
PRIVACY_POLICY_AVAILABLE
SAMPLE_DELIVERABLE_AVAILABLE
CHECKOUT_CONFIGURATION_VALID
```

Gates não aplicáveis dependem da estratégia. Venda direta sem amostra recebe `NOT_APPLICABLE` em `SAMPLE_DELIVERABLE_AVAILABLE`.

## 4.4 `FUNCTIONAL_E2E`

Executado em `mode=TEST`:

```text
DESTINATION_REACHABLE
FORM_CAN_BE_SUBMITTED
TEST_LEAD_CREATED
FUNNEL_EVENT_RECORDED
MAGIC_LINK_DELIVERED
LEAD_PORTAL_ACCESSIBLE
SAMPLE_GENERATED_OR_DELIVERED
CHECKOUT_ACCESSIBLE
PAYMENT_SANDBOX_RECONCILED
```

Cada gate precisa de evidência persistida:

- evento de teste;
- ID da submissão;
- ID do e-mail;
- URL mascarada;
- ID do pacote;
- resultado do provider.

Eventos de teste devem conter `runMode=TEST` e nunca entrar em métricas comerciais.

## 4.5 `META_PUBLICATION`

```text
META_PAGE_AVAILABLE
META_IDENTITY_AVAILABLE
META_CAMPAIGN_CREATED
META_ADSET_CREATED
META_CREATIVE_CREATED
META_AD_CREATED
META_IMAGE_HASH_RESOLVED
META_TARGETING_ACCEPTED
META_REACH_VALIDATED
META_EFFECTIVE_STATUS_CONFIRMED
```

Publicação concluída não é apenas resposta 200. O backend deve receber os IDs, persistir o estado e confirmar o status efetivo retornado pela Meta.

## 4.6 `MEASUREMENT`

```text
ATTRIBUTION_PARAMETERS_PRESENT
PIXEL_OR_SERVER_EVENTS_READY_WHEN_REQUIRED
COMMERCIAL_EVENT_INGESTION_READY
SPEND_SYNC_READY
PAYMENT_RECONCILIATION_READY
DEDUPLICATION_READY
DATA_FRESHNESS_VALID
```

---

## 5. Máquina de estados

```text
DRAFT
  → PREFLIGHT_PENDING
  → PREFLIGHT_RUNNING
      ├── PREFLIGHT_FAILED
      └── READY_TO_PUBLISH
            → PUBLICATION_PENDING
            → PUBLISHING
                ├── FAILED
                └── PUBLISHED_AWAITING_EXPOSURE
                      ├── FAILED
                      └── RUNNING
                            ├── PAUSE_REQUESTED → PAUSED
                            ├── STOP_REQUESTED → COMPLETED
                            └── FAILED
```

Regras:

1. frontend nunca altera status diretamente;
2. backend valida transições;
3. worker reporta passos, mas não escolhe próximo estado;
4. `RUNNING` exige `first_verified_impression_at` ou evidência equivalente para a rota;
5. janela comercial começa somente em `commercial_window_started_at`;
6. falha após exposição real pode preservar parte da evidência, mas o backend recalcula validade;
7. status técnico e validade comercial são dimensões independentes.

---

## 6. Validade da evidência

## 6.1 Regras determinísticas iniciais

### `TECHNICALLY_INVALID`

Aplicar quando:

- publicação não concluiu;
- destino indisponível;
- formulário não passou no teste E2E;
- integração necessária falhou;
- campanha não teve exposição verificada por motivo técnico.

### `MEASUREMENT_INVALID`

Aplicar quando:

- eventos essenciais ausentes;
- duplicidade não controlada;
- gasto ou conversões não reconciliáveis;
- janela comercial não pode ser isolada;
- tráfego de teste foi misturado ao de produção.

### `STRATEGICALLY_INVALID`

Aplicar quando:

- variável primária ausente;
- várias variáveis principais mudaram sem desenho declarado;
- estratégia incompatível com ativos publicados;
- upstream reprovado;
- público publicado não corresponde ao público planejado.

### `INSUFFICIENT_DATA`

Aplicar quando execução e mensuração são válidas, mas:

- janela não terminou;
- amostra abaixo do mínimo;
- conversões insuficientes;
- diferença abaixo do limiar de decisão.

### `COMMERCIALLY_VALID`

Aplicar quando:

- preflight obrigatório passou;
- exposição foi confirmada;
- janela comercial é identificável;
- mensuração é íntegra;
- estratégia publicada corresponde ao desenho;
- dados podem apoiar uma conclusão, mesmo que inconclusiva.

`COMMERCIALLY_VALID` significa evidência confiável, não resultado positivo.

---

## 7. Orquestração e módulos

## 7.1 Backend

Pacote proposto:

```text
com.marketinghub.experiment.run
com.marketinghub.experiment.run.preflight
com.marketinghub.experiment.run.publication
com.marketinghub.experiment.run.validity
com.marketinghub.experiment.run.web
```

Responsabilidades:

- persistir run;
- avaliar gates determinísticos;
- publicar pendências para executores;
- receber resultados;
- controlar transições;
- calcular validade;
- montar read model do frontend.

## 7.2 Facebook Ads Worker

Responsável por:

- consumir pendência de publicação;
- executar chamadas Meta;
- registrar cada passo por `jobId`;
- retornar IDs, status e payload normalizado;
- não atualizar `ExperimentRunStatus` diretamente.

## 7.3 Lead Portal

Responsável por executar prova funcional pública solicitada pelo backend, sem decidir aprovação do gate.

## 7.4 Email Service

Responsável por enviar e reportar entrega/teste transacional através do backend.

## 7.5 Payments Service

Responsável por teste de checkout/reconciliação quando aplicável, sempre por contrato do backend.

---

## 8. APIs administrativas propostas

```text
POST /api/experiments/{experimentId}/runs
GET  /api/experiments/{experimentId}/runs
GET  /api/experiment-runs/{runId}
GET  /api/experiment-runs/{runId}/timeline
GET  /api/experiment-runs/{runId}/preflight
POST /api/experiment-runs/{runId}/preflight
POST /api/experiment-runs/{runId}/publish
POST /api/experiment-runs/{runId}/pause
POST /api/experiment-runs/{runId}/resume
POST /api/experiment-runs/{runId}/stop
GET  /api/experiment-runs/{runId}/validity
POST /api/experiment-runs/{runId}/human-gate-decisions
```

Comandos devem ser idempotentes. Repetir `publish` com job ativo retorna o job atual.

## 8.1 Endpoints internos propostos

```text
GET   /api/internal/experiment-runs/v1/meta-publication/stage-executions/pending
PATCH /api/internal/experiment-runs/v1/meta-publication/stage-executions/{jobId}

GET   /api/internal/experiment-runs/v1/functional-check/stage-executions/pending
PATCH /api/internal/experiment-runs/v1/functional-check/stage-executions/{jobId}
```

O desenho final deve respeitar o controller do contexto proprietário e atualizar Swagger.

---

## 9. Read model para frontend

```json
{
  "runId": 123,
  "runNumber": 2,
  "mode": "PRODUCTION",
  "status": "PREFLIGHT_FAILED",
  "evidenceValidity": "TECHNICALLY_INVALID",
  "dataQualityStatus": "BLOCKED",
  "summary": {
    "title": "Execução bloqueada antes da publicação",
    "reason": "O formulário não possui submissão de teste válida",
    "nextActionCode": "RUN_FUNCTIONAL_PREFLIGHT"
  },
  "gateGroups": [],
  "timeline": [],
  "publication": {},
  "measurement": {},
  "allowedCommands": []
}
```

O backend deve produzir textos objetivos e códigos estruturados. O frontend não infere causa-raiz combinando flags soltas.

---

## 10. Frontend mínimo da primeira entrega

Na visão do experimento:

### Card `Execução atual`

Mostrar:

- run;
- modo;
- status;
- validade;
- início da janela comercial;
- exposição confirmada;
- bloqueio principal;
- próximo comando.

### Aba `Preparação e execução`

Seções:

1. estratégia e desenho;
2. qualidade dos ativos;
3. teste ponta a ponta;
4. publicação Meta;
5. mensuração;
6. linha do tempo;
7. histórico de runs.

Cores e badges não podem ser o único meio de comunicar status.

---

## 11. Migração

1. criar tabelas;
2. criar um run legado para experimentos ativos/concluídos;
3. marcar validade inicial como `NOT_EVALUATED`;
4. inferir somente dados objetivos;
5. não converter `INVALIDATED` automaticamente em `COMMERCIAL_HYPOTHESIS_FAILURE`;
6. manter status do experimento por compatibilidade;
7. migrar gradualmente os comandos de release/reset para run;
8. depois de estabilizar, derivar status agregado do experimento a partir dos runs.

Run legado:

```text
mode = PRODUCTION
status = COMPLETED ou FAILED conforme fatos objetivos
evidenceValidity = NOT_EVALUATED
failureClassification = null
```

---

## 12. Testes obrigatórios

### Backend

- criação sequencial de run;
- transições válidas e inválidas;
- idempotência de comando;
- avaliação de cada gate;
- validade por falha técnica;
- validade por medição;
- insuficiência de dados;
- separação TEST/PRODUCTION;
- concorrência em publicação;
- migração de legado.

### Integração

- formulário de teste aprovado;
- formulário quebrado bloqueia produção;
- publicação Meta parcial;
- campanha criada sem impressão;
- primeira impressão inicia janela;
- reset não apaga histórico do run;
- retry técnico preserva run/job;
- nova tentativa após correção cria run novo.

### Frontend

- estado sem run;
- preflight em andamento;
- bloqueio com remediação;
- publicação falha;
- execução válida;
- histórico de runs;
- comandos permitidos por status.

---

## 13. Sequência de PRs

### PR 1 — persistência e estados

- tabelas;
- enums;
- entidade;
- service;
- API de leitura/criação;
- migração inicial.

### PR 2 — gates determinísticos

- grupos;
- avaliadores;
- readiness;
- testes.

### PR 3 — frontend de run/preflight

- card;
- aba;
- timeline;
- comandos sem publicação real.

### PR 4 — publicação vinculada ao run

- contrato interno;
- passos por job;
- callback;
- confirmação de exposição.

### PR 5 — E2E funcional

- eventos de teste;
- verificação de formulário/e-mail/portal;
- exclusão das métricas comerciais.

### PR 6 — cálculo de validade

- engine determinística;
- classificação;
- relatório final.

---

## 14. Critérios de aceite

- [ ] uma correção técnica não exige duplicar o experimento;
- [ ] um run tecnicamente inválido não reprova o mercado;
- [ ] eventos de teste ficam separados;
- [ ] produção exige preflight aprovado;
- [ ] publicação exige confirmação operacional;
- [ ] janela comercial começa na primeira exposição verificada;
- [ ] usuário entende o bloqueio pela tela;
- [ ] logs não são necessários para leitura operacional comum;
- [ ] workers não controlam transições;
- [ ] backend persiste todos os passos necessários para relatório;
- [ ] apenas runs comercialmente válidos entram na decisão por IA.
