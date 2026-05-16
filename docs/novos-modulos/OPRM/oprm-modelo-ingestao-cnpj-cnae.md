# OPRM — Modelo de Dados e Fluxo de Ingestão CNPJ/CNAE

## Objetivo

Este documento descreve, de forma operacional, **como funciona o modelo de dados e a ingestão de CNPJ/CNAE** no OPRM para suportar análise de tamanho de mercado por CNAE.

O foco é garantir rastreabilidade de execução, observabilidade por arquivo e geração de métricas consolidadas por CNAE no snapshot processado.

---

## Visão Geral do Pipeline

1. O coletor inicia uma execução (`import run`) para um `snapshot_date`.
2. O backend cria a run e semeia os arquivos esperados da execução.
3. Para cada arquivo processado, o coletor publica eventos de progresso/resultado.
4. O backend persiste status e métricas por arquivo, além de dimensões e agregados de mercado por CNAE.
5. Ao final, a execução é fechada e consolidada com status final (`COMPLETED` ou `PARTIAL`).

---

## Modelo de Dados (Tabelas Principais)

### 1) `oprm_cnpj_import_run`

Representa a execução macro de ingestão para um snapshot.

Campos-chave:
- `id`
- `snapshot_date`
- `source_url`
- `status`
- `started_at` / `finished_at`
- `files_total` / `files_processed`
- `rows_read` / `rows_valid` / `rows_rejected`
- `error_message`

Uso principal:
- Auditoria da execução completa.
- Indicadores globais da ingestão (linhas e arquivos).

### 2) `oprm_cnpj_import_file`

Representa cada arquivo da execução (`run_id`).

Campos-chave:
- `id`
- `run_id` (FK para `oprm_cnpj_import_run`)
- `file_name`, `file_url`, `dataset_type`
- `status`
- `rows_read` / `rows_valid` / `rows_rejected`
- `started_at` / `finished_at`
- `error_message`

Uso principal:
- Observabilidade e diagnóstico por arquivo.
- Identificação de falhas pontuais sem perder a execução inteira.

### 3) `oprm_cnpj_cnae_dim`

Dimensão de CNAE utilizada no OPRM.

Campos-chave:
- `cnae_code` (PK)
- `description`
- `active`
- `updated_at`

Uso principal:
- Catálogo de CNAEs utilizados na modelagem e leitura analítica.

### 4) `oprm_market_size_by_cnae`

Agregado de tamanho de mercado por CNAE e snapshot.

Chave composta:
- `snapshot_date`
- `cnae_code`

Métricas principais:
- `total_estabelecimentos`
- `total_estabelecimentos_ativos`
- `total_empresas`
- `total_empresas_mei`
- `total_empresas_simples`
- `avg_socios_por_empresa`

Uso principal:
- Base para ranking de CNAEs por volume.
- Base para análise de oportunidade por mercado.

---

## Endpoints de Ingestão (Backend OPRM)

Base path:
- `/api/oprm/market/import-runs`

Principais rotas:

1. `POST /api/oprm/market/import-runs`
   - Cria uma nova execução e seeds de arquivos.

2. `POST /api/oprm/market/import-runs/{runId}/files/{fileId}/events`
   - Registra progresso/resultado por arquivo.
   - Atualiza status e contadores (`rows_read`, `rows_valid`, `rows_rejected`).
   - Persiste upserts de:
     - CNAEs (`oprm_cnpj_cnae_dim`)
     - agregados de mercado (`oprm_market_size_by_cnae`)

3. `POST /api/oprm/market/import-runs/{runId}/complete`
   - Finaliza a run e determina status final.

4. `GET /api/oprm/market/import-runs`
   - Lista execuções.

5. `GET /api/oprm/market/import-runs/{runId}/files`
   - Lista arquivos de uma execução.

6. `GET /api/oprm/market/import-runs/cnaes`
   - Lista catálogo de CNAEs.

7. `GET /api/oprm/market/import-runs/cnaes/top-volume?limit=`
   - Retorna ranking de CNAEs por `totalEmpresas`, no snapshot mais recente.

---

## Regras de Consolidação e Status

- Se nenhum status explícito for informado no fechamento da run, o backend resolve automaticamente:
  - `COMPLETED` quando não há falha de arquivo.
  - `PARTIAL` quando existe ao menos um arquivo `FAILED`.
- Arquivo com status `STARTED` no momento da finalização é convertido para `FAILED` com mensagem padrão de fechamento incompleto.
- O endpoint de ranking de volume aplica limite seguro (1..100) e ordena por `totalEmpresas` no snapshot mais recente.

---

## Onde ficam as "linhas lidas"

As linhas lidas são armazenadas em dois níveis:

1. **Nível run**: `oprm_cnpj_import_run.rows_read`
2. **Nível arquivo**: `oprm_cnpj_import_file.rows_read`

Isso permite visão executiva da carga e diagnóstico fino por arquivo.

---

## Resultado de Negócio no OPRM

Com esse modelo, o sistema consegue:
- medir tamanho de mercado por CNAE com dados consolidados por snapshot;
- identificar rapidamente falhas operacionais de ingestão sem perder rastreabilidade;
- disponibilizar ranking de volume para priorização de mercados com maior potencial de escala.
