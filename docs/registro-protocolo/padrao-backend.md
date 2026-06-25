

## 2026-06-19 — OPRM NichoCNAE versão 2

- **Backend:** `backend/ads-service`.
- **Pacote protegido:** `com.marketinghub.oprm.nichocnae.v2.candidategenerator`.
- **Endpoint pending canônico:** `/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions/pending`.
- **Aplicação:** protocolo padrão backend aplicado para a etapa inicial `candidate-generator`, com controller canônico, service canônico, contrato `record` em subpacote de service e regra ArchUnit dedicada em `ArquiteturaTest`.

## 2026-06-25 — OPRM NichoCNAE versão 2 completo

- **Backend:** `backend/ads-service`.
- **Pacote protegido:** `com.marketinghub.oprm.nichocnae.v2`.
- **Executor operacional externo:** `oprm-coletor-mei`.
- **Etapas protegidas:** `candidate-generator`, `source-safety-filter`, `adaptive-query-planner`, `source-fetcher-reranker`, `candidate-tournament`, `knowledge-accumulator`, `commercial-evidence-gate`, `reprocess-controller` e `enriched-niche-materializer`.
- **Endpoint pending canônico:** `/api/internal/oprm/nichocnae/v2/<etapa>/stage-executions/pending`.
- **Aplicação:** protocolo padrão backend ampliado para todas as etapas v2 existentes, exigindo controller único, service backend canônico único, endpoint interno `pending` como ponto inicial do executor e contratos `record` em subpacotes de service por regra ArchUnit dedicada em `ArquiteturaTest`.

## 2026-06-23 — Ops Monitor

- Pacote backend protegido: `com.marketinghub.opsmonitor`.
- Executor operacional externo: `ops-monitor-worker`.
- Endpoint pending canônico: `/api/internal/ops-monitor/v1/module-checks/stage-executions/pending`.

## 2026-06-25 — MOIS Biblioteca de Páginas de Vendas / dossiê
- Pacote protegido: `com.marketinghub.mois.bibliotecapaginavenda.worker.v1`.
- Etapa: dossiê de prestígio e aquecimento da Biblioteca de Páginas de Vendas.
- Endpoint pending canônico exposto no controller único: `/api/mois/sales-library/market-warmup/stage-executions/pending`.
- Executor externo responsável pela execução operacional: `mois-sales-library-worker`.

## 2026-06-25 — OPRM NichoCNAE v3

- Pacote backend protegido: `com.marketinghub.oprm.nichocnae.v3`.
- Módulo executor externo: `oprm-coletor-mei`.
- Protocolo aplicado com etapas versionadas, controller/service canônicos por etapa, contratos `record` em subpacotes de service e endpoint `pending` em `/api/internal/oprm/nichocnae/v3/<etapa>/stage-executions/pending`.

## 2026-06-25 — MOIS dossiê v1

- Backend protegido/criado: `com.marketinghub.mois.dossie.v1` com pacotes por etapa.
- Módulo executor responsável pela execução operacional: `mois-sales-library-worker`.
- Endpoints internos pending canônicos aplicados no padrão `/api/internal/mois/dossie/v1/<etapa>/stage-executions/pending`.
- Contratos da operação mantidos como `record` em subpacotes de `service`.
