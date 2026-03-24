# video-management-service

Módulo dedicado ao gerenciamento técnico do ciclo de vida de vídeos que não utilizam o OpenAI diretamente.

## Funcionalidades entregues nesta sprint

- Estrutura básica em Spring Boot 3 / Java 21.
- Configurações externalizadas via `video.backend-base-url`, token opcional e parâmetros de polling.
- Poller interno (desativado por padrão) que consulta o backend nos endpoints `/internal/video/jobs`.
- Logs visíveis para cada job coletado, preparando o terreno para integrações reais com providers.
- Endpoint administrativo `GET /api/status` para inspecionar a configuração ativa.

## Execução local

```bash
mvn spring-boot:run
```

Variáveis importantes:

| Propriedade | Descrição | Padrão |
|-------------|-----------|--------|
| `video.backend-base-url` | URL interna do `backend/ads-service`. | `http://backend:8000` |
| `video.auth-token` | Token (Bearer) opcional para autenticação mútua. | vazio |
| `video.jobs.polling-enabled` | Liga/desliga o poller automático. | `false` |
| `video.jobs.poll-interval` | Intervalo em ISO-8601 (ex.: `PT30S`). | `PT30S` |
| `video.jobs.batch-size` | Número máximo de jobs por ciclo. | `10` |

## Construção do container

```bash
docker build -t marketinghub-video-management:latest .
```
