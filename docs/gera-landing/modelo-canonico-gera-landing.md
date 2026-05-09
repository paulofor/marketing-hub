# Modelo Canônico — Módulo Gera Landing (Wireframe)

## Objetivo

Este documento descreve com **nível operacional completo** o fluxo implementado de Gera Landing Wireframe, usando o código atual do repositório como fonte de verdade (backend `ads-service` + `ai-worker`).

Ele cobre:

1. Contratos e endpoints públicos/internos.
2. Máquina de estados real (incluindo transições intermediárias e falhas).
3. Montagem de prompt, resolução de placeholders e envelope final.
4. Montagem de payload OpenAI Responses API + execução batch.
5. Persistência, auditoria e rastreabilidade fim a fim.
6. Regras de fallback, idempotência operacional e limitações atuais.

---

## 1) Visão de arquitetura executável

### 1.1 Componentes do fluxo

- **Backend (`com.marketinghub.geralanding`)**
  - Cria e persiste execuções.
  - Expõe endpoint de start para a UI administrativa.
  - Entrega pendências para o worker.
  - Recebe prompt montado, dispatch OpenAI e resultado final.
  - Mantém trilha de auditoria completa.

- **AI Worker (`com.marketinghub.worker.geralanding`)**
  - Faz polling de pendências.
  - Monta prompt de wireframe com dados do experimento.
  - Monta request da Responses API com `json_schema strict`.
  - Executa ciclo batch (upload JSONL → create batch → polling → download output).
  - Devolve status de dispatch e resultado (ou falha) para o backend.

### 1.2 Etapa suportada oficialmente no código atual

- `landing-page-wireframe`.

Qualquer etapa fora desse valor é ignorada pelo worker com log informativo.

---

## 2) Modelo de dados canônico (`gera_landing_stage_execution`)

## 2.1 Finalidade

Registrar o ciclo de vida completo de uma execução, incluindo:

- solicitação inicial;
- prompt bruto/montado e markdown usado;
- schema e payload OpenAI enviados;
- rastreio de dispatch e job OpenAI;
- resposta de modelo (ou falha), tokens/custo e HTML provisório.

## 2.2 Campos canônicos observados no fluxo

| Campo | Tipo (MySQL 5.7) | Obrigatório | Papel no fluxo |
|---|---|---:|---|
| `id_job` | `BINARY(36)` (trafega como string) | Sim | Identificador da execução ponta a ponta. |
| `experiment_id` | `BIGINT` | Sim | Dono da execução (experimento). |
| `stage_code` | `VARCHAR(100)` | Sim | Etapa lógica (`landing-page-wireframe`). |
| `execution_requested_at` | `DATETIME(3)` | Sim | Momento em que a execução foi criada/solicitada. |
| `created_at` | `DATETIME(3)` | Sim | Criação do registro de execução. |
| `processing_started_at` | `DATETIME(3)` | Não | Preenchido quando prompt auditável é recebido no backend. |
| `completed_at` | `DATETIME(3)` | Não | Preenchido em sucesso **ou** falha final. |
| `prompt_template_id` | `VARCHAR(191)` | Não | Identificador técnico da origem do prompt inicial. |
| `prompt_content` | `LONGTEXT` | Sim | Prompt inicial do start/manual ou registro worker. |
| `prompt` | `LONGTEXT` | Não | Prompt final montado no worker (envelope de tarefa + instruções). |
| `openai_request_body` | `LONGTEXT` | Não | Request JSON efetivo para Responses API em batch. |
| `schema_json` | `LONGTEXT` | Não | Schema serializado usado em `text.format.schema`. |
| `openai_model` | `VARCHAR(120)` | Não | Modelo OpenAI efetivamente usado na execução (ex.: `gpt-5.2`). |
| `prompt_markdown_content` | `LONGTEXT` | Não | Conteúdo markdown cru da etapa (`*.md`). |
| `status` | `VARCHAR(50)` | Sim | Estado atual da execução (ver seção 5). |
| `openai_job_id` | `VARCHAR(120)` | Não | ID técnico da resposta/job OpenAI quando disponível. |
| `model_response` | `LONGTEXT` | Não | Conteúdo final retornado pelo modelo. |
| `provisional_html` | `LONGTEXT` | Não | HTML provisório (informado ou derivado do model response). |
| `error_message` | `LONGTEXT` | Não | Motivo textual de falha quando houver. |
| `input_tokens` | `INT` | Não | Tokens de entrada efetivos. |
| `output_tokens` | `INT` | Não | Tokens de saída efetivos. |
| `cost_usd` | `DECIMAL(12,6)` | Não | Custo estimado em USD. |

