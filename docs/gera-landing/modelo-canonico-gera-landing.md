# Modelo Canônico — Módulo Gera Landing

## Objetivo

Este documento define, de forma canônica, duas dimensões do módulo **Gera Landing**:

1. **Modelo de dados canônico** (estrutura persistida no banco);
2. **Fluxos canônicos** (comportamentos operacionais do módulo).

> Nesta versão, o documento registra o **primeiro fluxo canônico** do módulo: geração de prompts pelo Worker AI.

---

## PARTE A — MODELO DE DADOS CANÔNICO

### Tabela de controle de jobs

### Nome da tabela

- `gera_landing_stage_execution`

### Finalidade

Registrar cada execução de etapa do fluxo Gera Landing, com:

- identificação do experimento;
- identificação da etapa;
- marca temporal da solicitação;
- dados de prompt de entrada e prompt final montado;
- status e identificador do job;
- timestamps de ciclo de vida da execução.

### Definição canônica dos campos (aderente ao código e Liquibase atual)

| Campo | Tipo (MySQL 5.7) | Obrigatório | Regra canônica |
|---|---|---:|---|
| `experiment_id` | `BIGINT` | Sim | Identificador do experimento. FK para `experiment.id`. |
| `stage_code` | `VARCHAR(100)` | Sim | Código da etapa do Gera Landing (ex.: `landing-page-wireframe`). |
| `execution_requested_at` | `DATETIME(3)` | Sim | Momento da solicitação da execução da etapa. Default `CURRENT_TIMESTAMP(3)`. |
| `prompt_template_id` | `VARCHAR(191)` | Não | Identificador técnico do template/origem do prompt inicial da execução. |
| `prompt_content` | `LONGTEXT` | Sim | Conteúdo de prompt recebido no início da execução da etapa. |
| `status` | `VARCHAR(50)` | Sim | Estado atual da execução. Valor inicial: `INICIADO`. |
| `id_job` | `CHAR(36)` | Sim | Identificador do job em formato textual UUID. É a chave primária atual. |
| `created_at` | `DATETIME(3)` | Sim | Momento de criação do registro no banco. Default `CURRENT_TIMESTAMP(3)`. |
| `processing_started_at` | `DATETIME(3)` | Não | Momento em que o processamento efetivo da execução iniciou. |
| `completed_at` | `DATETIME(3)` | Não | Momento de conclusão da execução da etapa. |
| `prompt` | `LONGTEXT` | Não | Prompt final montado pelo Worker AI para a execução da etapa. |

### Chaves e restrições

- **Chave primária**: `id_job`.
- **Chave estrangeira**: `experiment_id` referencia `experiment.id` com `ON DELETE CASCADE`.
- **Índice auxiliar**: `idx_gera_landing_stage_execution_experiment` em `experiment_id`.

### Validação da PARTE A contra implementação atual

Resultado da verificação: **PARTE A foi ajustada para refletir exatamente o estado atual do código e do schema**.

Principais confirmações:

- `prompt_content` e `prompt` estão em `LONGTEXT` no schema vigente.
- `id_job` está em `CHAR(36)` no schema, mesmo com mapeamento JPA em `byte[]` (conversão UTF-8 no serviço).
- chave primária ativa é `id_job`.
- pendência oficial continua sendo `status = INICIADO`.

---

## PARTE B — FLUXOS CANÔNICOS DO MÓDULO

### Catálogo de fluxos desta versão

- **Fluxo 01 — Geração de prompts pelo Worker AI (geralanding)**: detalhado abaixo.

### Fluxo 01 — geração de prompts pelo Worker AI

> Este fluxo descreve o caminho oficial e atual do processamento assíncrono do Gera Landing, da busca de pendências até a persistência do prompt final no backend.

### 1) Criação do job no backend com status inicial

1. O backend registra execução inicial da etapa `landing-page-wireframe` em `gera_landing_stage_execution`.
2. O registro nasce com:
   - `status = INICIADO`;
   - `id_job` gerado por UUID;
   - `execution_requested_at` e `created_at` preenchidos com timestamp atual;
   - `prompt_template_id = manual/start`;
   - `prompt_content = "Início manual via interface do experimento."`.
