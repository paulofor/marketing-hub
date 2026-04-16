# Avatar Sales Video — Histórico de Implantação

## Como ler este histórico

Este arquivo registra, de forma cumulativa, as entregas e pendências relevantes do módulo Avatar Sales Video, seguindo o protocolo em `avatar-sales-video-implementation-history-protocol.md`.

Cada entrada descreve:
- o que foi implementado;
- o que foi alterado;
- validações executadas;
- limitações e continuidade.

---

## Índice rápido
- 2026-04-16 — Sprint V2 (robustez do ciclo assíncrono e recuperação de órfãos)
- 2026-04-16 — Sprint V1 (contrato de integração e atualização de planejamento)
- 2026-04-16 — Sprint V1 (implementação do adapter real e integração backend)

---

## Entradas

## 2026-04-16 — Sprint V2 (robustez do ciclo assíncrono e recuperação de órfãos)

**Status:** concluída com pendências

### Resumo
- A Sprint V2 fortaleceu o ciclo assíncrono com deduplicação de claim, retry técnico para integração backend e recuperação automática de jobs órfãos.
- O `video-management-service` passou a tratar concorrência de workers como condição operacional prevista.
- O contrato OpenAPI foi atualizado para deixar explícitos cenários de claim concorrente.

### O que foi implementado
- Tratamento seguro de `claim` duplicado (`409`) e job inexistente (`404`) no `VideoJobProcessor`.
- Retry técnico para chamadas ao backend com tentativas e backoff configuráveis.
- Recuperação de jobs órfãos (`VIDEO_PROCESSING` stale) no `VideoJobPoller`.
- Heartbeat explícito no início do processamento.
- Enriquecimento do modelo local de job para refletir campos de retry/tenant já existentes no contrato.

