

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
- Pacote executor: `com.marketinghub.pipelines.nichocnae.v3`.
- Protocolo aplicado com núcleo genérico `pipeline`, etapas concretas plugáveis em subpacotes por etapa, scheduler no executor, consumo via endpoint `pending` do backend e teste ArchUnit próprio.

## 2026-06-25 — MOIS dossiê v1

- Módulo executor: `mois-sales-library-worker`.
- Pacote protegido/criado: `com.marketinghub.pipelines.dossie.v1`.
- Pipeline criado como `v1`, com núcleo genérico (`PipelineWorker`, `StageProcessor`, `StageContext`, `StageResult`, `StageArtifact`, `ArtifactStore`) e etapas plugáveis `intake`, `product-understanding`, `investigation-anchor-builder`, `warmup-resource-discovery`, `source-product-match`, `warmup-signal-extraction`, `warmup-map-builder` e `dossier-synthesis`.
- Pontos iniciais canônicos previstos para consumo pelo executor: `/api/internal/mois/dossieproduto/v1/<etapa>/stage-executions/pending`, começando por `intake` e cobrindo todas as etapas v1 do dossiê.

- 2026-06-26 — Registro do pipeline `geracaoanuncios` v1 no `ai-worker` aplicado no pacote correto `com.marketinghub.pipelines.geracaoanuncios.v1` e pelas etapas `texto` e `imagem`.
- 2026-06-26 — Aplicado ao módulo executor `ai-worker`, pacote `com.marketinghub.worker.geraanunciov2.pipeline`, pipeline `geraanuncio` v2, etapa inicial `criativo`, consumindo trabalho pelo endpoint pending canônico do backend `/api/internal/geraanuncio/v2/criativo/stage-executions/pending`.

## 2026-06-26 — Consolidação MOIS dossiê v1

- Módulo executor: `mois-sales-library-worker`.
- Removido o pacote executor duplicado `com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1`.
- Pacote canônico mantido no módulo: `com.marketinghub.pipelines.dossie.v1`.
- 2026-06-26 — Registro legado do GeraAnuncio v2 no `ai-worker` substituído pelo pacote correto `com.marketinghub.geraanuncio.v2` e pelas etapas `texto` e `imagem`.


## 2026-06-26 — AI Worker — GeracaoAnuncios v1 texto/imagem

- Módulo executor: `ai-worker`.
- Pacote protegido: `com.marketinghub.pipelines.geracaoanuncios.v1`.
- Alteração: o pipeline GeracaoAnuncios v1 passou a ter duas etapas internas espelhadas com o backend, `texto` e `imagem`, cada uma com client próprio para o endpoint `pending` canônico da etapa par no backend.

## 2026-06-27 — MOIS dossieproduto v1 no Sales Library Worker

- Módulo executor: `mois-sales-library-worker`.
- Solicitação aplicada para `dossieproduto.v1`, usando o pacote executor canônico consolidado `com.marketinghub.pipelines.dossie.v1` para evitar recriar o pacote duplicado removido em 2026-06-26.
- Escopo protegido: núcleo genérico do dossiê v1 e etapas plugáveis `intake`, `productunderstanding`, `investigationanchorbuilder`, `warmupresourcediscovery`, `sourceproductmatch`, `warmupsignalextraction`, `warmupmapbuilder` e `dossiersynthesis`.
- Validação adicional: teste de contrato do executor garante catálogo completo de processors, execução por `PipelineWorker`, saída estruturada, artefatos auditáveis não nulos e bloqueio para etapa sem processor registrado.
- Backend permanece fora do protocolo padrão módulo; o worker continua consumindo o trabalho pelos contratos `pending` canônicos do backend e reportando resultados pelos callbacks oficiais.
## 2026-06-27 — OPRM NichoCNAE v3 / reforço do protocolo padrão módulo

- Módulo executor: `oprm-coletor-mei`.
- Pacote protegido: `com.marketinghub.pipelines.nichocnae.v3`.
- Reforço aplicado no teste ArchUnit do executor para bloquear tecnologia concreta no núcleo `v3.core` e ciclos entre pacotes da v3, mantendo etapas plugáveis e consumo por endpoints `pending` do backend.
- Backend fora do escopo desta aplicação: permanece como fonte de verdade de contratos, pendências e callbacks.
## 2026-06-27 — AI Worker — GeracaoAnuncios v1

- Módulo executor: `ai-worker`.
- Pacote protegido: `com.marketinghub.pipelines.geracaoanuncios.v1`.
- Etapas protegidas: `texto` e `imagem`.
- Ponto inicial canônico de consumo pelo executor: `/api/internal/aiworker/geracaoanuncios/v1/<etapa>/stage-executions/pending`.
- Aplicação: protocolo padrão módulo reforçado no executor, sem alterar o backend principal, com núcleo declarativo versionado, etapas plugáveis e regras ArchUnit específicas para impedir dependência do núcleo em etapas concretas, dependência entre etapas, ciclos, processor fora do contrato `StageProcessor` e tecnologia concreta no núcleo.

## 2026-06-27 — AI Worker — GeracaoAnuncios v1 / classes completas por etapa

- Módulo executor: `ai-worker`.
- Pacote protegido: `com.marketinghub.pipelines.geracaoanuncios.v1`.
- Etapas protegidas: `texto` e `imagem`.
- Aplicação: protocolo padrão módulo reforçado com conjunto completo de classes por etapa (`BackendClient`, `ExecutionScheduler`, `Input`, `Output`, `Processor`, `PromptBuilder`, `ResponseHandler`, `ResponseValidator`, `WorkerConfiguration` e `WorkerProperties`), mantendo consumo pelo endpoint `pending` canônico e backend fora do escopo do protocolo módulo.
