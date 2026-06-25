

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