### O que foi alterado
- Arquivos:
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/exception/BackendIntegrationException.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/config/VideoManagementProperties.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/client/dto/SalesVideoJob.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/client/BackendVideoClient.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/service/VideoJobProcessor.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/service/VideoJobPoller.java`
  - `video-management-service/src/main/resources/application.yml`
  - `video-management-service/src/test/java/com/marketinghub/videomanagement/service/VideoJobProcessorTest.java`
  - `video-management-service/src/test/java/com/marketinghub/videomanagement/service/VideoAssetUploaderTest.java`
  - `video-management-service/src/test/java/com/marketinghub/videomanagement/service/provider/StubVideoProviderTest.java`
  - `docs/novos-modulos/avatar/avatar-sales-video-integration-swagger.yaml`
  - `docs/novos-modulos/avatar/avatar-sales-video-restart-plan.md`
  - `docs/novos-modulos/avatar/avatar-sales-video-implementation-history.md`
- Módulos:
  - `video-management-service`
  - documentação canônica do módulo Avatar Sales Video
- Endpoints/contratos:
  - `/internal/video/jobs` (uso adicional para status `VIDEO_PROCESSING`)
  - `/internal/video/jobs/{jobId}/claim` (tratamento explícito de `404` e `409`)
  - `/internal/video/jobs/{jobId}/heartbeat`
  - `/internal/video/jobs/{jobId}/fail`

### Contratos e artefatos afetados
- `avatar.salesVideoRenderJob.v1` (campos de retry, tenant, progress e status de execução).
- `avatar.salesVideoJobEvent.v1` (maior previsibilidade de heartbeat/progresso em cenários de recuperação).
- `JobFailureRequest` atualizado com `retryable` e `retryReason` no Swagger canônico de integração.

### Testes e validações executados
- Teste unitário de claim duplicado no `VideoJobProcessorTest`.
- Suíte de testes do `video-management-service` para validar regressão local.
- Revisão de aderência da documentação do plano e do Swagger com o protocolo de histórico.

### Limitações e pendências
- Ainda sem validação E2E em staging dos cenários de órfão + concorrência com backend compartilhado.
- Sem métricas e alertas para observar taxa de recuperação/retry em produção (Sprint V3).
- Retry técnico atual é focado em falhas transitórias de integração, sem reabertura automática de novo job canônico no backend.

### Próximo passo sugerido
- Executar Sprint V3 com foco em observabilidade operacional (métricas, logs correlacionáveis, dashboards e alertas).

### Handoff para a próxima etapa
- Prioridade imediata: instrumentação de métricas para backlog, retries e orphans recoveries.
- O que não deve ser refeito: deduplicação de claim e retry técnico transitório já implementados neste ciclo.
- Riscos abertos: divergência de semântica de `updatedAt` entre ambientes pode afetar limiar de órfão.
- Dependências externas: ambiente staging com carga concorrente e provider real habilitado.
- Onde continuar: `video-management-service/src/main/java/com/marketinghub/videomanagement/service/VideoJobPoller.java` e `.../BackendVideoClient.java`.

## 2026-04-16 — Sprint V1 (contrato de integração e atualização de planejamento)

**Status:** concluída com pendências

### Resumo
- Sprint V1 foi consolidada no plano de reinício com foco em contrato operacional e rastreabilidade.
- Foi criado um documento OpenAPI dedicado à troca de dados backend ↔ módulo de vídeo.
- As pendências críticas de robustez foram explicitamente carregadas para a Sprint V2.

### O que foi implementado
- Atualização do fechamento da Sprint V1 no plano de reinício.
- Preenchimento do bloco obrigatório de handoff para a próxima sprint.
- Formalização do contrato de endpoints e payloads com OpenAPI 3.0.3.

### O que foi alterado
- Arquivos:
  - `docs/novos-modulos/avatar/avatar-sales-video-restart-plan.md`
  - `docs/novos-modulos/avatar/avatar-sales-video-integration-swagger.yaml`
  - `docs/novos-modulos/avatar/avatar-sales-video-implementation-history.md`
- Módulos:
  - Documentação canônica do módulo Avatar Sales Video.
- Endpoints/contratos:
  - `/internal/video/jobs`
  - `/internal/video/jobs/{jobId}`
  - `/internal/video/jobs/{jobId}/claim`
  - `/internal/video/jobs/{jobId}/heartbeat`
  - `/internal/video/jobs/{jobId}/progress`
  - `/internal/video/jobs/{jobId}/complete`
  - `/internal/video/jobs/{jobId}/fail`
  - `/internal/video/jobs/{jobId}/expired`
  - `/api/sales-videos/profiles/{profileId}`

### Contratos e artefatos afetados
- DTOs de job e perfil (`SalesVideoJobDto`, `SalesVideoProfileDto`).
- Payloads de atualização assíncrona (`JobClaimRequest`, `JobProgressRequest`, `JobCompletionRequest`, `JobFailureRequest`, `JobHeartbeatRequest`, `JobExpirationRequest`).
- Enumerações canônicas de status, tipo de job, família de provider e retry reason.

### Testes e validações executados
- Revisão de aderência entre o Swagger novo e os controladores/DTOs existentes no backend.
- Revisão de consistência do planejamento da Sprint V1 com o protocolo de histórico.
- Verificação local de mudanças via `git diff` e inspeção dos arquivos alterados.

### Limitações e pendências
- Integração com provider real ainda não está validada em staging nesta entrega documental.
- Políticas de timeout/retry/claim duplicado permanecem para Sprint V2.
- Observabilidade e alertas seguem como pendência para Sprint V3.

### Próximo passo sugerido
- Implementar Sprint V2 com foco em robustez do ciclo assíncrono e recuperação automática segura.

### Handoff para a próxima etapa
- Prioridade imediata: endurecer regras de claim/heartbeat/timeout/retry no fluxo de render.
- O que não deve ser refeito: contrato de integração backend ↔ módulo de vídeo já consolidado neste ciclo.
- Riscos abertos: drift de estado entre provider externo e backend; backlog por falhas intermitentes sem auto-recuperação.
- Dependências externas: credenciais/provider real e ambiente de staging com conectividade validada.
- Onde continuar: `docs/novos-modulos/avatar/avatar-sales-video-restart-plan.md` e `docs/novos-modulos/avatar/avatar-sales-video-integration-swagger.yaml`.

## 2026-04-16 — Sprint V1 (implementação do adapter real e integração backend)

**Status:** parcialmente concluída

### Resumo
- Foi implementado um adapter `real` no `video-management-service`, preservando o `stub` como fallback de desenvolvimento.
- O fluxo real passou a suportar `submit`, polling, download de artefatos e devolução de metadata mínima para o backend.
- Falhas do provider e expiração foram normalizadas para os endpoints internos canônicos do backend.

### O que foi implementado
- Novo provider `RealVideoProvider` com:
  - seleção por `providerName`;
  - criação de job no provider externo;
  - polling de status com timeout configurável;
  - download de vídeo/poster/legenda por URL;
  - metadata com `provider`, `provider_job_id` e status externo final.
- Ampliação do `BackendVideoClient` para:
  - `heartbeat` (`POST /internal/video/jobs/{jobId}/heartbeat`);
  - `expired` (`POST /internal/video/jobs/{jobId}/expired`).
- Atualização do `VideoJobProcessor` para:
  - mapear `PROVIDER_ASSET_EXPIRED` para endpoint de expiração;
  - manter tradução de falhas técnicas em `failureCode`.
- Configuração de staging com backend `http://191.252.181.168:8000`.

