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

- O padrão interno por etapa é `/api/internal/oprm/nichocnae/v3/<stageCode>/stage-executions`.
- A leitura administrativa de progresso fica no pacote `com.marketinghub.oprmcoletormei.nichocnae.v3.progress`.
- O endpoint administrativo de progresso é `/api/oprm/nichocnae/v3/cnaes/{cnaeCode}/progress`.
- A confirmação de finalização usa `/api/oprm/nichocnae/v3/cnaes/{cnaeCode}/progress/confirm-finalization`.
