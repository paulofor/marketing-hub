# Registro operacional — Sales Video

## 2026-07-02 — Correção de truncamento em evento de retry

- Problema observado: backend saudável, mas logs com `Data truncated for column 'event_type' at row 1` durante `SalesVideoAutoRetryScheduler`.
- Histórico consultado: `docs/diagnostics/ai-worker-jobs-log-check-2026-05-01.md` já apontava suspeita de incompatibilidade entre código e banco.
- Confirmação via MCP: `sales_video_job_event.event_type` no banco real estava como `ENUM` sem o valor `RETRIED`; o código atual grava `SalesVideoJobEventType.RETRIED`.
- Causa-raiz: schema real ficou preso em contrato antigo de enum, enquanto entidade JPA e changelog fundacional atuais esperam `VARCHAR(64)`.
- Correção preparada: Liquibase `sales-video-hardening-007-event-type-varchar` converte `sales_video_job_event.event_type` para `VARCHAR(64) NOT NULL`, mantendo compatibilidade com eventos futuros sem precisar alterar enum físico no banco.