### O que foi alterado
- Arquivos:
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/service/provider/RealVideoProvider.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/service/provider/VideoProviderException.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/service/VideoJobProcessor.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/service/VideoJobProgressReporter.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/client/BackendVideoClient.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/client/payload/JobHeartbeatPayload.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/client/payload/JobExpirationPayload.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/config/VideoManagementProperties.java`
  - `video-management-service/src/main/resources/application.yml`
  - `video-management-service/README.md`
  - `docs/novos-modulos/avatar/avatar-sales-video-integration-swagger.yaml`
  - `docs/novos-modulos/avatar/avatar-sales-video-restart-plan.md`
- Módulos:
  - `video-management-service`
  - documentação canônica do módulo Avatar Sales Video
- Endpoints/contratos:
  - `/internal/video/jobs/{jobId}/heartbeat`
  - `/internal/video/jobs/{jobId}/expired`
  - `/internal/video/jobs/{jobId}/progress`
  - `/internal/video/jobs/{jobId}/complete`
  - `/internal/video/jobs/{jobId}/fail`

### Contratos e artefatos afetados
- `avatar.salesVideoRenderJob.v1` (campos `providerName`, `providerJobId`, estados do ciclo assíncrono).
- `avatar.salesVideoProviderExecution.v1` (metadata técnica de provider devolvida ao backend).
- Payloads de job interno para heartbeat e expiração.

### Testes e validações executados
- Build e suíte de testes do `video-management-service`.
- Revisão de compatibilidade do client com os endpoints OpenAPI da integração backend ↔ módulo de vídeo.

### Limitações e pendências
- Sem credenciais reais no repositório para validar provider externo contra ambiente staging.
- Contrato JSON do provider real pode exigir ajuste fino por vendor (nomes de campos de status/URL).
- Sem observabilidade consolidada de métricas/alertas (escopo Sprint V3).

### Próximo passo sugerido
- Executar validação E2E em staging com credenciais reais, cobrindo sucesso/falha/expiração e aferindo tempos de polling.

### Handoff para a próxima etapa
- Prioridade imediata: Sprint V2 (robustez de timeout/retry/deduplicação) + validação E2E do provider real.
- O que não deve ser refeito: contrato de endpoints internos `/internal/video/jobs/*` e fluxo canônico backend como fonte de verdade.
- Riscos abertos: campos de status divergentes entre providers reais e parser atual; timeout insuficiente para renders longos.
- Dependências externas: credenciais e documentação final do provider real por ambiente.
- Onde continuar: `video-management-service/src/main/java/com/marketinghub/videomanagement/service/provider/RealVideoProvider.java`.
