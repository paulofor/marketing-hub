# video-management-service

Módulo dedicado ao gerenciamento técnico do ciclo de vida de vídeos que não utilizam o OpenAI diretamente.

## Funcionalidades entregues nesta sprint

- Aplicação Spring Boot 3 / Java 21 com scheduler interno para polling assíncrono.
- Configuração externa de URL/base do backend, token opcional, identificador do worker e parâmetros do poller.
- Cliente REST para todos os endpoints internos de jobs (`claim`, `progress`, `complete`, `fail`).
- Upload automático de vídeo, poster e legendas através do novo endpoint interno `/internal/video/assets` do backend.
- Provider `stub` que lê o script aprovado do perfil, gera artefatos fictícios (MP4, PNG e VTT) e reporta progresso durante o pipeline.
- Provider `real` com integração HTTP configurável (request render + polling + download), com normalização de falhas/expiração para o backend.
- Dispatcher multi-thread com controle contra concorrência duplicada.
- Poller interno (desativado por padrão) que consome jobs `RENDER` com `provider_family=EXTERNAL_VIDEO_MODULE` e envia para o dispatcher.

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
| `video.jobs.polling-enabled` | Liga/desliga o poller automático. | `false` |
| `video.jobs.poll-interval` | Intervalo em ISO-8601 (ex.: `PT30S`). | `PT30S` |
| `video.jobs.batch-size` | Número máximo de jobs por ciclo. | `10` |
| `video.providers.real.enabled` | Habilita adapter do provider real. | `false` |
| `video.providers.real.accepted-names` | Valores de `providerName` que ativam o provider real. | `REAL,HEYGEN,SYNTHESIA` |
| `video.providers.real.base-url` | Base URL da API do provider real. | `http://real-video-provider:8080` |
| `video.providers.real.create-path` | Path para criar render no provider. | `/v1/renders` |
| `video.providers.real.status-path-template` | Path para consultar status do render por `providerJobId`. | `/v1/renders/{providerJobId}` |
| `video.providers.real.poll-interval` | Intervalo de polling do status no provider. | `PT5S` |
| `video.providers.real.max-poll-attempts` | Máximo de tentativas de polling antes de timeout técnico. | `120` |

### Staging atual

- Backend de staging configurado para `http://191.252.181.168:8000`.
- O provider real só deve ser habilitado após configurar credenciais (`video.providers.real.auth-token`) e conectividade da API externa.

## Construção do container

```bash
docker build -t marketinghub-video-management:latest .
```
