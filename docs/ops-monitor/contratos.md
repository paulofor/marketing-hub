# Ops Monitor Worker — contratos

## Entrada canônica do executor

```http
GET /api/internal/ops-monitor/v1/module-checks/stage-executions/pending
```

## Registro de heartbeat

```http
POST /api/internal/ops-monitor/v1/modules/{moduleCode}/heartbeat
```

Payload funcional produzido pela etapa `healthcheck`:

- `moduleCode`
- `status`
- `httpStatus`
- `responseTimeMs`
- `rawPayload`
- `errorMessage`

## Registro de incidente

```http
POST /api/internal/ops-monitor/v1/modules/{moduleCode}/incidents
```

Payload funcional produzido pela etapa `logscan`:

- `moduleCode`
- `incidentSignalFound`
- `signals`
