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
- 2026-04-17 — Sprint V7 (camada comercial inicial com playbooks e eventos de conversão)
- 2026-04-17 — Hotfix backend (compliance snapshot + normalização de executionMode no request-render)
- 2026-04-17 — Sprint V6 (rollout controlado por tenant/perfil e atualização de contrato)
- 2026-04-17 — Sprint V5 (validação E2E administrativa, compliance UI e cobertura backend)
- 2026-04-17 — Sprint V4 (compliance, consentimento e governança)
- 2026-04-16 — Sprint V3 (observabilidade e confiabilidade operacional)
- 2026-04-16 — Sprint V2 (robustez do ciclo assíncrono e recuperação de órfãos)
- 2026-04-16 — Sprint V1 (contrato de integração e atualização de planejamento)
- 2026-04-16 — Sprint V1 (implementação do adapter real e integração backend)

---

## Entradas

## 2026-04-17 — Sprint V7 (camada comercial inicial com playbooks e eventos de conversão)

**Status:** concluída com pendências

### Resumo
- Implementada a primeira camada comercial da Sprint V7 no backend com persistência canônica de playbooks e eventos de conversão por perfil.
- O módulo passou a expor endpoints para registrar fatos de conversão e gerar resumo comparativo por script/provider.
- O Swagger canônico foi atualizado para refletir os novos contratos de integração do módulo com a base via backend.

### O que foi implementado
- Nova entidade `sales_video_commercial_playbook` para registrar objeções e CTA por nicho/variação.
- Nova entidade `sales_video_conversion_event` para registrar eventos de conversão (`VIEW`, `LEAD`, `QUALIFIED_LEAD`, `CHECKOUT_STARTED`, `PURCHASE`) vinculáveis a perfil/job/script.
- Serviço `SalesVideoCommercialInsightsService` com:
  - criação e listagem de playbooks;
  - ingestão de eventos de conversão;
  - resumo de performance comercial por variação técnica (`scriptId` + `providerName`) com agregados de leads, purchases e receita.
- Novos endpoints:
  - `POST /api/sales-videos/profiles/{profileId}/commercial-playbooks`
  - `GET /api/sales-videos/profiles/{profileId}/commercial-playbooks`
  - `POST /api/sales-videos/profiles/{profileId}/conversion-events`
  - `GET /api/sales-videos/profiles/{profileId}/performance-summary`

