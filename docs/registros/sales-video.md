# Registro operacional — Sales Video

## 2026-07-23 — Player de streaming adaptativo para vídeo comercial

- Problema observado: entregar MP4 bruto direto para a cliente aumenta tempo de início, consumo de banda e risco de abandono no celular.
- Causa-raiz tratada: o contrato de conclusão do render só expunha asset bruto, sem URL publicável de streaming adaptativo.
- Correção preparada: `sales_video_job` passou a persistir `stream_playback_url`; o callback de conclusão aceita `streamPlaybackUrl`; os DTOs expõem essa URL; Marketing Hub e PDE priorizam HLS adaptativo com MP4 como fallback.
- Regra operacional: vídeo bruto/renderizado é ativo de auditoria e contingência; a experiência principal da usuária deve usar stream adaptativo sempre que houver URL processada pelo pipeline de mídia.

## 2026-07-21 — Vídeos de entrada do PDE pelo Marketing Hub

- Problema observado: a nova área `Vídeos` existia como planejamento local no navegador, mas isso não criava artefato rastreável no Marketing Hub.
- Causa-raiz: a tela usava `localStorage` como fonte de verdade e não acionava o módulo canônico `sales-video`.
- Correção preparada: a tela `/videos` passou a selecionar produto, criar perfil de vídeo do PDE, salvar roteiro aprovado e solicitar job de criação pelo backend do Marketing Hub.
- Regra operacional: código de vídeo muda via GitHub; artefato comercial de vídeo para PDE deve nascer e evoluir pelo Marketing Hub, preservando perfil, roteiro, job, asset e versão comercial associada.

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