## 2.3 Chaves, índices e consultas operacionais

- PK: `id_job`.
- FK lógica/JPA para `experiment.id`.
- Consultas críticas:
  - pendentes: `findTop20ByStatusInOrderByExecutionRequestedAtAsc([INICIADO, AGUARDANDO_RETORNO_OPENAI])`;
  - listagem por experimento+etapa (com ou sem concluídos);
  - detalhe por `experimentId + idJob`.

---

## 3) Contratos HTTP canônicos

## 3.1 Endpoints públicos (admin)

- `POST /api/experiments/{experimentId}/geralanding/wireframe/start`
  - cria execução inicial;
  - retorna `202 Accepted` com `{ idJob, status }`.

- `GET /api/experiments/{experimentId}/geralanding/stage-executions`
  - query params:
    - `stageCode` default `landing-page-wireframe`
    - `includeCompleted` default `true`
  - retorna até 20 execuções ordenadas por `executionRequestedAt DESC`.

- `GET /api/experiments/{experimentId}/geralanding/stage-executions/{idJob}`
  - retorna detalhamento completo de auditoria da execução.

## 3.2 Endpoints internos backend ↔ worker

- `GET /api/internal/geralanding/stage-executions/pending`
  - retorna pendências para consumo do worker.
  - Observação: o worker envia `?limit=...`, mas o backend usa consulta fixa top 20.

- `POST /api/internal/geralanding/stage-executions/{idJob}/receive-prompt`
  - persiste prompt montado + payload/schema + markdown cru.
  - muda estado para `AGUARDANDO_RETORNO_OPENAI`.

- `POST /api/internal/geralanding/stage-executions/{idJob}/receive-dispatch`
  - persiste `openAiJobId` após envio efetivo para OpenAI.
  - muda estado para `EM_PROCESSAMENTO`.

- `POST /api/internal/geralanding/stage-executions/{idJob}/receive-result`
  - persiste resultado final (sucesso/falha), tokens, custo, timestamps e html provisório.

- `POST /api/internal/geralanding/stage-executions`
  - registro alternativo técnico de execução inicial via worker.

- `POST /api/internal/geralanding/stage-executions/receive-prompt`
  - rota body-based de compatibilidade, sem payload completo de auditoria.

---

## 4) Fluxo operacional ponta a ponta

## 4.1 Start manual da etapa

1. UI chama `wireframe/start`.
2. Backend cria execução com:
   - `stageCode = landing-page-wireframe`
   - `promptTemplateId = manual/start`
   - `promptContent = Início manual via interface do experimento.`
   - `status = INICIADO`
   - `idJob = UUID` (persistido como `byte[]` UTF-8).
3. Resposta `202` devolve `idJob` e `status`.

## 4.2 Polling e filtro de elegibilidade no worker

1. Scheduler chama `processPendingExecutions()` por cron.
2. Se `OPENAI_API_KEY` ausente: worker não processa e apenas loga warning.
3. Worker busca pendências no backend.
4. Para cada item:
   - valida `stageCode` e `idJob` não vazios;
   - normaliza etapa (`trim + lowerCase`);
   - processa somente `landing-page-wireframe`.

## 4.3 Montagem do contexto de dados

O worker carrega dados do experimento e materializa chaves como:

- `campaignAngle`
- `adCopy`
- `adImageBriefing`
- `NICHE_NAME`
- `PAIN_JSON` (quando disponível em hypothesis.framework.pain)
- `RESULT_JSON` (quando disponível em hypothesis.framework.result)

Se chamadas auxiliares falharem, defaults vazios são usados para manter robustez.

## 4.4 Montagem de prompt

- Template principal: `prompts/geralanding/landing-page-wireframe.md`.
- Placeholders suportados:
  - `{prompt-xxx}`: inclui recursivamente `xxx.md`.
  - `{dados-xxx}`: injeta valor de `dadosPayload[xxx]`.
  - `{{xxx}}` (mustache simples): injeta `dadosPayload[xxx]`.
- Regras:
  - detecta referência circular em `{prompt-*}` e lança erro;
  - valor ausente vira string vazia;
  - objetos viram JSON pretty.

Prompt final é encapsulado em:

- bloco `# Tarefa`
- bloco `# Instruções do usuário`

