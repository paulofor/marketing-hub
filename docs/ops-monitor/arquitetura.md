# Ops Monitor — Arquitetura

O Ops Monitor separa execução operacional e fonte de verdade.

- `ops-monitor-worker` executará as verificações ativas dos módulos.
- O backend principal persiste módulos, heartbeats, disponibilidade e incidentes.
- O frontend deverá apenas apresentar os dados expostos pelo backend, sem inferir disponibilidade localmente.

## Backend — fase 1

Pacote criado: `com.marketinghub.opsmonitor`.

Contratos principais:

- `GET /api/internal/ops-monitor/v1/module-checks/stage-executions/pending`
- `POST /api/internal/ops-monitor/v1/modules/{moduleCode}/heartbeat`
- `POST /api/internal/ops-monitor/v1/modules/{moduleCode}/incidents`
- `GET /api/ops-monitor/v1/summary`
- `GET /api/ops-monitor/v1/modules/availability`
- `GET /api/ops-monitor/v1/modules/{moduleCode}/availability-history`
- `GET /api/ops-monitor/v1/incidents/open`
- `GET /api/ops-monitor/v1/incidents/history`