### O que foi alterado
- Arquivos:
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/SalesVideoCommercialPlaybook.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/SalesVideoConversionEvent.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/SalesVideoConversionEventType.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/service/SalesVideoCommercialInsightsService.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/web/SalesVideoCommercialController.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/dto/CreateSalesVideoCommercialPlaybookRequest.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/dto/SalesVideoCommercialPlaybookDto.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/dto/CreateSalesVideoConversionEventRequest.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/dto/SalesVideoConversionEventDto.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/dto/SalesVideoPerformanceSummaryDto.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/dto/SalesVideoVariantPerformanceDto.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/repository/SalesVideoCommercialPlaybookRepository.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/repository/SalesVideoConversionEventRepository.java`
  - `backend/ads-service/src/main/resources/db/changelog/changesets/2037-04-17-sales-video-commercial-insights.yaml`
  - `backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml`
  - `docs/swagger/avatar-sales-video-integration-swagger.yaml`
  - `docs/novos-modulos/avatar/avatar-sales-video-restart-plan.md`
  - `docs/modelo-dados-experimento.md`
  - `docs/novos-modulos/avatar/avatar-sales-video-canonical-artifacts-initial.md`
  - `docs/novos-modulos/avatar/avatar-sales-video-implementation-history.md`
- Módulos:
  - `backend/ads-service`
  - documentação canônica do módulo Avatar Sales Video
- Endpoints/contratos:
  - contratos comerciais da Sprint V7 adicionados ao Swagger canônico.

### Contratos e artefatos afetados
- `avatar.salesVideoCommercialPlaybook.v1` (novo artefato canônico inicial para variações de objeção/CTA por nicho).
- `avatar.salesVideoConversionEvent.v1` (novo artefato canônico inicial para fatos de conversão vinculados ao perfil/job/script).
- `avatar.salesVideoPerformanceSummary.v1` (projeção agregada para revisão comercial inicial).

### Testes e validações executados
- `cd backend/ads-service && mvn -s ../settings.xml test -Dtest=SalesVideoCommercialInsightsServiceTest`.
- revisão de consistência do contrato OpenAPI em `docs/swagger/avatar-sales-video-integration-swagger.yaml`.

### Limitações e pendências
- painel administrativo no frontend para operar playbooks e leitura do resumo comercial ainda não foi implementado.
- ingestão automática de conversão a partir da landing/checkout ainda depende de integração operacional em módulos consumidores.
- validação E2E em staging (`191.252.181.168`) continua dependente de credenciais operacionais e janela assistida.

### Próximo passo sugerido
- Executar Sprint V8 focando em automação de ingestão de conversões, painel de revisão comercial e rotina semanal de aprendizado por tenant/perfil.

### Handoff para a próxima etapa
- Prioridade imediata: integrar emissão de eventos de conversão dos pontos de contato reais ao endpoint canônico do backend.
- O que não deve ser refeito: modelagem de entidades comerciais da Sprint V7 e contratos básicos de playbook/performance-summary.
- Riscos abertos: sem automação de ingestão, o resumo comercial pode ficar incompleto e enviesado.
- Dependências externas: credenciais de staging, headers de tenant e integração com fontes reais de conversão.
- Onde continuar: `backend/ads-service` (integrações de ingestão e agregações), frontend (painel comercial) e docs do plano de reinício.

## 2026-04-17 — Hotfix backend (compliance snapshot + normalização de executionMode no request-render)

**Status:** concluída

### Resumo
- Corrigido erro de build no `backend/ads-service` causado por falha na serialização do snapshot de auditoria (`auditSnapshotJson`) em render `PRODUCTION`.
- Adicionada proteção defensiva para `executionMode` quando a normalização de rollout retornar `null`, evitando NPE durante a criação do job.
- Mantida aderência ao contrato canônico: persistência via backend e governança de render produtivo com compliance.

### O que foi implementado
- `ObjectMapper` do `SalesVideoProfileService` migrado para `JsonMapper.builder().findAndAddModules().build()` para suportar serialização de tipos Java Time (`Instant`) no snapshot de auditoria.
- Fallback explícito de `executionMode` em `requestRender`: quando `rolloutService.normalizeExecutionMode(...)` retornar `null`, o serviço passa a usar o modo solicitado no request e, na ausência deste, `TEST`.
- Mantido fluxo de bloqueio de `PRODUCTION` por rollout/compliance sem alterar o contrato de endpoint.

### O que foi alterado
- Arquivos:
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/service/SalesVideoProfileService.java`
  - `docs/novos-modulos/avatar/avatar-sales-video-implementation-history.md`
- Módulos:
  - `backend/ads-service`
  - documentação canônica do módulo Avatar Sales Video
- Endpoints/contratos:
  - `POST /api/sales-videos/profiles/{profileId}/request-render` (mesmo contrato, com robustez interna no tratamento de `executionMode`)

### Contratos e artefatos afetados
- `avatar.salesVideoRenderJob.v1` (snapshot auditável em render produtivo com serialização estável).
- `RequestVideoRenderRequest.executionMode` (normalização defensiva sem alteração de schema).

### Testes e validações executados
- `cd backend/ads-service && mvn -s ../settings.xml test -Dtest=SalesVideoProfileServiceTest`.

### Limitações e pendências
- Hotfix cobre o erro de build unitário; validação E2E em staging com provider real continua pendente conforme plano de reinício.
- Baseline operacional de rollout/compliance em ambiente compartilhado segue dependente de credenciais e janela operacional do time.

### Próximo passo sugerido
- Executar bateria E2E de staging para validar os cenários de sucesso/falha/timeout/asset expirado após este hotfix.

### Handoff para a próxima etapa
- Prioridade imediata: validar no staging compartilhado o fluxo `PRODUCTION` completo com compliance aprovado.
- O que não deve ser refeito: gate de rollout/compliance e contrato de endpoint permanecem os mesmos.
- Riscos abertos: sem E2E real, ainda há risco de divergência entre provider e estado canônico do backend.
- Dependências externas: credenciais do provider real, tenant/header de staging e disponibilidade do backend `191.252.181.168`.
- Onde continuar: `backend/ads-service` (testes de integração) e `video-management-service` (validação operacional E2E).


