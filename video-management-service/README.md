# video-management-service

Módulo dedicado ao gerenciamento técnico do ciclo de vida de vídeos que não utilizam o OpenAI diretamente.

## Funcionalidades entregues nesta sprint

- Aplicação Spring Boot 3 / Java 21 com scheduler interno para polling assíncrono.
- Configuração externa de URL/base do backend, token opcional, identificador do worker e parâmetros do poller.
- Cliente REST para todos os endpoints internos de jobs (`claim`, `progress`, `complete`, `fail`).
- Upload automático de vídeo, poster e legendas através do novo endpoint interno `/internal/video/assets` do backend.
- Provider `stub` que lê o script aprovado do perfil, gera artefatos fictícios (MP4, PNG e VTT) e reporta progresso durante o pipeline.
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

## Construção do container

```bash
docker build -t marketinghub-video-management:latest .
```
