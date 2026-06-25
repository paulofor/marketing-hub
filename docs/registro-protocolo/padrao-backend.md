

## 2026-06-19 — OPRM NichoCNAE versão 2

- **Backend:** `backend/ads-service`.
- **Pacote protegido:** `com.marketinghub.oprm.nichocnae.v2.candidategenerator`.
- **Endpoint pending canônico:** `/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions/pending`.
- **Aplicação:** protocolo padrão backend aplicado para a etapa inicial `candidate-generator`, com controller canônico, service canônico, contrato `record` em subpacote de service e regra ArchUnit dedicada em `ArquiteturaTest`.

## 2026-06-23 — Ops Monitor

- Pacote backend protegido: `com.marketinghub.opsmonitor`.
- Executor operacional externo: `ops-monitor-worker`.
- Endpoint pending canônico: `/api/internal/ops-monitor/v1/module-checks/stage-executions/pending`.

## 2026-06-25 — MOIS Biblioteca de Páginas de Vendas / dossiê
- Pacote protegido: `com.marketinghub.mois.bibliotecapaginavenda.worker.v1`.
- Etapa: dossiê de prestígio e aquecimento da Biblioteca de Páginas de Vendas.
- Endpoint pending canônico exposto no controller único: `/api/mois/sales-library/market-warmup/stage-executions/pending`.
- Executor externo responsável pela execução operacional: `mois-sales-library-worker`.