## 2026-04-17 — Sprint V6 (rollout controlado por tenant/perfil e atualização de contrato)

**Status:** concluída com pendências

### Resumo
- A Sprint V6 implementou o gate canônico de rollout no backend para liberar render produtivo de forma gradual por tenant e por perfil.
- O módulo passou a expor endpoints de leitura de elegibilidade para reduzir decisões manuais durante operação assistida.
- O Swagger de integração foi atualizado para refletir os novos contratos operacionais de rollout.

### O que foi implementado
- Serviço de política de rollout (`SalesVideoRolloutService`) com regras de elegibilidade por `tenantId` e `profileId`.
- Bloqueio explícito de `request-render` em `PRODUCTION` quando rollout não estiver habilitado para o escopo solicitado, retornando `ROLLOUT_NOT_ALLOWED`.
- Novos endpoints administrativos:
  - `GET /api/sales-videos/rollout/status`
  - `GET /api/sales-videos/profiles/{profileId}/rollout-status`
- Inclusão de flags de configuração no backend:
  - `sales-video.rollout.enabled`
  - `sales-video.rollout.allowed-tenants`
  - `sales-video.rollout.allowed-profile-ids`
- Cobertura de teste no `SalesVideoProfileServiceTest` para cenário de bloqueio por rollout.

