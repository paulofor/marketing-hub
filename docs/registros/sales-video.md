# Registro operacional — Sales Video

## 2026-07-02 — Correção de truncamento em evento de retry

- Problema observado: backend saudável, mas logs com `Data truncated for column 'event_type' at row 1` durante `SalesVideoAutoRetryScheduler`.
- Histórico consultado: `docs/diagnostics/ai-worker-jobs-log-check-2026-05-01.md` já apontava suspeita de incompatibilidade entre código e banco.
- Confirmação via MCP: `sales_video_job_event.event_type` no banco real estava como `ENUM` sem o valor `RETRIED`; o código atual grava `SalesVideoJobEventType.RETRIED`.
- Causa-raiz: schema real ficou preso em contrato antigo de enum, enquanto entidade JPA e changelog fundacional atuais esperam `VARCHAR(64)`.
- Correção preparada: Liquibase `sales-video-hardening-007-event-type-varchar` converte `sales_video_job_event.event_type` para `VARCHAR(64) NOT NULL`, mantendo compatibilidade com eventos futuros sem precisar alterar enum físico no banco.

## 2026-07-09 — Separação de roteiro OpenAI, render VEO e entrega por R2

- Decisão operacional: o roteiro comercial deve ser gerado pelo `ai-worker` com OpenAI, enquanto a produção/renderização do vídeo fica no `video-management-service` ou módulo externo equivalente conectado ao Gemini/VEO.
- Regra de entrega: o MP4 final deve permanecer no Cloudflare R2/CDN; o Marketing Hub deve trafegar e persistir referência do asset, URL pública/streaming, provider job id e metadados, sem mover o binário entre módulos depois do upload.
- Experiência de página: a landing deve reproduzir o vídeo como streaming/progressivo em `<video>` ou player equivalente; link simples para download/aba externa não atende ao fluxo comercial.
- Cânone atualizado: `docs/canonical/avatar-sales-video-canonical-rules.md`.
