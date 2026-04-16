# Avatar Sales Video — Status Atual (baseado em código implementado)

- **Data da verificação:** 2026-04-16
- **Método:** leitura de código-fonte e documentação local do repositório (sem validação E2E em ambiente remoto).
- **Objetivo:** registrar o status real do módulo considerando o que **já existe implementado**.

---

## 1) Resumo objetivo

O módulo de Avatar Sales Video está em **MVP funcional interno** (não apenas planejado):

- backend com domínio, tabelas, serviços e APIs públicas/internas;
- `ai-worker` processando jobs OpenAI de script por polling + claim/progress/complete/fail;
- frontend com telas para operar perfis, scripts, render, retry e publicação em slots;
- `video-management-service` implementado com polling/disparo e provider `stub`.

Ponto crítico atual: o render de vídeo no módulo dedicado ainda está centrado no provider de desenvolvimento (`stub`), então a prontidão para escala depende da integração com provider real.

---

## 2) Evidências confirmadas no código

## 2.1 Backend (`backend/ads-service`) — **Implementado**

### Banco / modelo
- Migração SQL de fundação já criada com tabelas:
  - `sales_video_profile`
  - `sales_video_script`
  - `sales_video_job`
  - `sales_video_job_event`
  - `landing_video_slot`
- Arquivo: `src/main/resources/db/changelog/V2037_01_05__sales_video_foundation.sql`.

### APIs administrativas
- Perfis/scripts/render:
  - `POST /api/products/{productId}/sales-videos/profiles`
  - `GET /api/products/{productId}/sales-videos/profiles`
  - `GET /api/sales-videos/profiles/{profileId}`
  - `GET /api/sales-videos/profiles/{profileId}/scripts`
  - `POST /api/sales-videos/profiles/{profileId}/generate-script`
  - `POST /api/sales-videos/profiles/{profileId}/approve-script`
  - `POST /api/sales-videos/profiles/{profileId}/request-render`
- Arquivo: `src/main/java/com/marketinghub/salesvideo/web/SalesVideoProfileController.java`.

### APIs administrativas de operação
- Jobs/eventos/retry:
  - `GET /api/sales-videos/profiles/{profileId}/jobs`
  - `GET /api/sales-videos/jobs/{jobId}`
  - `GET /api/sales-videos/jobs/{jobId}/events`
  - `POST /api/sales-videos/jobs/{jobId}/retry`
- Arquivo: `src/main/java/com/marketinghub/salesvideo/web/SalesVideoJobAdminController.java`.

### APIs internas para workers
- OpenAI worker:
  - base: `/internal/ai/openai-jobs`
  - operações: list/get/claim/heartbeat/progress/complete/fail
- Módulo de vídeo:
  - base: `/internal/video/jobs`
  - operações: list/get/claim/heartbeat/progress/complete/fail/expired
- Arquivos:
  - `src/main/java/com/marketinghub/salesvideo/web/SalesVideoInternalOpenAiJobController.java`
  - `src/main/java/com/marketinghub/salesvideo/web/SalesVideoInternalVideoJobController.java`

### Rotinas operacionais automáticas
- Auto retry de jobs falhos (configurável):
  - `sales-video.reprocess.auto.*`
- Limpeza de assets órfãos (configurável):
  - `sales-video.assets.cleanup.*`
- Arquivos:
  - `src/main/java/com/marketinghub/salesvideo/service/SalesVideoAutoRetryScheduler.java`
  - `src/main/java/com/marketinghub/salesvideo/service/SalesVideoAssetCleanupScheduler.java`

---

## 2.2 AI Worker (`ai-worker`) — **Implementado para scripts OpenAI**

### Scheduler + processamento
- Scheduler com `fixedDelay` configurável (`sales-video.script.fixed-delay`, default 45000).
- Flag de ativação: `sales-video.script.enabled`.
- Arquivo: `src/main/java/com/marketinghub/worker/salesvideo/SalesVideoScriptJobScheduler.java`.