### O que foi alterado
- Arquivos:
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/service/SalesVideoRolloutService.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/dto/SalesVideoRolloutStatusDto.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/service/SalesVideoProfileService.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/web/SalesVideoProfileController.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/exception/VideoModuleErrorCode.java`
  - `backend/ads-service/src/main/resources/application.properties`
  - `backend/ads-service/src/test/java/com/marketinghub/salesvideo/service/SalesVideoProfileServiceTest.java`
  - `docs/swagger/avatar-sales-video-integration-swagger.yaml`
  - `docs/novos-modulos/avatar/avatar-sales-video-restart-plan.md`
  - `docs/novos-modulos/avatar/avatar-sales-video-implementation-history.md`
- Módulos:
  - `backend/ads-service`
  - documentação canônica do módulo Avatar Sales Video
- Endpoints/contratos:
  - `POST /api/sales-videos/profiles/{profileId}/request-render` (gate de rollout para `PRODUCTION`)
  - `GET /api/sales-videos/rollout/status`
  - `GET /api/sales-videos/profiles/{profileId}/rollout-status`

### Contratos e artefatos afetados
- `avatar.salesVideoRenderJob.v1` (governança de liberação por rollout antes da criação de render produtivo).
- `SalesVideoRolloutStatusDto` no Swagger canônico para consulta de elegibilidade operacional.
- `VideoModuleErrorCode` expandido com `ROLLOUT_NOT_ALLOWED`.

### Testes e validações executados
- `cd backend/ads-service && mvn -s ../settings.xml test -Dtest=SalesVideoProfileServiceTest`.
- revisão de consistência do Swagger de integração com os endpoints implementados no backend.

### Limitações e pendências
- validação E2E completa com provider real em staging (`191.252.181.168`) ainda depende de credenciais/tenant operacionais.
- baseline diário real de rollout (sucesso/falha/latência/retry por tenant/provider) ainda não foi coletado na stack compartilhada.
- runbook de rollback ainda precisa ser exercitado em simulação operacional com o time.

### Próximo passo sugerido
- Iniciar Sprint V7 após executar rollout piloto assistido em staging, registrar baseline real e validar rollback por flags sem intervenção em banco.

### Handoff para a próxima etapa
- Prioridade imediata: rodar ciclo piloto com tenant/perfil autorizados e registrar baseline por 3-5 dias operacionais.
- O que não deve ser refeito: gate de rollout no backend e contratos de status já publicados no Swagger.
- Riscos abertos: sem baseline real, alertas podem continuar descalibrados no primeiro rollout.
- Dependências externas: credenciais do provider real, janela de operação em staging e stack de observabilidade compartilhada.
- Onde continuar: `backend/ads-service` (governança operacional), `video-management-service` (monitoramento do ciclo assíncrono), documentação da Sprint V7.

## 2026-04-17 — Sprint V5 (validação E2E administrativa, compliance UI e cobertura backend)

**Status:** concluída com pendências

### Resumo
- A Sprint V5 atacou pendências remanescentes das Sprints V4/V5 com foco em aderência ponta a ponta do fluxo administrativo ao contrato canônico.
- O frontend passou a operar o checklist de compliance diretamente no endpoint canônico do backend.
- A cobertura de testes de serviço no backend foi ampliada para bloquear render produtivo sem compliance e validar snapshot auditável.

### O que foi implementado
- Integração frontend do endpoint `PATCH /api/sales-videos/profiles/{profileId}/compliance` com mutation dedicada.
- Nova seção de checklist de compliance na tela de detalhe do perfil para consentimento, revisão humana e notas.
- Ajuste do formulário de render para enviar `executionMode` explícito (`TEST`/`PRODUCTION`) conforme Swagger.
- Expansão das tipagens frontend para refletir campos de compliance e auditoria (`executionMode`, `auditSnapshotJson`, metadados de compliance).
- Novos testes unitários no backend (`SalesVideoProfileServiceTest`) cobrindo:
  - bloqueio de `PRODUCTION` sem compliance;
  - geração de `auditSnapshotJson` quando compliance está completo;
  - limpeza de campos de consentimento ao desativar exigência.

### O que foi alterado
- Arquivos:
  - `frontend/src/api/salesVideo/types.ts`
  - `frontend/src/api/salesVideo/useUpdateSalesVideoCompliance.ts`
  - `frontend/src/pages/salesVideo/SalesVideoProfileDetailPage.tsx`
  - `backend/ads-service/src/test/java/com/marketinghub/salesvideo/service/SalesVideoProfileServiceTest.java`
  - `docs/novos-modulos/avatar/avatar-sales-video-restart-plan.md`
  - `docs/novos-modulos/avatar/avatar-sales-video-implementation-history.md`
- Módulos:
  - `frontend`
  - `backend/ads-service` (camada de testes)
  - documentação canônica do módulo Avatar Sales Video
- Endpoints/contratos:
  - `PATCH /api/sales-videos/profiles/{profileId}/compliance`
  - `POST /api/sales-videos/profiles/{profileId}/request-render` (campo `executionMode`)

### Contratos e artefatos afetados
- `avatar.salesVideoComplianceRecord.v1` (operação via UI administrativa conectada ao backend canônico).
- `avatar.salesVideoRenderJob.v1` (execução com `executionMode` explícito e snapshot de auditoria validado por testes).
- `RequestVideoRenderRequest`, `UpdateSalesVideoComplianceRequest`, `SalesVideoProfileDto` e `SalesVideoJobDto`.

### Testes e validações executados
- `cd backend/ads-service && mvn -s ../settings.xml test -Dtest=SalesVideoProfileServiceTest,SalesVideoJobServiceTest`.
- `cd frontend && npm run build`.
- `cd frontend && npm run test -- --runInBand`.

### Limitações e pendências
- validação E2E integral contra o backend staging `191.252.181.168` não foi concluída nesta sprint devido dependências externas (headers/credenciais/provider real).
- cenários de timeout/falha/expiração com provider real permanecem para bateria E2E de staging.
- dashboards/alertas de compliance ainda dependem de provisionamento na stack de observabilidade compartilhada.

### Próximo passo sugerido
- Executar Sprint V6 com rollout controlado apenas após concluir a bateria E2E integral em staging e consolidar baseline operacional.

### Handoff para a próxima etapa
- Prioridade imediata: concluir E2E em staging para sucesso, falha, timeout, retry, asset expirado e publicação final.
- O que não deve ser refeito: integração de compliance no frontend e testes de snapshot auditável já implementados.
- Riscos abertos: dependência de provider real/credenciais pode impedir validação completa; limiares de alerta sem baseline ainda podem gerar ruído.
- Dependências externas: backend staging (`191.252.181.168`), credenciais de provider real e stack de observabilidade compartilhada.
- Onde continuar: `video-management-service` (validação operacional), `backend/ads-service` (rollout/flags) e documentação de runbook da Sprint V6.

## 2026-04-17 — Sprint V4 (compliance, consentimento e governança)

**Status:** concluída com pendências

### Resumo
- A Sprint V4 implementou o checklist mínimo de compliance no backend e passou a bloquear render/publish produtivo sem pré-condições obrigatórias.
- O fluxo de render recebeu separação explícita por modo de execução (`TEST` vs `PRODUCTION`) para impedir publicação produtiva sem governança.
- Foi adicionada trilha auditável no job com snapshot de script/provider/model/prompt/consentimento no momento da solicitação.

### O que foi implementado
- Novos campos de compliance no `SalesVideoProfile` para consentimento auditável e revisão humana obrigatória.
- Endpoint de administração `PATCH /api/sales-videos/profiles/{profileId}/compliance` para atualizar checklist mínimo de governança.
- Validação de compliance no `request-render`: modo `PRODUCTION` exige consentimento (quando aplicável) e revisão humana concluída.
- Bloqueio de publicação em `LandingVideoSlotService` sem checklist mínimo de compliance.
- Inclusão de `executionMode` e `auditSnapshotJson` em `SalesVideoJob` para rastreabilidade operacional e auditoria posterior.
- Novo changelog Liquibase incremental para persistência de compliance e governança no MySQL 5.7.

### O que foi alterado
- Arquivos:
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/SalesVideoExecutionMode.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/SalesVideoProfile.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/SalesVideoJob.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/dto/RequestVideoRenderRequest.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/dto/UpdateSalesVideoComplianceRequest.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/dto/SalesVideoProfileDto.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/dto/SalesVideoJobDto.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/mapper/SalesVideoMapper.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/exception/VideoModuleErrorCode.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/service/SalesVideoProfileService.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/service/SalesVideoJobService.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/service/LandingVideoSlotService.java`
  - `backend/ads-service/src/main/java/com/marketinghub/salesvideo/web/SalesVideoProfileController.java`
  - `backend/ads-service/src/main/resources/db/changelog/changesets/2037-04-17-sales-video-compliance-governance.yaml`
  - `backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml`
  - `docs/swagger/avatar-sales-video-integration-swagger.yaml`
  - `docs/novos-modulos/avatar/avatar-sales-video-restart-plan.md`
  - `docs/novos-modulos/avatar/avatar-sales-video-implementation-history.md`
