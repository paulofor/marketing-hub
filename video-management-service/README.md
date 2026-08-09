# video-management-service

Módulo dedicado ao gerenciamento técnico do ciclo de vida de vídeos que não utilizam o OpenAI diretamente.

## Funcionalidades entregues nesta sprint

- Aplicação Spring Boot 3 / Java 21 com scheduler interno para polling assíncrono.
- Configuração externa de URL/base do backend, token opcional, identificador do worker e parâmetros do poller.
- Cliente REST para todos os endpoints internos de jobs (`claim`, `progress`, `complete`, `fail`).
- Upload automático de vídeo, poster e legendas através do novo endpoint interno `/internal/video/assets` do backend.
- Provider `stub` que lê o script aprovado do perfil, gera artefatos fictícios (MP4, PNG e VTT) e reporta progresso durante o pipeline.
- Provider `real` com integração HTTP configurável (request render + polling + download), com normalização de falhas/expiração para o backend.
- Provider `luma` com integração direta à Luma Agents API para jobs `providerName=LUMA_RAY_3_2`, gerando três cenas Ray 3.2 de 10s e montando o MP4 final com `ffmpeg`.
- Provider `veo` com integração direta à Gemini API para jobs `providerName=VEO`, incluindo criação da operação, polling e download do MP4 final.
- Providers `kling` e `runway` preparados para cenas curtas image-to-video quando o job trouxer imagem aprovada em `metadataJson.image_to_video.source_image_url`; a Runway atende Gen-4.5, Seedance 2.5, Gen-4 Turbo, Veo 3.1 Fast e Veo 3.1 pelo mesmo adapter.
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
| `video.providers.real.accepted-names` | Valores de `providerName` que ativam o provider real genérico. | `REAL,SYNTHESIA,VEO` |
| `video.providers.real.base-url` | Base URL da API do provider real. | `http://real-video-provider:8080` |
| `video.providers.real.create-path` | Path para criar render no provider. | `/v1/renders` |
| `video.providers.real.status-path-template` | Path para consultar status do render por `providerJobId`. | `/v1/renders/{providerJobId}` |
| `video.providers.real.poll-interval` | Intervalo de polling do status no provider. | `PT5S` |
| `video.providers.real.max-poll-attempts` | Máximo de tentativas de polling antes de timeout técnico. | `120` |
| `video.providers.veo.enabled` | Habilita o adapter direto VEO/Gemini. | `false` |
| `video.providers.veo.api-key` | Chave Gemini usada pelo adapter VEO. | `${GEMINI_API_KEY}` ou arquivo em `${GEMINI_API_KEY_FILE}` |
| `video.providers.veo.model` | Modelo VEO usado para gerar vídeo. | `veo-3.1-generate-preview` |
| `video.providers.veo.duration-seconds` | Duração numérica enviada ao VEO. | `8` |
| `video.providers.luma.enabled` | Habilita o adapter direto Luma Ray 3.2. | `false` |
| `video.providers.luma.base-url` | Host oficial da Luma Agents API. | `https://agents.lumalabs.ai` |
| `video.providers.luma.api-key` | Chave Luma Agents usada pelo adapter Luma. | `${LUMA_AGENTS_API_KEY}` / `${VIDEO_PROVIDERS_LUMA_API_KEY}` |
| `video.providers.luma.api-key-file` | Arquivo montado com a chave Luma. | `${LUMA_API_KEY_FILE}` |
| `video.providers.luma.scene-count` | Quantidade de cenas Ray 3.2 para montagem do hero. | `3` |
| `video.providers.luma.duration` | Duração por cena enviada à Luma Agents API. | `10s` |
| `video.providers.heygen.enabled` | Habilita o adapter direto HeyGen. | `false` |
| `video.providers.heygen.api-key` | Chave HeyGen usada pelo adapter direto. | `${HEYGEN_API_KEY}` ou arquivo em `${HEYGEN_API_KEY_FILE}` |
| `video.providers.heygen.avatar-id` | Avatar/apresentadora HeyGen usado por padrão quando o job não informar `heygen_avatar_id`. | `${VIDEO_PROVIDERS_HEYGEN_AVATAR_ID}` |
| `video.providers.heygen.voice-id` | Voz HeyGen usada por padrão quando o job não informar `heygen_voice_id`. | `${VIDEO_PROVIDERS_HEYGEN_VOICE_ID}` |
| `LUMA_API_KEY_FILE` | Arquivo montado com a chave Luma usada por integrações Ray/Agents. | `/run/secrets/luma_api_key` |
| `KLING_API_KEY_FILE` | Arquivo montado com a chave Kling usada pelo fallback de vídeo. | `/run/secrets/kling_api_key` |
| `HEYGEN_API_KEY_FILE` | Arquivo montado com a chave HeyGen para o adapter HeyGen. | `/run/secrets/heygen_api_key` |
| `RUNWAY_API_KEY_FILE` | Arquivo montado com a chave Runway para o módulo de vídeo. | `/run/secrets/runway_api_key` |