### Ciclo de job
- Busca jobs `SCRIPT_PENDING` do backend.
- Faz `claim`, reporta `progress`, chama OpenAI, envia `complete` ou `fail`.
- Mantém classificação de falha (`OPENAI_ERROR`, `BACKEND_ERROR`, etc.).
- Arquivo: `src/main/java/com/marketinghub/worker/salesvideo/SalesVideoScriptJobService.java`.

### Cliente backend interno
- Consome `/internal/ai/openai-jobs` e `/api/sales-videos/profiles/{id}`.
- Usa payloads de claim/progress/complete/fail já tipados.
- Arquivo: `src/main/java/com/marketinghub/worker/salesvideo/SalesVideoBackendClient.java`.

---

## 2.3 Frontend (`frontend`) — **Implementado para operação administrativa**

### Tela de produto (perfis)
- Criação/listagem de perfis por produto.
- Tipos suportados na UI: `HERO`, `OBJECTION`, `PROOF`.
- Arquivo: `src/pages/salesVideo/ProductSalesVideoPage.tsx`.

### Tela de detalhe do perfil
- Solicitar geração de script.
- Aprovar script.
- Solicitar render com `providerFamily` (`OPENAI` ou `EXTERNAL_VIDEO_MODULE`).
- Acompanhar jobs e eventos.
- Reprocessar jobs com motivo.
- Gerenciar slots da landing e histórico.
- Arquivo: `src/pages/salesVideo/SalesVideoProfileDetailPage.tsx`.

### Camada de API
- Hooks para perfis, scripts, jobs, retry, slots e histórico estão implementados em `src/api/salesVideo/`.

---

## 2.4 Video Management Service (`video-management-service`) — **Implementado em estágio inicial**

### Polling + dispatcher
- Poller agendado que busca jobs `VIDEO_REQUESTED` em `/internal/video/jobs`.
- Execução controlada por `video.jobs.polling-enabled`.
- Arquivo: `src/main/java/com/marketinghub/videomanagement/service/VideoJobPoller.java`.

### Processamento do job
- Faz claim automático, resolve provider, processa artefatos e reporta conclusão/falha.
- Upload de assets para backend antes de completar job.
- Arquivo: `src/main/java/com/marketinghub/videomanagement/service/VideoJobProcessor.java`.

### Integração com backend
- Cliente dedicado com autenticação opcional por bearer token.
- Consome endpoints internos e API de perfil.
- Arquivo: `src/main/java/com/marketinghub/videomanagement/client/BackendVideoClient.java`.

### Provider atual
- Provider principal de desenvolvimento: `StubVideoProvider`.
- Gera MP4/PNG/VTT artificiais e metadados locais.
- Arquivo: `src/main/java/com/marketinghub/videomanagement/service/provider/StubVideoProvider.java`.

---

## 3) O que já está pronto vs o que falta

## Pronto (confirmado em código)
1. Domínio `salesvideo` persistido no backend.
2. APIs administrativas e internas para orquestração.
3. Worker OpenAI de script funcional no fluxo assíncrono.
4. UI administrativa para operação de perfis e jobs.
5. Serviço dedicado de vídeo com pipeline técnico e upload de assets.

## Falta / próximo nível
1. Integração de provider real de avatar falante no `video-management-service`.
2. Testes E2E formais cobrindo fluxo completo entre os 4 módulos.
3. Endurecimento de observabilidade para operação em escala (SLO/alertas/painéis de produção).
4. Rollout por tenant com critérios de go/no-go documentados.

---

## 4) Classificação atual

- **Status funcional:** MVP interno operacional.
- **Status de produção ampla:** parcial (dependente de provider real + hardening operacional).
- **Risco principal no estado atual:** discrepância entre sucesso do fluxo `stub` e comportamento real com provider externo.

