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

## Consultas administrativas com filtros

Os endpoints administrativos aceitam filtros opcionais para reduzir a visão operacional por impacto:

```http
GET /api/ops-monitor/v1/modules/availability?criticality=CRITICAL&type=WORKER
GET /api/ops-monitor/v1/incidents/open?criticality=CRITICAL&type=WORKER
GET /api/ops-monitor/v1/incidents/history?criticality=CRITICAL&type=WORKER
```

- `criticality`: `CRITICAL`, `HIGH`, `MEDIUM` ou `LOW`.
- `type`: `BACKEND`, `WORKER`, `COLLECTOR`, `PORTAL`, `SERVICE` ou `PDE`.

## Confiabilidade do status atual

O endpoint `modules/availability` lista apenas módulos habilitados no cadastro operacional. Quando o último heartbeat passa da janela `offlineThresholdSeconds`, o backend retorna `status=UNKNOWN`, `heartbeatStale=true`, `lastCheckAgeSeconds` e `statusReason`. Assim, a tela diferencia queda confirmada de monitor atrasado antes de acionar recuperação operacional.

## Módulos monitorados na fase 4

Além de `backend`, `ai-worker` e `facebook-ads-worker`, o backend passa a cadastrar: `oprm-coletor-mei`, `mois-clickbank-collector`, `mois-hotmart-collector`, `mois-sales-library-worker`, `lead-portal` e `email-service`.
