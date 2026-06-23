# Ops Monitor — Contratos

## Consumo do worker

`GET /api/internal/ops-monitor/v1/module-checks/stage-executions/pending` retorna módulos habilitados para verificação.

## Callback do worker

`POST /api/internal/ops-monitor/v1/modules/{moduleCode}/heartbeat` registra uma verificação de saúde.

`POST /api/internal/ops-monitor/v1/modules/{moduleCode}/incidents` registra um incidente operacional.

## Tela administrativa

A tela administrativa deve consumir os endpoints `/api/ops-monitor/v1/*` para resumo, disponibilidade, histórico e incidentes.
