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

### Definição canônica dos campos

| Campo | Tipo (MySQL 5.7) | Obrigatório | Regra canônica |
|---|---|---:|---|
| `experiment_id` | `BIGINT` | Sim | Identificador do experimento. FK para `experiment.id`. |
| `stage_code` | `VARCHAR(100)` | Sim | Código da etapa do Gera Landing (ex.: `landing-page-wireframe`). |
| `execution_requested_at` | `DATETIME(3)` | Sim | Momento da solicitação da execução da etapa. Compõe chave primária. Default `CURRENT_TIMESTAMP(3)`. |
| `prompt_template_id` | `VARCHAR(191)` | Não | Identificador do template de prompt usado na execução. |
| `prompt_content` | `TINYTEXT` | Sim | Conteúdo de prompt recebido no início da execução da etapa. |
| `status` | `VARCHAR(50)` | Sim | Estado atual da execução (ex.: `INICIADO`, `EM_PROCESSAMENTO`). Default inicial `INICIADO`. |
| `id_job` | `BINARY(36)` | Sim | Identificador do job da execução armazenado em binário; API mantém representação textual do UUID. |
| `created_at` | `DATETIME(3)` | Sim | Momento de criação do registro no banco. Default `CURRENT_TIMESTAMP(3)`. |
| `processing_started_at` | `DATETIME(3)` | Não | Momento em que o processamento efetivo da execução iniciou. |
| `completed_at` | `DATETIME(3)` | Não | Momento de conclusão da execução da etapa. |
| `prompt` | `TINYTEXT` | Não | Prompt final montado pelo Worker AI para a execução da etapa. |

### Chaves e restrições

- **Chave primária**: `id_job`.
- **Chave estrangeira**: `experiment_id` referencia `experiment.id` com `ON DELETE CASCADE`.
- **Índice auxiliar**: `idx_gera_landing_stage_execution_experiment` em `experiment_id`.

### Regras canônicas de contrato e consistência (modelo)

- `id_job` é persistido como **binário** (`BINARY(36)`) no banco; backend converte para/desde `String` UTF-8 nos contratos de API e integração com Worker.
- `prompt_content` e `prompt` seguem `TINYTEXT` conforme o schema atual; qualquer evolução de capacidade deve partir de mudança explícita de banco.
- O critério de pendência oficial é `status = INICIADO`.

---

## PARTE B — FLUXOS CANÔNICOS DO MÓDULO

### Catálogo de fluxos desta versão

- **Fluxo 01 — Geração de prompts pelo Worker AI (geralanding)**: detalhado abaixo.

### Fluxo 01 — geração de prompts pelo Worker AI

> Este fluxo descreve o caminho oficial e atual do processamento assíncrono do Gera Landing, da busca de pendências até a persistência do prompt final no backend.

### 1) Criação do job no backend com status inicial

1. O backend registra execução inicial da etapa (atualmente `landing-page-wireframe`) em `gera_landing_stage_execution`.
2. O registro nasce com `status = INICIADO`, `id_job` UUID convertido para `BINARY(36)`, `created_at` e `execution_requested_at`.
3. Este status inicial é a condição de elegibilidade para o polling do Worker AI.

### 2) Agendamento do Worker AI (polling contínuo)

1. O Worker AI executa um agendamento com cron padrão `0 */1 * * * *` (a cada 1 minuto).
2. Em cada ciclo, chama `processPendingExecutions()`.
3. O tamanho do lote é controlado por `geralanding.execution.pending-limit` (default `20`, com mínimo efetivo `1`).

### 3) Busca de jobs pendentes no endpoint interno do backend

1. O Worker AI chama `GET /api/internal/geralanding/stage-executions/pending?limit={n}`.
2. O backend retorna lista com payload mínimo por item: `experimentId`, `idJob`, `stageCode`.
3. O backend monta essa lista buscando somente registros com `status = INICIADO`, ordenados por `execution_requested_at` ascendente.

### 4) Loop de processamento no Worker AI

Para cada item retornado no lote:

1. O Worker valida presença de `stageCode` e `idJob`; itens inválidos são ignorados com log de aviso.
2. O `stageCode` é normalizado (`trim + lowerCase`).
3. Nesta versão canônica, apenas `landing-page-wireframe` entra no fluxo de geração; outras etapas são ignoradas sem erro.

### 5) Montagem do prompt da etapa no Worker AI

1. O Worker cria um `ExperimentPipelineJobDto` interno para acionar o serviço de montagem.
2. A montagem usa `GeraLandingService.montarERegistrarPromptEtapa(...)`, que:
   - carrega template base em `prompts/geralanding/{etapa}.md`;
   - resolve placeholders `{prompt-*}` com inclusão recursiva de outros prompts;
   - resolve placeholders `{dados-*}` a partir do payload do job (serializado de forma legível);
   - protege contra referência circular de prompts.
3. Após montar, o Worker registra log técnico de geração via endpoint interno já existente de `ai_worker_generation` (rastreabilidade de auditoria do prompt montado).

### 6) Envio do prompt montado para persistência no backend

1. O Worker chama `POST /api/internal/geralanding/stage-executions/{idJob}/receive-prompt`.
2. Body enviado pelo Worker:
   - `experimentId`
   - `stageCode`
   - `prompt`
3. O contrato do endpoint exige os três campos (`@NotNull` / `@NotBlank`).

### 7) Persistência final no backend e atualização de status

1. O backend localiza a execução por `idJob` usando a ocorrência mais recente (`findTopByIdJobOrderByExecutionRequestedAtDesc`).
2. Em caso de ausência da execução, retorna erro de entidade não encontrada e registra log de diagnóstico.
3. Em caso de sucesso, persiste:
   - `prompt` com o texto final montado;
   - `processing_started_at` com timestamp atual;
   - `status = EM_PROCESSAMENTO`.
4. O backend responde `202 Accepted` ao Worker.
5. O endpoint de pendências deve sempre retornar `experimentId`, `idJob` e `stageCode` para permitir processamento e rastreabilidade ponta a ponta.

## Escopo desta versão

Este documento cobre:

- **PARTE A (Modelo)**: modelo canônico da tabela `gera_landing_stage_execution`;
- **PARTE B (Fluxos)**: fluxo 01 de geração/persistência de prompts pelo Worker AI.

Evoluções de novas etapas do Gera Landing e novos estados do ciclo devem ser versionadas neste mesmo cânone.