3. Este status inicial é a condição de elegibilidade para o polling do Worker AI.

### 2) Agendamento do Worker AI (polling contínuo)

1. O Worker AI executa um agendamento com cron padrão `0 */1 * * * *` (a cada 1 minuto).
2. Em cada ciclo, chama `processPendingExecutions()`.
3. O tamanho do lote é controlado por `geralanding.execution.pending-limit` (default `20`, com mínimo efetivo `1`).

### 3) Busca de jobs pendentes no endpoint interno do backend

1. O Worker AI chama `GET /api/internal/geralanding/stage-executions/pending?limit={n}`.
2. O backend retorna lista com payload por item: `experimentId`, `idJob`, `stageCode`.
3. O backend monta essa lista buscando somente registros com `status = INICIADO`, ordenados por `execution_requested_at` ascendente.
4. Observação importante de implementação: o parâmetro `limit` enviado pelo Worker não é aplicado no serviço backend atual, que usa `findTop20ByStatusOrderByExecutionRequestedAtAsc("INICIADO")`.

### 4) Loop de processamento no Worker AI

Para cada item retornado no lote:

1. O Worker valida presença de `stageCode` e `idJob`; itens inválidos são ignorados com log de aviso.
2. O `stageCode` é normalizado (`trim + lowerCase`).
3. Nesta versão canônica, apenas `landing-page-wireframe` entra no fluxo de geração; outras etapas são ignoradas sem erro.
4. O contexto é montado como `GeraLandingPromptContext(experimentId, idJob, stageCode, Collections.emptyMap())`.

### 5) Montagem do prompt da etapa no Worker AI

1. O Worker chama `GeraLandingService.montarERegistrarPromptEtapa(context, etapa)`.
2. A montagem usa:
   - template base em `prompts/geralanding/{etapa}.md`;
   - resolução de placeholders `{prompt-*}` com inclusão recursiva de outros prompts;
   - resolução de placeholders `{dados-*}` via serialização JSON pretty do mapa `dados` do contexto.
3. Proteção aplicada: referência circular entre prompts gera erro explícito (`IllegalStateException`).
4. Como o fluxo atual cria `context.dados` vazio, placeholders `{dados-*}` resultam em string vazia na versão corrente.

### 6) Envio do prompt montado para persistência no backend

1. Após montar o prompt, o Worker chama `POST /api/internal/geralanding/stage-executions/{idJob}/receive-prompt`.
2. O `idJob` do path **é exatamente o mesmo** recebido no item de pendência retornado por `GET /pending`.
3. Body enviado pelo Worker:
   - `experimentId`
   - `stageCode`
   - `prompt`
4. O contrato do endpoint exige os três campos (`@NotNull` / `@NotBlank`).

### 7) Persistência final no backend e atualização de status

1. O backend tenta localizar a execução por `idJob` usando `findTopByIdJobOrderByExecutionRequestedAtDesc`.
2. Se não encontrar, aplica fallback por `experimentId + stageCode` (`findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc`).
3. Em caso de ausência em ambos os critérios, retorna erro de entidade não encontrada e registra log de diagnóstico.
4. Em caso de sucesso, persiste:
   - `prompt` com o texto final montado;
   - `processing_started_at` com timestamp atual;
   - `status = EM_PROCESSAMENTO`.
5. O endpoint responde `202 Accepted` ao Worker.

## Escopo desta versão

Este documento cobre:

- **PARTE A (Modelo)**: modelo canônico da tabela `gera_landing_stage_execution` validado contra Liquibase e backend;
- **PARTE B (Fluxos)**: fluxo 01 de geração/persistência de prompts pelo Worker AI, com comportamento real atual (incluindo limitações já observadas em código).

Evoluções de novas etapas do Gera Landing e novos estados do ciclo devem ser versionadas neste mesmo cânone.
