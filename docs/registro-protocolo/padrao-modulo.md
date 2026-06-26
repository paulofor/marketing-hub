

## 2026-06-19 — OPRM NichoCNAE versão 2

- **Módulo executor:** `oprm-coletor-mei`.
- **Pacote protegido:** `com.marketinghub.nichocnaev2.pipeline`.
- **Etapa inicial criada:** `com.marketinghub.nichocnaev2.pipeline.candidategenerator`.
- **Aplicação:** protocolo padrão módulo aplicado no executor responsável pelo fluxo, com núcleo genérico `pipeline`, etapa concreta plugável e teste ArchUnit para impedir dependência do núcleo em etapas concretas, dependência entre etapas e processor fora do contrato `StageProcessor`.

## 2026-06-23 — ops-monitor-worker

- Aplicado protocolo padrão módulo ao executor `ops-monitor-worker`.
- Pacote executor: `com.marketinghub.opsmonitor.pipeline`.
- Etapas iniciais: `healthcheck`, `availability` e `logscan`.
- Ponto inicial canônico previsto para consumo no backend: `GET /api/internal/ops-monitor/v1/module-checks/stage-executions/pending`.
- O worker não acessa banco de dados diretamente; a persistência permanece no backend principal.

## 2026-06-25 — MOIS Sales Library Worker / dossiê
- Módulo executor: `mois-sales-library-worker`.
- Pacote executor: `com.marketinghub.mois.bibliotecapaginavenda.worker.v1`.
- Fluxo protegido: dossiê de prestígio e aquecimento da Biblioteca de Páginas de Vendas.
- O worker mantém a execução operacional de OpenAI e pesquisas externas; o backend mantém estado, persistência, pending e callbacks.

## 2026-06-25 — OPRM NichoCNAE v3

- Módulo executor: `oprm-coletor-mei`.
- Pacote executor: `com.marketinghub.nichocnaev3`.
- Protocolo aplicado com núcleo genérico `pipeline`, etapas concretas plugáveis em subpacotes por etapa, scheduler no executor, consumo via endpoint `pending` do backend e teste ArchUnit próprio.

## 2026-06-25 — MOIS dossiê v1

- Módulo executor: `mois-sales-library-worker`.
- Pacote protegido/criado: `com.marketinghub.mois.dossiev1.pipeline`.
- Pipeline criado como `v1`, com núcleo genérico (`PipelineWorker`, `StageProcessor`, `StageContext`, `StageResult`, `StageArtifact`, `ArtifactStore`) e etapas plugáveis `intake`, `product-understanding`, `investigation-anchor-builder`, `warmup-resource-discovery`, `source-product-match`, `warmup-signal-extraction`, `warmup-map-builder` e `dossier-synthesis`.
- Pontos iniciais canônicos previstos para consumo pelo executor: `/api/internal/mois/dossie/v1/<etapa>/stage-executions/pending`, começando por `intake` e cobrindo todas as etapas v1 do dossiê.

- 2026-06-26 — Aplicado ao módulo executor `ai-worker`, pacote `com.marketinghub.worker.geraanunciov2.pipeline`, pipeline `geraanuncio` v2, etapa inicial `criativo`, consumindo trabalho pelo endpoint pending canônico do backend `/api/internal/geraanuncio/v2/criativo/stage-executions/pending`.

## 2026-06-26 — MOIS dossiê do produto v1

- Módulo executor: `mois-sales-library-worker`.
- Pacote protegido/criado: `com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1` no padrão `pipelines.<nome-pipeline>.v<numero-versao>.<nome-etapa>`.
- Pipeline criado como `v1`, com núcleo genérico (`PipelineWorker`, `StageProcessor`, `StageContext`, `StageResult`, `StageArtifact`, `ArtifactStore`) e etapas plugáveis `fatosproduto`, `analisepagina`, `planejabuscas`, `qualificafontes` e `consolidadossie`.
- Pontos iniciais canônicos previstos para consumo pelo executor: `/api/internal/mois/dossieproduto/v1/<etapa>/stage-executions/pending`.
