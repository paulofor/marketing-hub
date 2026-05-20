# Especificação — Worker da Biblioteca de Sales Pages (MOIS)

Data: 2026-05-19

## 1. Objetivo

Processar de forma assíncrona os jobs `PENDING` da biblioteca de páginas de vendas do MOIS, executar análise estruturada inicial de cada URL e persistir os artefatos de análise no backend para uso operacional e comercial.

Referência canônica:
- `docs/canonical/mois-worker-canon.v1.md`

## 2. Escopo

### 2.1 Responsabilidades
- Consumir jobs da fila via backend (`jobs:claim`).
- Fazer fetch do conteúdo da página de vendas (URL canônica).
- Extrair sinais estruturais e comerciais mínimos.
- Calcular score agregado.
- Publicar resultado em `jobs/{jobId}:complete`.
- Em erro, publicar falha em `jobs/{jobId}:fail`.

### 2.2 Fora de escopo
- Acesso direto ao banco de dados.
- Alteração de regras de domínio fora do módulo MOIS.
- Orquestração de módulos externos sem passar pelo backend principal.

## 3. Fluxo operacional

1. Scheduler do worker dispara um ciclo de polling.
2. Worker chama `POST /api/mois/sales-library/jobs:claim` com `workspaceId` e `source`.
3. Backend retorna:
   - `claimed=false` quando não há job pendente.
   - `claimed=true` + `job` quando um job é reservado.
4. Quando reservado, backend muda o job de `PENDING` para `FETCHING`.
5. Worker processa a URL da página.
6. Em sucesso, worker chama `POST /api/mois/sales-library/jobs/{jobId}:complete`.
7. Em falha, worker chama `POST /api/mois/sales-library/jobs/{jobId}:fail`.

## 4. Máquina de estados

Estados esperados dos jobs:
- `PENDING`: aguardando consumo.
- `FETCHING`: job reservado e em processamento.
- `DONE`: concluído com análise persistida.
- `FAILED`: concluído com erro categorizado.

Transições permitidas:
- `PENDING -> FETCHING`
- `FETCHING -> DONE`
- `FETCHING -> FAILED`

## 5. Contratos de API

### 5.1 Claim
- Endpoint: `POST /api/mois/sales-library/jobs:claim`
- Request mínimo:
  - `workspaceId`
  - `source`
- Response:
  - `claimed` (boolean)
  - `job` (quando `claimed=true`): `jobId`, `pageId`, `urlCanonical`, `title`

### 5.2 Complete
- Endpoint: `POST /api/mois/sales-library/jobs/{jobId}:complete`
- Payload mínimo:
  - `scoreTotal`
  - `sectionsJson`
  - `copyJson`
  - `visualJson`
  - `imageJson`
  - `analysisNotes`
  - `parserVersion`
  - `promptVersion`
  - `modelName`
  - `analyzedAt` (opcional)

Efeito esperado:
- Inserção em `mois_sales_library_page_analysis` com status `DONE`.
- Atualização do job para status `DONE`.

### 5.3 Fail
- Endpoint: `POST /api/mois/sales-library/jobs/{jobId}:fail`
- Payload mínimo:
  - `errorCategory`
  - `errorMessage`

Efeito esperado:
- Atualização do job para status `FAILED` com causa explícita.

## 6. Requisitos de processamento

### 6.1 Requisitos funcionais
- O worker deve encerrar o ciclo sem erro quando `claimed=false`.
- O worker deve processar exatamente 1 job por ciclo de polling.
- O worker deve sempre finalizar um job claimado com `complete` ou `fail`.

### 6.2 Requisitos técnicos
- Polling baseado em agendamento (`@Scheduled`) com delay configurável.
- Timeout de request HTTP configurável para fetch da página.
- Chamadas ao backend com base URL configurável por ambiente.

## 7. Configuração

Propriedades mínimas:
- `worker.backend-base-url`
- `worker.workspace-id`
- `worker.source`
- `worker.poll-interval-ms`
- `worker.request-timeout-ms`

Defaults atuais no projeto:
- `BACKEND_BASE_URL=http://191.252.181.168:8000`
- `MOIS_WORKSPACE_ID=workspace-001`
- `MOIS_SOURCE=CLICKBANK`
- `MOIS_POLL_INTERVAL_MS=15000`
- `MOIS_REQUEST_TIMEOUT_MS=30000`

## 8. Observabilidade (obrigatório)

Logs mínimos por ciclo:
- início do ciclo com contexto (`workspaceId`, `source`, timeouts);
- resultado do claim (`claimed`, presença de job);
- `jobId`/`pageId`/`urlCanonical` quando houver claim;
- sucesso de `complete` com status HTTP;
- falha com `errorCategory`, `errorMessage` e exceção completa.

## 9. Critérios de aceite

1. Jobs pendentes são consumidos e transitam para `FETCHING`.
2. Jobs processados encerram em `DONE` ou `FAILED`.
3. Jobs `DONE` geram registro em `mois_sales_library_page_analysis` com artefatos completos.
4. Diagnóstico operacional é possível somente pelos logs e status da fila.
5. A tela `/mois/sales-pages-library` reflete o ciclo completo da fila.

## 10. Riscos operacionais e mitigação

Riscos comuns:
- Worker fora do ar (fila acumula em `PENDING`).
- `workspaceId/source` incompatíveis com os jobs (não há claim).
- Timeout/rede em fetch de página.

Mitigação:
- health checks e alertas de backlog de `PENDING`;
- validação de configuração em startup;
- logs com contexto completo para troubleshooting.

## 11. Referências de implementação

- Backend Controller:
  - `backend/ads-service/src/main/java/com/marketinghub/mois/biblioteca/web/MoisSalesLibraryController.java`
- Backend Service:
  - `backend/ads-service/src/main/java/com/marketinghub/mois/biblioteca/service/MoisSalesLibraryService.java`
- Worker Pipeline:
  - `mois-sales-library-worker/src/main/java/com/marketinghub/mois/libraryworker/service/PipelineRunner.java`
- Worker Client:
  - `mois-sales-library-worker/src/main/java/com/marketinghub/mois/libraryworker/client/BackendClient.java`
- Worker Config:
  - `mois-sales-library-worker/src/main/resources/application.yml`
  - `mois-sales-library-worker/src/main/java/com/marketinghub/mois/libraryworker/config/WorkerProperties.java`
- Cânone:
  - `docs/canonical/mois-worker-canon.v1.md`