### Nota operacional sobre VEO

O VEO ja foi validado para experimentos do Marketing Hub. Desde 2026-07-10, este modulo tambem possui adapter direto `veo`, acionado por `providerName=VEO` quando `video.providers.veo.enabled=true` e a chave Gemini esta configurada por `GEMINI_API_KEY` ou pelo arquivo `GEMINI_API_KEY_FILE`.

O adapter direto usa o contrato REST da Gemini API: cria uma operacao `predictLongRunning`, faz polling em `operations/...` e baixa o MP4 retornado. O fluxo manual continua sendo fallback operacional quando nao houver chave Gemini ou quando o provider externo estiver indisponivel.

No container, o compose monta por padrao `/root/infra/gemini-token/gemini_api_key` em `/run/secrets/gemini_api_key:ro`; o entrypoint carrega esse arquivo para `GEMINI_API_KEY` antes de iniciar a aplicacao.

### Nota operacional sobre Luma e Kling

O compose monta por padrao os arquivos criados no host de video:

- `/root/infra/luma-token/luma_api_key` em `/run/secrets/luma_api_key:ro`
- `/root/infra/kling-token/kling_api_key` em `/run/secrets/kling_api_key:ro`

O entrypoint carrega esses arquivos sem imprimir os valores. A chave Luma fica disponivel como `LUMA_API_KEY`, `LUMA_AGENTS_API_KEY` e `VIDEO_PROVIDERS_LUMA_API_KEY`, cobrindo o contrato novo da Luma Agents usado por Ray 3.2. A chave Kling fica disponivel como `KLING_API_KEY` e `VIDEO_PROVIDERS_KLING_API_KEY`.

### Nota operacional sobre HeyGen

O compose monta por padrao `/root/infra/heygen-token/heygen_api_key` em `/run/secrets/heygen_api_key:ro`. O entrypoint carrega esse arquivo sem imprimir o valor e deixa a chave disponivel como `HEYGEN_API_KEY` e `VIDEO_PROVIDERS_HEYGEN_API_KEY`, seguindo o mesmo padrao de Luma/Kling/VEO.

O adapter direto `HeyGenVideoProvider` processa jobs `providerName=HEYGEN` usando `POST /v3/videos`, polling em `GET /v3/videos/{videoId}` e download do MP4 final retornado pela API. Para gerar de ponta a ponta, o ambiente precisa informar uma apresentadora e uma voz por `VIDEO_PROVIDERS_HEYGEN_AVATAR_ID` e `VIDEO_PROVIDERS_HEYGEN_VOICE_ID`, ou o job precisa trazer `heygen_avatar_id` e `heygen_voice_id` no `metadataJson`.

### Nota operacional sobre Runway

O compose monta por padrao `/root/infra/runaway-token/runaway_api_key` em `/run/secrets/runway_api_key:ro`. O entrypoint carrega esse arquivo sem imprimir o valor e deixa a chave disponivel como `RUNWAY_API_KEY` e `VIDEO_PROVIDERS_RUNWAY_API_KEY`, seguindo o mesmo padrao de Luma/Kling/VEO/HeyGen. No compose de deploy, `VIDEO_PROVIDERS_RUNWAY_ENABLED` deve permanecer `true` para que os jobs da familia `RUNWAY*` curada tenham adapter registrado. Os contratos enviam o identificador oficial de cada modelo e reutilizam a mesma credencial Runway, sem token adicional; Gen-4 Turbo exige imagem-base e Veo 3.1/3.1 Fast são limitados a 8 segundos por solicitação.

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

Em produção, o serviço grava o arquivo em `/app/logs/video-management-service.log` e expõe a leitura operacional em `/actuator/logfile`, usado pelo MCP no módulo `video-management-service`.

### Dashboards/alertas mínimos recomendados

- **Backlog**: painel com `video_jobs_backlog` por status.
- **Confiabilidade**: taxa de falhas e retries por provider.
- **Latência**: p95/p99 de `video_render_latency_seconds`.
- **Alertas**:
  - backlog `VIDEO_REQUESTED` acima do limiar operacional;
  - aumento de `video_jobs_failed_total`;
  - elevação de `video_backend_retry_total` por 5xx/429.

### Staging atual

- Backend de staging configurado para `http://191.252.181.168`.
- O provider VEO só deve ser habilitado após configurar `GEMINI_API_KEY` real no host/container.

## Construção do container

```bash
docker build -f video-management-service/Dockerfile -t marketinghub-video-management:latest .
```
