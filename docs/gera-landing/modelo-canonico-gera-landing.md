# Modelo Canônico — Módulo Gera Landing

## Objetivo

Este documento descreve, no nível operacional e de contrato, o fluxo **implementado** do módulo Gera Landing, tomando o código atual (backend + ai-worker) como fonte de verdade.

Escopo desta versão:

1. Jornada completa da solicitação do usuário até a conclusão da geração de wireframe.
2. Montagem de prompt, schema, payload OpenAI e execução em modo batch.
3. Recuperação de resultado e persistência no backend.
4. Modelo de dados da tabela `gera_landing_stage_execution` com campos de rastreabilidade ponta-a-ponta.

---

## 1) Modelo de dados canônico (`gera_landing_stage_execution`)

### 1.1 Finalidade

A tabela registra o ciclo de vida completo de uma execução de etapa do Gera Landing (hoje: `landing-page-wireframe`), incluindo:

- solicitação inicial;
- prompt final montado;
- payload OpenAI efetivamente enviado;
- schema aplicado;
- identificador do job OpenAI;
- resposta final do modelo e métricas (tokens/custo);
- timestamps de início e término do processamento.

### 1.2 Campos canônicos

| Campo | Tipo (MySQL 5.7) | Obrigatório | Origem/uso no fluxo |
|---|---|---:|---|
| `id_job` | `BINARY(36)` físico (mapeado em `byte[]`) | Sim | Chave primária da execução. Exposto na API como texto UUID por conversão UTF-8. |
| `experiment_id` | `BIGINT` | Sim | Experimento associado; FK para `experiment.id`. |
| `stage_code` | `VARCHAR(100)` | Sim | Etapa da execução (`landing-page-wireframe`). |
| `execution_requested_at` | `DATETIME(3)` | Sim | Timestamp da solicitação da etapa. |
| `created_at` | `DATETIME(3)` | Sim | Timestamp de criação do registro. |
| `processing_started_at` | `DATETIME(3)` | Não | Preenchido no recebimento do prompt pelo backend. |
| `completed_at` | `DATETIME(3)` | Não | Preenchido no recebimento do resultado final do modelo. |
| `prompt_template_id` | `VARCHAR(191)` | Não | Origem técnica do prompt inicial (`manual/start` no início manual). |
| `prompt_content` | `LONGTEXT` | Sim | Prompt base da solicitação inicial. |
| `prompt` | `LONGTEXT` | Não | Prompt final montado pelo worker (já com placeholders resolvidos e envelope de tarefa). |
| `openai_request_body` | `LONGTEXT` | Não | JSON do request da Responses API enviado em batch. |
| `schema_json` | `LONGTEXT` | Não | Schema JSON usado em `text.format` (json_schema strict). |
| `prompt_markdown_content` | `LONGTEXT` | Não | Conteúdo markdown cru do template da etapa (`prompts/geralanding/*.md`). |
| `status` | `VARCHAR(50)` | Sim | Estados atuais: `INICIADO` → `EM_PROCESSAMENTO` → `CONCLUIDO`. |
| `openai_job_id` | `VARCHAR(120)` | Não | ID de resposta/job da OpenAI retornado ao fim do batch. |
| `model_response` | `LONGTEXT` | Não | Conteúdo final gerado pelo modelo (`responseContent`). |
| `input_tokens` | `INT` | Não | Tokens de entrada efetivos do uso OpenAI. |
| `output_tokens` | `INT` | Não | Tokens de saída efetivos do uso OpenAI. |
| `cost_usd` | `DECIMAL(12,6)` | Não | Estimativa de custo USD calculada no worker. |

### 1.3 Índices e chaves

- PK: `id_job`.
- FK: `experiment_id -> experiment.id` (com relacionamento JPA `ManyToOne`).
- Consultas operacionais usam:
  - `findTop20ByStatusOrderByExecutionRequestedAtAsc("INICIADO")` para pendências;
  - lookup prioritário por `id_job`, com fallback por `experiment_id + stage_code`.