- Módulos:
  - `backend/ads-service`
  - documentação canônica do módulo Avatar Sales Video
- Endpoints/contratos:
  - `PATCH /api/sales-videos/profiles/{profileId}/compliance`
  - `POST /api/sales-videos/profiles/{profileId}/request-render` (campo `executionMode` com bloqueio de compliance)
  - `GET /api/sales-videos/profiles/{profileId}` e DTOs internos com campos adicionais de compliance/auditoria

### Contratos e artefatos afetados
- `avatar.salesVideoComplianceRecord.v1` (materializado via campos canônicos de consentimento e revisão humana no perfil).
- `avatar.salesVideoRenderJob.v1` (novos campos `executionMode` e `auditSnapshotJson` para rastreabilidade de governança).
- `avatar.landingVideoSlotHistorySnapshot.v1` (publicação condicionada ao checklist de compliance no backend).

### Testes e validações executados
- `mvn -s ../settings.xml test` no `backend/ads-service`.
- validação estática de consistência entre contrato OpenAPI e DTOs/serviços atualizados.
- revisão de aderência do plano de sprint e protocolo de histórico após implementação.

### Limitações e pendências
- ainda sem fluxo de UI dedicado para atualizar compliance no frontend administrativo.
- ausência de validação E2E em staging com o backend `191.252.181.168` cobrindo bloqueios de compliance.
- política de consentimento ainda não contém taxonomia detalhada de evidência por tipo de avatar/jurisdição.

### Próximo passo sugerido
- Executar Sprint V5 com foco em validação E2E dos bloqueios e operação real em staging.

### Handoff para a próxima etapa
- Prioridade imediata: executar bateria E2E em staging para os cenários `TEST` e `PRODUCTION` com e sem compliance.
- O que não deve ser refeito: modelagem base de compliance no perfil e bloqueios backend de render/publicação já implementados.
- Riscos abertos: ausência de UI pode induzir operação manual por API; falta de validação E2E pode esconder edge-cases.
- Dependências externas: disponibilidade do backend staging (`191.252.181.168`) e credenciais do provider real.
- Onde continuar: `backend/ads-service` (testes de integração), `frontend` (painel de compliance) e runbook operacional de staging.

## 2026-04-16 — Sprint V3 (observabilidade e confiabilidade operacional)

**Status:** concluída com pendências

