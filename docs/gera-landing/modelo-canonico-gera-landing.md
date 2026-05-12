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

### 1.2 Etapas suportadas oficialmente no código atual

- `landing-page-wireframe`;
- `landing-page-copy`.

Observação operacional: no worker `geralanding`, etapas fora desse conjunto ainda são ignoradas com log informativo.

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
| `error_message` | `LONGTEXT` | Não | Motivo textual resumido de falha quando houver. |
| `error_detail` | `LONGTEXT` | Não | Detalhe técnico complementar da falha (stack, contexto, payload rejeitado etc.). |
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


## 2.4 Evolução recente de schema (erro detalhado)

- ChangeSet Liquibase: `2026-05-09-add-gera-landing-error-detail`.
- Tabela afetada: `gera_landing_stage_execution`.
- Coluna adicionada: `error_detail` (`LONGTEXT`).
- Objetivo: separar mensagem resumida de falha (`error_message`) do detalhe técnico completo (`error_detail`) no fechamento de execução (`receive-result`).

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
  - objetos viram JSON pretty;
  - na etapa `landing-page-wireframe`, o foco é estrutura de venda para o público-alvo com coleta de informação para envio de amostra/prova do produto;
  - na etapa `landing-page-wireframe`, nenhum elemento recebe copy final: `texto.conteudo` deve permanecer vazio (`""`) em toda a árvore de elementos.

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

### 4.5.1 Regra canônica de compatibilidade de schema com OpenAI (obrigatória)

Para qualquer etapa do Gera Landing que use `text.format.type = json_schema` com `strict = true`, o schema enviado **deve** respeitar o subconjunto suportado pela OpenAI Structured Outputs.

Procedimento obrigatório antes de publicar alteração de schema:

1. Consultar a documentação oficial da OpenAI (Structured Outputs / response_format json_schema) via MCP/documentação oficial.
2. Validar se não há uso de keywords não suportadas no strict mode.
3. Registrar no histórico operacional (`docs/gera-landing/registros1.md`) a revisão realizada e o resultado.

Lista mínima de keywords de composição que **não podem** ser usadas no strict mode (fonte oficial OpenAI):

- `allOf`
- `not`
- `dependentRequired`
- `dependentSchemas`
- `if`
- `then`
- `else`

Consequência operacional esperada: evitar erro `400 invalid_json_schema` no Batch/Responses por envio de schema inválido.

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
- persiste `error_detail` (quando houver);
- persiste `openai_job_id` (quando informado);
- persiste `openai_model` (quando informado no handoff do prompt);
- persiste `input_tokens`, `output_tokens`, `cost_usd`;
- seta `completed_at = now`;
- seta status:
  - `FALHA` se `error_message` preenchido;
  - `CONCLUIDO` caso contrário.

Além disso, sem erro e com `modelResponse` válido, o backend atualiza o artefato consolidado no `experiment` conforme a etapa:

- `landing-page-wireframe`:
  - `landingPageWireframe = modelResponse`
  - `landingPageWireframeJobId = execution.idJob`.
- `landing-page-copy`:
  - `landingPageCopy = modelResponse`
  - `landingPageCopyJobId = execution.idJob` (versão da copy que deve ser usada nas próximas etapas).

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
   - erro final persistido em `error_message` (resumo) e opcionalmente `error_detail` (detalhe técnico).

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
- `error_message` e `error_detail` quando aplicável.

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

---

## 9) Construção canônica das telas (fonte: frontend atual)

> Esta seção documenta **como as telas estão implementadas hoje** no frontend administrativo, tomando o código como fonte de verdade.

## 9.1 Tela de monitoramento (card no detalhe do experimento)

Local: aba `gera-landing` em `ExperimentDetailPage`.

### 9.1.1 Estrutura visual do card de execução

- O módulo é renderizado em cards separados por etapa; hoje existe card explícito para:
  - **Gera WireFrame** (stage `landing-page-wireframe`)
  - **Gera Copy** (stage `landing-page-copy`)
- Cada card contém:
  1. título da etapa;
  2. badge de custo agregado de execuções concluídas (`Total execuções: US$ ...`);
  3. botão de início da etapa (`Iniciar`);
  4. tabela de **execuções em andamento** (quando houver);
  5. bloco de **histórico de execuções** com custo por linha.

### 9.1.2 Fontes de dados e atualização

Para cada etapa, a tela abre duas consultas do mesmo endpoint, mudando apenas `includeCompleted`:

- **Pendentes/rodando**: `GET /api/experiments/{experimentId}/geralanding/stage-executions?stageCode=...&includeCompleted=false`
  - atualização automática a cada **10 segundos** (`refetchInterval: 10000`);
  - usada para alimentar a tabela “jobs da etapa”.
- **Histórico (incluindo concluídas)**: `GET ... includeCompleted=true`
  - sem polling automático (`refetchInterval: false`);
  - usada para histórico e para o somatório de custo exibido no badge.

### 9.1.3 Regras de habilitação do botão “Iniciar”

O botão fica desabilitado quando:

1. há requisição de start em andamento (`isStarting... = true`), ou
2. existe ao menos um job em estado não final da mesma etapa (`hasRunning...Execution = true`).

Com isso, o comportamento operacional é de **um job por etapa em execução simultânea na UI**.

### 9.1.4 Estados de renderização do bloco “em andamento”

Na área de jobs ativos, o frontend aplica três estados mutuamente exclusivos:

1. `isLoading`: texto “Carregando jobs da etapa...”;
2. lista vazia: texto “Nenhum job pendente ou em execução.”;
3. lista com dados: tabela com colunas `Job ID`, `Status`, `Data-hora`.

O `Job ID` sempre vira link navegável para a tela de detalhe (`/experiments/{id}/geralanding/stage-executions/{jobId}`).

### 9.1.5 Estados de renderização do histórico

No bloco “Histórico de execuções”, o frontend aplica:

1. `isLoading`: texto “Carregando execuções...”;
2. lista vazia: texto “Nenhuma execução registrada para esta etapa.”;
3. lista com dados: tabela com colunas:
   - `Job ID` (link para detalhe);
   - `Status`;
   - `Data-hora`;
   - `Custo` (alinhado à direita).

O custo por linha usa `costUsd` quando disponível e cai para `0` quando ausente.

### 9.1.6 Contrato de start disparado pelo card

Na etapa wireframe, o botão chama:

- `POST /api/experiments/{experimentId}/geralanding/wireframe/start`

com feedback visual de loading no próprio botão (`spinner` + texto “Iniciando...” ).

## 9.2 Tela de detalhe por execução (reutilizável por qualquer estágio)

Local: rota `/experiments/:id/geralanding/stage-executions/:jobId`.

### 9.2.1 Premissa de reutilização

A tela é orientada por `jobId` e consulta única:

- `GET /api/experiments/{experimentId}/geralanding/stage-executions/{jobId}`

Como o payload traz `stageCode`, a mesma página já funciona para qualquer estágio do Gera Landing, sem template dedicado por etapa.

### 9.2.2 Estrutura de navegação e contexto

- breadcrumb: `Experimento > Detalhe da execução`;
- título principal “Detalhe da execução Gera Landing”;
- badge com `stageCode` quando presente;
- botão “Voltar” para a página do experimento.

### 9.2.3 Estados de carregamento/erro

1. carregando: “Carregando detalhes da execução...”;
2. erro/sem payload: “Não foi possível carregar os detalhes da execução.”;
3. sucesso: renderiza painéis de metadados e artefatos.

### 9.2.4 Bloco de alerta de falha

- Se `errorMessage` existir, exibe alerta vermelho com mensagem literal do backend.
- Se status for `FALHA` sem `errorMessage`, exibe alerta amarelo com fallback: “não informado pelo Worker AI.”

### 9.2.5 Grade de metadados operacionais

A tela exibe, em grid de duas colunas (quando houver espaço), os campos:

- `Job ID`, `Status`, `Stage`;
- `OpenAI Job ID`;
- `Modelo usado` (extraído por parse de `openAiRequestBody.model`);
- `Criado em`, `Solicitado em`, `Processamento iniciado`, `Concluído em`;
- `Input tokens`, `Output tokens`;
- `Prompt template ID`;
- `Custo USD`.

Campos ausentes são mostrados como `—`.

### 9.2.6 Blocos auditáveis com copiar/baixar

Para cada artefato textual/JSON, a UI oferece:

- botão **Copiar** (clipboard com fallback para `textarea`);
- botão **Baixar** (`data:` URL);
- visualização colapsável (`CollapsibleJsonViewer`) ou markdown (`MarkdownContentViewer`).

Artefatos renderizados:

1. `promptContent`
2. `prompt`
3. `openAiRequestBody`
4. `schemaJson`
5. `promptMarkdownContent` (viewer markdown)
6. `modelResponse`

### 9.2.7 Área de HTML provisório

- Quando `provisionalHtml` existe:
  - botão copiar HTML;
  - botão baixar `.html`;
  - link “Abrir HTML provisório em nova aba” com `target="_blank"` e `rel="noopener noreferrer"`;
  - preview em `<pre><code>` com rolagem.
- Quando não existe: mensagem “Nenhum HTML provisório disponível para este registro.”

## 9.3 Contrato frontend canônico para monitoramento e detalhe

O hook `useGeraLandingStageExecutions` define o contrato mínimo de item para listagens:

- `idJob`
- `status`
- `executionRequestedAt`
- `costUsd?`

O hook `useGeraLandingStageExecutionDetail` estende para auditoria completa com os campos de prompt, schema, request OpenAI, resposta e métricas.

Esse contrato é a base concreta para suportar **qualquer estágio** na tela de detalhe, desde que o backend continue retornando a mesma estrutura por `jobId`.

## 12) Mecanismo canônico de Assembler por etapa para HTML provisório

### 12.1 Objetivo do mecanismo

No pipeline Gera Landing, cada etapa de geração (ex.: `landing-page-wireframe`, `landing-page-copy`, `landing-page-image-briefing`, `landing-page-design-preset`) deve possuir seu próprio **Assembler** responsável por transformar a saída estruturada do modelo em um HTML provisório visualizável e auditável.