---

## 2) Arquitetura operacional do fluxo wireframe

## 2.1 Entrada do usuário (frontend/admin)

1. Usuário aciona **Iniciar** no card de Gera Landing (wireframe).
2. Backend recebe `POST /api/experiments/{experimentId}/geralanding/wireframe/start`.
3. Serviço cria execução inicial com:
   - `stageCode = "landing-page-wireframe"`
   - `promptTemplateId = "manual/start"`
   - `promptContent = "Início manual via interface do experimento."`
   - `status = "INICIADO"`
   - `idJob = UUID`.
4. Retorno: `202 Accepted` com `{ idJob, status }`.

## 2.2 Polling do worker para capturar pendências

1. Scheduler do ai-worker roda por cron (`geralanding.execution.fixed-cron`, default a cada 1 minuto).
2. `GeraLandingExecutionService.processPendingExecutions()`:
   - valida se OpenAI está habilitada (`OPENAI_API_KEY`);
   - chama backend para pendências.
3. Endpoint consultado: `GET /api/internal/geralanding/stage-executions/pending`.
4. Backend retorna lista mínima de pendências (`experimentId`, `idJob`, `stageCode`) com status `INICIADO`.

## 2.3 Seleção e validação da etapa no worker

Para cada item pendente:

- valida presença de `stageCode` e `idJob`;
- normaliza `stageCode` para lower-case/trim;
- processa apenas `landing-page-wireframe` (demais etapas são ignoradas com log).

---

## 3) Montagem detalhada de prompt no ai-worker

## 3.1 Fonte de template

- Template principal: `classpath:prompts/geralanding/{etapa}.md`.
- Exemplo da etapa atual: `landing-page-wireframe.md`.

## 3.2 Resolução de placeholders

Motor de resolução em `GeraLandingService` suporta:

- `{prompt-xxx}`: inclui outro markdown da pasta `prompts/geralanding/xxx.md` (resolução recursiva).
- `{dados-xxx}`: injeta JSON pretty de `context.dados().get("xxx")`.

Regras implementadas:

- detecção de referência circular entre prompts (lança `IllegalStateException`);
- se dado não existir, substitui por string vazia.

Observação importante do estado atual:

- o contexto hoje é criado com `Collections.emptyMap()`; portanto, placeholders `{dados-*}` tendem a ficar vazios no fluxo wireframe atual.

## 3.3 Envelope final do prompt de usuário

Após resolver o markdown, o worker encapsula no formato:

- seção `# Tarefa` instruindo a executar a etapa;
- seção `# Instruções do usuário` com o prompt resolvido.

Esse texto vira o `prompt` canônico da execução.

---

## 4) Montagem do schema e payload OpenAI

## 4.1 Schema de saída

- O worker carrega `classpath:prompts/geralanding/landing-page-wireframe-schema.json`.
- O schema é usado de duas formas:
  1. inserido no body OpenAI (`text.format.schema`);
  2. serializado e enviado ao backend em `schemaJson` para auditoria.

## 4.2 Payload OpenAI (Responses API)

O worker monta um JSON com:

- `model`: atualmente `gpt-5.2`;
- `input`:
  - role `system` com instrução de especialista em pipeline;
  - role `user` com conteúdo do prompt final (tipo `input_text`);
- `text.format`:
  - `type: "json_schema"`
  - `name: "experiment_pipeline_landing_page_copy"` (nome técnico atual implementado)
  - `schema`: conteúdo do schema de wireframe
  - `strict: true`.

O body completo é persistido no backend em `openai_request_body`.

---

## 5) Handoff Worker → Backend antes da geração

Antes de chamar a OpenAI, o worker executa:

`POST /api/internal/geralanding/stage-executions/{idJob}/receive-prompt`

Body enviado:

- `experimentId`
- `stageCode`
- `prompt`
- `openAiRequestBody`
- `schemaJson`
- `promptMarkdownContent`

No backend, `receivePrompt(...)`:

1. busca execução por `idJob` (fallback por `experimentId + stageCode`);
2. persiste os campos de prompt/auditoria;
3. define:
   - `processing_started_at = now`
   - `status = "EM_PROCESSAMENTO"`.

---

## 6) Execução OpenAI em modo batch (implementação atual)

## 6.1 Estratégia batch

`GeraLandingOpenAiBatchClient.generate(...)` executa ciclo completo:

1. monta **uma linha JSONL** com request para `/v1/responses` (`custom_id = idJob`);
2. upload em `/files` com `purpose=batch`;
3. cria batch em `/batches` (`endpoint=/v1/responses`, `completion_window=24h`);
4. polling de status em `/batches/{id}` até `completed` (timeout configurável);
5. baixa output em `/files/{output_file_id}/content`;
6. parse da primeira linha JSONL para extrair `response.body`;
7. extrai texto final (`firstText()`), uso de tokens e custo estimado.

Status terminais inválidos (`failed`, `expired`, `cancelled`) geram erro.

## 6.2 Resultado consolidado no worker

O worker monta `GeraLandingJobCompletionPayload` com:

- `responseContent` (resposta final);
- `rawResponse` (saída bruta JSONL);
- `openAiJobId`;
- `inputTokens`, `outputTokens`;
- `costUsd` estimado.

---

## 7) Handoff Worker → Backend de resultado final

Após sucesso no batch:

`POST /api/internal/geralanding/stage-executions/{idJob}/receive-result`

Body enviado:

- `experimentId`
- `stageCode`
- `modelResponse`
- `inputTokens`
- `outputTokens`
- `costUsd`
- `openAiJobId`

Backend `receiveResult(...)` persiste:

- `model_response`
- `openai_job_id` (quando informado)
- `input_tokens`
- `output_tokens`
- `cost_usd`
- `completed_at = now`
- `status = "CONCLUIDO"`.

---

## 8) Endpoints canônicos do fluxo wireframe

## 8.1 Públicos (admin/backend)

- `POST /api/experiments/{experimentId}/geralanding/wireframe/start`
- `GET /api/experiments/{experimentId}/geralanding/stage-executions`
- `GET /api/experiments/{experimentId}/geralanding/stage-executions/{idJob}`

## 8.2 Internos backend↔worker

- `GET /api/internal/geralanding/stage-executions/pending`
- `POST /api/internal/geralanding/stage-executions/{idJob}/receive-prompt`
- `POST /api/internal/geralanding/stage-executions/{idJob}/receive-result`
- `POST /api/internal/geralanding/stage-executions` (registro técnico alternativo via worker)
- `POST /api/internal/geralanding/stage-executions/receive-prompt` (rota body-based mantida para compatibilidade)

---

## 9) Máquina de estados canônica (estado atual)

1. **INICIADO**
   - criado no start manual;
   - elegível para polling.

2. **EM_PROCESSAMENTO**
   - prompt recebido e persistido;
   - payload/schema registrados.

3. **CONCLUIDO**
   - resultado OpenAI persistido;
   - tokens/custo registrados;
   - `completed_at` preenchido.

---

## 10) Observações de aderência ao código

- O `id_job` é armazenado no banco como binário (`byte[]`), mas trafega na API como string UUID por conversão UTF-8 no serviço.
- O endpoint de pendências atualmente não aplica parâmetro `limit` no backend; usa consulta fixa top 20.
- A execução atual trata apenas `landing-page-wireframe` no worker.
- A auditoria persiste simultaneamente o prompt renderizado, markdown original, schema e request OpenAI, permitindo reconstituição completa da geração.

---

## 11) Fonte de verdade desta versão

Este documento foi atualizado a partir da implementação vigente em:

- backend (`com.marketinghub.geralanding`)
- ai-worker (`com.marketinghub.worker.geralanding`)
- recursos de prompt/schema em `ai-worker/src/main/resources/prompts/geralanding`.

Qualquer mudança de contrato, estados, payloads ou armazenamento deve atualizar este arquivo imediatamente para manter caráter canônico.