### Resumo
- A Sprint V3 instrumentou observabilidade mínima no `video-management-service` com métricas, correlação de logs e exposição Prometheus.
- O contrato de falha backend ↔ módulo de vídeo foi alinhado ao Swagger canônico com `retryable` e `retryReason`.
- Dashboards e alertas foram definidos como baseline documental, permanecendo a implantação na stack compartilhada como pendência operacional.

### O que foi implementado
- Instrumentação de métricas operacionais para dispatch, conclusão, falha, expiração, conflito de claim, retry backend, recuperação de órfãos, backlog e latência total de render.
- Inclusão de contexto de correlação (`jobId`, `profileId`, `provider`, `providerJobId`, `tenant`) no MDC do processamento de jobs.
- Padronização do `logging.pattern.level` para emitir os campos de correlação em todos os logs do módulo durante execução do job.
- Exposição de métricas em `/actuator/prometheus` via Micrometer Prometheus Registry.
- Alinhamento do payload de falha para incluir `retryable` e `retryReason` em conformidade com `JobFailureRequest` do OpenAPI canônico.

### O que foi alterado
- Arquivos:
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/service/VideoJobObservabilityService.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/service/VideoJobProcessor.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/service/VideoJobPoller.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/client/BackendVideoClient.java`
  - `video-management-service/src/main/java/com/marketinghub/videomanagement/client/payload/JobFailurePayload.java`
  - `video-management-service/src/main/resources/application.yml`
  - `video-management-service/pom.xml`
  - `video-management-service/README.md`
  - `video-management-service/src/test/java/com/marketinghub/videomanagement/service/VideoJobProcessorTest.java`
  - `docs/novos-modulos/avatar/avatar-sales-video-restart-plan.md`
  - `docs/novos-modulos/avatar/avatar-sales-video-implementation-history.md`
- Módulos:
  - `video-management-service`
  - documentação canônica do módulo Avatar Sales Video
- Endpoints/contratos:
  - `/internal/video/jobs/{jobId}/fail` (payload alinhado com `retryable` + `retryReason`)
  - `/actuator/prometheus` (telemetria operacional)

### Contratos e artefatos afetados
- `avatar.salesVideoRenderJob.v1` (rastreabilidade operacional de retries, falhas e latência do ciclo).
- `avatar.salesVideoJobEvent.v1` (correlação de logs e eventos por chaves de contexto).
- `JobFailureRequest`/`JobFailurePayload` com `retryable` e `retryReason` como campos explícitos no fluxo de falha.

### Testes e validações executados
- `mvn test` no `video-management-service` com ajuste dos testes de `VideoJobProcessor`.
- `mvn package -DskipTests` no `video-management-service` para validar build com novo registry Prometheus.
- Revisão de consistência entre documentação de sprint, histórico e contrato OpenAPI do módulo.

### Limitações e pendências
- Dashboards e alertas ainda não foram provisionados no ambiente compartilhado de observabilidade (Grafana/Alertmanager).
- Limiar de alertas ainda depende de baseline real em staging com carga concorrente.
- Validação E2E dos alarmes em cenários de degradação permanece para próximos ciclos.

### Próximo passo sugerido
- Iniciar Sprint V4 (compliance/consentimento), mantendo em paralelo a materialização dos dashboards e alertas definidos na Sprint V3.

### Handoff para a próxima etapa
- Prioridade imediata: implementar bloqueios de workflow para compliance e trilha auditável de consentimento/publicação.
- O que não deve ser refeito: métricas base, MDC de correlação, alinhamento de payload de falha e retry técnico do backend.
- Riscos abertos: alertas sem calibração em produção podem ter ruído; compliance ainda não bloqueia render produtivo.
- Dependências externas: stack de observabilidade compartilhada para dashboards/alertas e staging com provider real.
- Onde continuar: `backend/ads-service` (regras de compliance), `video-management-service` (calibração de métricas) e documentação canônica de Sprint V4.

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
  - `docs/swagger/avatar-sales-video-integration-swagger.yaml`
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
  - `docs/swagger/avatar-sales-video-integration-swagger.yaml`
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
- Onde continuar: `docs/novos-modulos/avatar/avatar-sales-video-restart-plan.md` e `docs/swagger/avatar-sales-video-integration-swagger.yaml`.

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
  - `docs/swagger/avatar-sales-video-integration-swagger.yaml`
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