## 4.5 Montagem do payload OpenAI

Request montado com:

- `model = gpt-5.2`
- `input`:
  - `system` com prefixo `[gera-landing-pipeline]`
  - `user` com `input_text` contendo o prompt montado
- `text.format`:
  - `type = json_schema`
  - `name = experiment_pipeline_landing_page_copy`
  - `schema = landing-page-wireframe-schema.json`
  - `strict = true`

O JSON final vai para `openai_request_body` (auditoria).

## 4.6 Handoff de prompt para backend

Antes de executar batch, worker chama `receive-prompt` com:

- `experimentId`
- `stageCode`
- `prompt`
- `openAiRequestBody`
- `openAiModel`
- `schemaJson`
- `promptMarkdownContent`

Backend:

1. tenta localizar por `idJob`;
2. fallback por `experimentId + stageCode`;
3. persiste dados e seta:
   - `processing_started_at = now`
   - `status = AGUARDANDO_RETORNO_OPENAI`.

## 4.7 Execução OpenAI em batch

Fluxo técnico no client batch:

1. cria JSONL com uma linha (`custom_id = idJob`) para `/v1/responses`;
2. upload em `/files` com `purpose=batch`;
3. cria batch em `/batches` (`completion_window=24h`);
4. polling até `completed` (respeitando timeout/poll interval configurados);
5. download do output por `output_file_id`;
6. parse da primeira linha JSONL (`response.body`);
7. extrai texto final, tokens e custo estimado.

Status `failed`, `expired` ou `cancelled` encerram em erro.

## 4.8 Dispatch e resultado

- Quando há `openAiJobId`, worker chama `receive-dispatch` e backend muda para `EM_PROCESSAMENTO`.
- Em seguida worker chama `receive-result` com conteúdo gerado/tokens/custo.

Backend em `receiveResult`:

- persiste `model_response`;
- calcula `provisional_html` (usa payload se veio, senão monta via assembler);
- persiste `error_message` (quando houver);
- persiste `openai_job_id` (quando informado);
- persiste `openai_model` (quando informado no handoff do prompt);
- persiste `input_tokens`, `output_tokens`, `cost_usd`;
- seta `completed_at = now`;
- seta status:
  - `FALHA` se `error_message` preenchido;
  - `CONCLUIDO` caso contrário.

Além disso, para etapa `landing-page-wireframe`, sem erro e com `modelResponse` válido:

- backend atualiza o experimento:
  - `landingPageWireframe = modelResponse`
  - `landingPageWireframeJobId = execution.idJob`.

---

## 5) Máquina de estados canônica (estado real implementado)

1. **INICIADO**
   - execução recém-criada.

2. **AGUARDANDO_RETORNO_OPENAI**
   - prompt/schema/request já auditados no backend.
   - pendência ainda pode ser reobtida no polling.

3. **EM_PROCESSAMENTO**
   - dispatch da OpenAI confirmado (há `openAiJobId`).

4. **CONCLUIDO**
   - resposta final persistida sem `errorMessage`.

5. **FALHA**
   - erro final persistido em `error_message`.

---

## 6) Regras de rastreabilidade e auditoria

Cada execução pode ser reconstituída de ponta a ponta com:

- template markdown cru da etapa;
- prompt final entregue ao modelo;
- payload OpenAI serializado;
- schema usado no `json_schema strict`;
- `openAiJobId` + resposta final;
- métricas de custo/tokens;
- timestamps de início/fim;
- `error_message` quando aplicável.

---

## 7) Observações de implementação (importantes para evolução)

- `id_job` é gravado como bytes UTF-8 (`byte[]`) e exposto como string nas APIs.
- O worker envia `limit` no endpoint de pendências, porém o backend devolve top 20 fixo.
- Existe dupla chamada de registro de prompt: uma interna no `GeraLandingService` (payload parcial) e outra explícita no `GeraLandingExecutionService` (payload completo); o estado final observado é o da chamada completa posterior.
- O fluxo atual usa modelo fixo `gpt-5.2` no código.

---

## 8) Fonte de verdade desta versão

Documento consolidado a partir dos pacotes:

- `backend/ads-service/src/main/java/com/marketinghub/geralanding`
- `ai-worker/src/main/java/com/marketinghub/worker/geralanding`
- `ai-worker/src/main/resources/prompts/geralanding`

Toda mudança de contrato, estados, payload, auditoria ou persistência deve refletir imediatamente neste arquivo para manter caráter canônico.
