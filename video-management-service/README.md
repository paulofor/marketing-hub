# video-management-service

Módulo dedicado ao gerenciamento técnico do ciclo de vida de vídeos que não utilizam o OpenAI diretamente.

## Funcionalidades entregues nesta sprint

- Aplicação Spring Boot 3 / Java 21 com scheduler interno para polling assíncrono.
- Configuração externa de URL/base do backend, token opcional, identificador do worker e parâmetros do poller.
- Cliente REST para todos os endpoints internos de jobs (`claim`, `progress`, `complete`, `fail`).
- Upload automático de vídeo, poster e legendas através do novo endpoint interno `/internal/video/assets` do backend.
- Provider `stub` que lê o script aprovado do perfil, gera artefatos fictícios (MP4, PNG e VTT) e reporta progresso durante o pipeline.
- Provider `real` com integração HTTP configurável (request render + polling + download), com normalização de falhas/expiração para o backend.
- Provider `veo` com integração direta à Gemini API para jobs `providerName=VEO`, incluindo criação da operação, polling e download do MP4 final.
- Dispatcher multi-thread com controle contra concorrência duplicada.
- Poller interno que consome jobs `RENDER` com `provider_family=EXTERNAL_VIDEO_MODULE` e envia para o dispatcher.

## Execução local

```bash
mvn spring-boot:run
```

Para acompanhar os logs dos jobs, mantenha `video.jobs.polling-enabled=true` e verifique as entradas `VideoJobPoller`/`VideoJobProcessor`.

## Configurações principais

| Propriedade | Descrição | Padrão |
|-------------|-----------|--------|
| `video.backend-base-url` | URL interna do `backend/ads-service`. | `http://backend:8000` |
| `video.worker-id` | Identificador enviado no `claim`/`heartbeat`. | `vm-service-local` |
| `video.auth-token` | Token (Bearer) opcional para autenticação mútua. | vazio |
| `video.jobs.polling-enabled` | Liga/desliga o poller automático. | `false` no `application.yml`; `true` no compose operacional |
| `video.jobs.poll-interval` | Intervalo em ISO-8601 (ex.: `PT30S`). | `PT30S` |
| `video.jobs.batch-size` | Número máximo de jobs por ciclo. | `10` |
| `video.providers.real.enabled` | Habilita adapter do provider real. | `false` |
| `video.providers.real.accepted-names` | Valores de `providerName` que ativam o provider real genérico. | `REAL,HEYGEN,SYNTHESIA,VEO` |
| `video.providers.real.base-url` | Base URL da API do provider real. | `http://real-video-provider:8080` |
| `video.providers.real.create-path` | Path para criar render no provider. | `/v1/renders` |
| `video.providers.real.status-path-template` | Path para consultar status do render por `providerJobId`. | `/v1/renders/{providerJobId}` |
| `video.providers.real.poll-interval` | Intervalo de polling do status no provider. | `PT5S` |
| `video.providers.real.max-poll-attempts` | Máximo de tentativas de polling antes de timeout técnico. | `120` |
| `video.providers.veo.enabled` | Habilita o adapter direto VEO/Gemini. | `false` |
| `video.providers.veo.api-key` | Chave Gemini usada pelo adapter VEO. | `${GEMINI_API_KEY}` ou arquivo em `${GEMINI_API_KEY_FILE}` |
| `video.providers.veo.model` | Modelo VEO usado para gerar vídeo. | `veo-3.1-generate-preview` |
| `video.providers.veo.duration-seconds` | Duração numérica enviada ao VEO. | `8` |

### Nota operacional sobre VEO

O VEO ja foi validado para experimentos do Marketing Hub. Desde 2026-07-10, este modulo tambem possui adapter direto `veo`, acionado por `providerName=VEO` quando `video.providers.veo.enabled=true` e a chave Gemini esta configurada por `GEMINI_API_KEY` ou pelo arquivo `GEMINI_API_KEY_FILE`.

O adapter direto usa o contrato REST da Gemini API: cria uma operacao `predictLongRunning`, faz polling em `operations/...` e baixa o MP4 retornado. O fluxo manual continua sendo fallback operacional quando nao houver chave Gemini ou quando o provider externo estiver indisponivel.

No container, o compose monta por padrao `/root/infra/gemini-token/gemini_api_key` em `/run/secrets/gemini_api_key:ro`; o entrypoint carrega esse arquivo para `GEMINI_API_KEY` antes de iniciar a aplicacao.

## Observabilidade (Sprint V3)

O módulo expõe métricas em `/actuator/prometheus` com os principais indicadores operacionais:

- `video_jobs_dispatched_total`
- `video_jobs_completed_total`
- `video_jobs_failed_total` (tag `failure_code`)
- `video_render_latency_seconds`
- `video_jobs_backlog` (tag `status=VIDEO_REQUESTED|VIDEO_PROCESSING`)
- `video_backend_retry_total` (tags `operation` e `status`)
- `video_jobs_claim_conflict_total`
- `video_jobs_orphan_recovery_total`
- `video_jobs_asset_expired_total`

Os logs também passam a incluir correlação por MDC com:

- `jobId`
- `profileId`
- `provider`
- `providerJobId`
- `tenant`

### Dashboards/alertas mínimos recomendados

- **Backlog**: painel com `video_jobs_backlog` por status.
- **Confiabilidade**: taxa de falhas e retries por provider.
- **Latência**: p95/p99 de `video_render_latency_seconds`.
- **Alertas**:
  - backlog `VIDEO_REQUESTED` acima do limiar operacional;
  - aumento de `video_jobs_failed_total`;
  - elevação de `video_backend_retry_total` por 5xx/429.

### Staging atual

- Backend de staging configurado para `http://191.252.181.168:8000`.
- O provider VEO só deve ser habilitado após configurar `GEMINI_API_KEY` real no host/container.

## Construção do container

```bash
docker build -f video-management-service/Dockerfile -t marketinghub-video-management:latest .
```