Esse HTML provisório é um artefato operacional de inspeção rápida e validação humana do conteúdo da etapa. Ele **não substitui** o render final da landing, mas garante:

- previsibilidade de apresentação por etapa;
- rastreabilidade do que foi gerado pelo modelo;
- persistência de uma visão legível no backend;
- contrato uniforme entre worker, backend e UI administrativa.

### 12.2 Princípio arquitetural (independente da etapa)

Cada etapa segue o mesmo ciclo:

1. O worker gera `modelResponse` conforme o schema da etapa.
2. O backend recebe `modelResponse` em `receive-result`.
3. O backend resolve o `StageCode` da execução.
4. O backend delega para o Assembler específico daquela etapa.
5. O Assembler converte o JSON da etapa em HTML provisório seguro para preview.
6. O backend persiste o HTML em `gera_landing_stage_execution.provisional_html`.
7. A UI administrativa lê esse campo para inspeção operacional.

### 12.3 Contrato mínimo canônico dos Assemblers

Para manter consistência cross-etapas, todo Assembler deve seguir o contrato mínimo:

- Entrada principal:
  - `modelResponse` (JSON retornado pelo modelo).
  - `jobId` (quando aplicável, para anotação de rastreio no HTML).
- Saída:
  - `String` contendo HTML provisório completo **ou** `null` quando não for possível montar com segurança.
- Comportamento de robustez:
  - nunca lançar erro para fora do fluxo de persistência final;
  - falhas de parse/normalização retornam `null` e ficam registráveis em auditoria;
  - tolerar variações controladas de envelope (objeto raiz da etapa ou objeto aninhado).

### 12.4 Responsabilidade por etapa (não ficar preso em Wireframe)

A estratégia canônica exige um Assembler dedicado por etapa de pipeline. Exemplo de distribuição de responsabilidades:

- **Wireframe Assembler**:
  - monta estrutura visual da página por seções;
  - aplica placeholders e estilos de preview para leitura rápida de layout.

- **Copy Assembler**:
  - organiza títulos, subtítulos, bullets, CTAs, provas e blocos textuais;
  - destaca hierarquia de copy e intenção de conversão sem depender do layout final.

- **Image Briefing Assembler**:
  - renderiza briefing por bloco (contexto, cena, elementos obrigatórios, restrições, variações);
  - facilita validação semântica de direção de arte por etapa.

- **Design Preset Assembler**:
  - apresenta decisões de sistema visual (tipografia, cores, espaçamentos, componentes, tokens);
  - permite revisão rápida de coerência estética antes da composição final.

Observação: os nomes concretos de classes podem variar, mas o padrão arquitetural e o contrato operacional devem permanecer os mesmos.

### 12.5 Roteamento canônico por `stage_code`

O backend deve possuir um roteador/fábrica de assembler por etapa, baseado em `stage_code`, com mapeamento explícito e sem heurísticas implícitas. Exemplo conceitual:

- `landing-page-wireframe` → Wireframe Assembler
- `landing-page-copy` → Copy Assembler
- `landing-page-image-briefing` → Image Briefing Assembler
- `landing-page-design-preset` → Design Preset Assembler

Regras:

- estágio não mapeado: persistir `provisional_html = null` e registrar a condição em auditoria;
- não compartilhar lógica de render específica entre etapas quando isso causar acoplamento indevido;
- extrair apenas utilitários genéricos realmente reutilizáveis (sanitização, helpers de markup, escape).

### 12.6 Persistência e consumo no backend

No fechamento da execução (`receive-result`), o backend deve aplicar a política abaixo:

1. Se o payload já trouxer `provisionalHtml` explícito e confiável, pode persistir esse valor.
2. Caso contrário, gerar via Assembler da etapa.
3. Persistir em `provisional_html`.
4. Manter `model_response` bruto para auditoria paralela.

Essa abordagem garante dupla rastreabilidade:

- o dado estruturado original do modelo;
- a visualização provisória derivada para operação.

### 12.7 Diretrizes de qualidade para os novos Assemblers

Ao criar Assemblers de novas etapas, cumprir obrigatoriamente:

- aderência ao schema da etapa e aos documentos canônicos de artefato;
- saída HTML estável para o mesmo input (determinismo funcional);
- tratamento de campos ausentes sem quebrar o fluxo;
- cobertura de testes unitários da etapa (sucesso, input parcial, input inválido);
- não injetar JSON-em-JSON em campos textuais;
- preservar foco de UX operacional: informação clara, sem poluição visual e sem contradições.

### 12.8 Benefícios do padrão por etapa

O padrão de Assembler dedicado por etapa traz:

- escalabilidade do pipeline sem crescimento de complexidade acoplada;
- facilidade de evolução de contratos por etapa;
- depuração mais rápida (erro localizado por estágio);
- melhor governança entre backend, worker e frontend administrativo;
- base consistente para futuras etapas além de Wireframe, Copy, Image Briefing e Design Preset.
