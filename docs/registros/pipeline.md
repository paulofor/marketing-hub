# Registro de pipelines

## 2026-06-26 — Mapa do pipeline NichoCNAE v3

### Visão geral

- **Executor real do pipeline v3:** `oprm-coletor-mei`.
- **Pacote executor:** `com.marketinghub.pipelines.nichocnae.v3`.
- **Backend fonte de verdade e controle:** `backend/ads-service`.
- **Pacote backend:** `com.marketinghub.oprmcoletormei.nichocnae.v3`.
- **Padrão de comunicação:** o executor consome pendências no endpoint `pending` do backend, executa a etapa e devolve `complete` ou `fail`.

### Pacotes no módulo executor `oprm-coletor-mei`

Raiz do executor:

- `com.marketinghub.pipelines.nichocnae.v3`

Subpacotes identificados:

- `cnaeintake`
- `core`
- `execution`
- `personacandidategenerator`
- `personatournament`
- `routinequeryplanner`
- `sourcesearcher`
- `sourcefetcher`
- `routinesignalextractor`
- `dailytaskssynthesizer`
- `qualitygate`
- `personaroutinematerializer`

### Pacotes no backend `backend/ads-service`

Raiz backend:

- `com.marketinghub.oprmcoletormei.nichocnae.v3`

Subpacotes principais identificados:

- `cnaeintake`
- `personacandidategenerator`
- `personatournament`
- `routinequeryplanner`
- `sourcesearcher`
- `sourcefetcher`
- `routinesignalextractor`
- `dailytaskssynthesizer`
- `qualitygate`
- `personaroutinematerializer`
- `progress`
- `shared`

### Etapas v3 registradas

- `cnae-intake`
- `persona-candidate-generator`
- `persona-tournament`
- `routine-query-planner`
- `source-searcher`
- `source-fetcher`
- `routine-signal-extractor`
- `daily-tasks-synthesizer`
- `quality-gate`
- `persona-routine-materializer`

### Endpoints e leitura administrativa

- O padrão interno por etapa é `/api/internal/oprmcoletormei/nichocnae/v3/<stageCode>/stage-executions`.
- A leitura administrativa de progresso fica no pacote `com.marketinghub.oprmcoletormei.nichocnae.v3.progress`.
- O endpoint administrativo de progresso é `/api/oprm/nichocnae/v3/cnaes/{cnaeCode}/progress`.
- A confirmação de finalização usa `/api/oprm/nichocnae/v3/cnaes/{cnaeCode}/progress/confirm-finalization`.

## 2026-06-27 — Saídas funcionais das etapas 6 a 9 do NichoCNAE v3

- Causa-raiz: as etapas 6 (`source-fetcher`), 7 (`routine-signal-extractor`), 8 (`daily-tasks-synthesizer`) e 9 (`quality-gate`) do executor `oprm-coletor-mei` retornavam apenas contratos genéricos de conclusão, sem artefatos funcionais compatíveis com coleta de evidência, extração de sinais, síntese de tarefas e decisão de qualidade.
- Correção aplicada: as etapas passaram a transformar entradas persistidas em `sourceSnapshots`, `routineSignals`, `dailyTasks` e decisão de gate com critérios, motivo e etapa recomendada de correção quando bloquear.
- Prevenção: adicionados testes unitários específicos para cada etapa, garantindo que a saída não volte a ser apenas status genérico.

## 2026-06-27 — Auditoria OpenAI em recebeRequest/recebeResponse no NichoCNAE v3

- Verificação: a única etapa v3 atual que chama OpenAI diretamente é `persona-candidate-generator` no executor `oprm-coletor-mei`.
- Causa-raiz: a etapa registrava request/response da OpenAI apenas em log local do executor, mas não enviava os payloads ao backend pelos callbacks canônicos `recebeRequest` e `recebeResponse`, deixando a auditoria persistida incompleta.
- Correção aplicada: antes da chamada à OpenAI o executor envia o request bruto, prompt, schema e plataforma ao endpoint `recebeRequest`; após sucesso ou erro HTTP da OpenAI envia response bruto, modelo, tokens quando disponíveis e descrição de erro ao endpoint `recebeResponse`.
- Prevenção: o teste do cliente OpenAI passou a validar a sequência backend `recebeRequest` → OpenAI `/responses` → backend `recebeResponse` nos cenários de sucesso e erro.
