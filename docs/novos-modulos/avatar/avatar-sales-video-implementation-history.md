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
- 2026-04-16 — Sprint V1 (contrato de integração e atualização de planejamento)

---

## Entradas

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
