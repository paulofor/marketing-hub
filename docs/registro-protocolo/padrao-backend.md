

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

- Pacote backend protegido: `com.marketinghub.oprmcoletormei.nichocnae.v3`.
- Módulo executor externo: `oprm-coletor-mei`.
- Protocolo aplicado com etapas versionadas, controller/service canônicos por etapa, contratos `record` em subpacotes de service e endpoint `pending` em `/api/internal/oprm/nichocnae/v3/<etapa>/stage-executions/pending`.

## 2026-06-25 — MOIS dossiê v1

- Backend protegido/criado: `com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1` com pacotes por etapa.
- Módulo executor responsável pela execução operacional: `mois-sales-library-worker`.
- Endpoints internos pending canônicos aplicados no padrão `/api/internal/moissaleslibraryworker/dossieproduto/v1/<etapa>/stage-executions/pending`.
- Contratos da operação mantidos como `record` em subpacotes de `service`.

- 2026-06-26 — Aplicado ao pipeline `geracaoanuncios` v1 no backend, pacote `com.marketinghub.pipelines.aiworker.geracaoanuncios.v1`, etapas `texto` e `imagem`, com endpoints pending canônicos `/api/internal/aiworker/geracaoanuncios/v1/texto/stage-executions/pending` e `/api/internal/aiworker/geracaoanuncios/v1/imagem/stage-executions/pending` e contratos DTO como `record` em subpacotes de service.
- 2026-06-26 — Aplicado ao pipeline `geraanuncio` v2 no backend, pacote `com.marketinghub.geraanuncio.v2`, etapa inicial `criativo`, com endpoint pending canônico `/api/internal/geraanuncio/v2/criativo/stage-executions/pending` e contratos DTO como `record` em subpacotes de service.

## 2026-06-26 — Consolidação MOIS dossiê v1

- Removido o pacote backend duplicado `com.marketinghub.pipelines.mois.dossieproduto.v1`.
- Pacote canônico mantido para o backend: `com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1`.
- Módulo executor responsável pela execução operacional: `mois-sales-library-worker`.
- 2026-06-26 — Aplicado ao pipeline `geraanuncio` v2 no backend, pacote `com.marketinghub.geraanuncio.v2`, etapas `texto` e `imagem`, com endpoints pending canônicos `/api/internal/geraanuncio/v2/texto/stage-executions/pending` e `/api/internal/geraanuncio/v2/imagem/stage-executions/pending` e contratos DTO como `record` em subpacotes de service.

## 2026-07-03 — MOIS Biblioteca de Páginas de Vendas — dois dossiês

- Pacote backend protegido: `com.marketinghub.mois.bibliotecapaginavenda.worker.v1`.
- Módulo executor externo: `mois-sales-library-worker`.
- Pipelines expostos para enfileiramento: `salespagepatterns.v1` e `warmupecosystem.v1`.
- Aplicação: ArchUnit no backend para impedir que a biblioteca vire executor runtime de OpenAI; backend mantém leitura/escrita, custos e contratos.

## 2026-07-12 — SalesVideo / solicitação de vídeos

- Pacote backend protegido: `com.marketinghub.salesvideo`.
- Módulo executor externo: `video-management-service`.
- Fluxo protegido: solicitação de render em `/api/sales-videos/profiles/{profileId}/request-render`, pending canônico legado em `/internal/video/jobs`, callbacks internos de claim/heartbeat/progress/complete/fail/expired e upload interno de artefatos em `/internal/video/assets`.
- Aplicação: protocolo padrão backend aplicado por regra ArchUnit em `ArquiteturaTest`, exigindo backend como fonte de verdade para job/eventos/status, persistência auditável da execução e gate de compliance antes de render produtivo.
