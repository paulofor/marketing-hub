# Registro operacional — Sales Video

## 2026-07-12 — Token Gemini via arquivo no container de video

- Problema observado: jobs de video VEO podiam falhar por provider sem token configurado quando o container nao recebia `GEMINI_API_KEY`.
- Causa-raiz tratada: o modulo `video-management-service` dependia de variavel de ambiente direta, mas a operacao real mantem a chave em arquivo no host.
- Correção preparada: compose local e compose de deploy montam `/root/infra/gemini-token/gemini_api_key` como `/run/secrets/gemini_api_key:ro`; o entrypoint carrega o arquivo para `GEMINI_API_KEY` e `VIDEO_PROVIDERS_VEO_API_KEY` antes de iniciar o Spring Boot.
- Protecao adicional: a passagem direta de `GEMINI_API_KEY` pelo compose foi removida para reduzir risco de vazamento em `docker compose config`.

## 2026-07-02 — Correção de truncamento em evento de retry

- Problema observado: backend saudável, mas logs com `Data truncated for column 'event_type' at row 1` durante `SalesVideoAutoRetryScheduler`.
- Histórico consultado: `docs/diagnostics/ai-worker-jobs-log-check-2026-05-01.md` já apontava suspeita de incompatibilidade entre código e banco.
- Confirmação via MCP: `sales_video_job_event.event_type` no banco real estava como `ENUM` sem o valor `RETRIED`; o código atual grava `SalesVideoJobEventType.RETRIED`.
- Causa-raiz: schema real ficou preso em contrato antigo de enum, enquanto entidade JPA e changelog fundacional atuais esperam `VARCHAR(64)`.
- Correção preparada: Liquibase `sales-video-hardening-007-event-type-varchar` converte `sales_video_job_event.event_type` para `VARCHAR(64) NOT NULL`, mantendo compatibilidade com eventos futuros sem precisar alterar enum físico no banco.
