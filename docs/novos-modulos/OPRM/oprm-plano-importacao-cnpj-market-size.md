# Plano de importação CNPJ para OPRM (fase 1: quantitativo-first sem geografia)

## Objetivo

Definir como implementar a importação da base CNPJ aberta para suportar métricas quantitativas de tamanho de mercado no OPRM, **com mínima persistência necessária no backend (MySQL 5.7)** e consumo via API.

## Princípios de arquitetura

- Somente o backend acessa o banco de dados.
- OPRM/coletor executa lógica de coleta, parsing e envio em lotes para endpoints do backend.
- Fase 1 sem recorte geográfico obrigatório (município/UF opcional).
- Evitar JSON dentro de JSON nos contratos.
- **Priorizar agregação quantitativa e evitar carga de armazenamento detalhado sem necessidade comprovada.**

## Diretriz desta fase

Como o foco atual é **dados quantitativos**, a fase 1 adota estratégia **quantitativo-first**:

- Persistir somente tabelas operacionais e agregadas necessárias para consulta de market size.
- Evitar, nesta fase, tabelas fato detalhadas por CNPJ (empresa/estabelecimento/sócio/simples).
- Evoluir para detalhamento apenas quando houver caso de uso validado (auditoria, drill-down, segmentação avançada).

## Tabelas propostas (backend)

## 1) Staging de execução

### `oprm_cnpj_import_run`
Controle de cada execução de importação.

Campos sugeridos:
- `id` BIGINT PK AUTO_INCREMENT
- `snapshot_date` DATE NOT NULL
- `source_url` VARCHAR(255) NOT NULL
- `status` VARCHAR(20) NOT NULL (`STARTED`, `COMPLETED`, `FAILED`, `PARTIAL`)
- `started_at` DATETIME NOT NULL
- `finished_at` DATETIME NULL
- `files_total` INT NOT NULL DEFAULT 0
- `files_processed` INT NOT NULL DEFAULT 0
- `rows_read` BIGINT NOT NULL DEFAULT 0
- `rows_valid` BIGINT NOT NULL DEFAULT 0
- `rows_rejected` BIGINT NOT NULL DEFAULT 0
- `error_message` VARCHAR(1000) NULL

Índices:
- `idx_import_run_snapshot` (`snapshot_date`)
- `idx_import_run_status` (`status`)

### `oprm_cnpj_import_file`
Rastreia cada arquivo ZIP processado na execução.

Campos sugeridos:
- `id` BIGINT PK AUTO_INCREMENT
- `run_id` BIGINT NOT NULL (FK lógica para `oprm_cnpj_import_run.id`)
- `file_name` VARCHAR(120) NOT NULL
- `file_url` VARCHAR(255) NOT NULL
- `dataset_type` VARCHAR(30) NOT NULL (`CNAE`, `EMPRESA`, `ESTABELECIMENTO`, `SOCIO`, `SIMPLES`, `DOMINIO`)
- `status` VARCHAR(20) NOT NULL
- `rows_read` BIGINT NOT NULL DEFAULT 0
- `rows_valid` BIGINT NOT NULL DEFAULT 0
- `rows_rejected` BIGINT NOT NULL DEFAULT 0
- `started_at` DATETIME NOT NULL
- `finished_at` DATETIME NULL
- `error_message` VARCHAR(1000) NULL

Índices:
- `idx_import_file_run` (`run_id`)
- `idx_import_file_dataset` (`dataset_type`)

## 2) Dimensão essencial (fase 1)

### `oprm_cnpj_cnae_dim`
- `cnae_code` VARCHAR(7) PK
- `description` VARCHAR(255) NOT NULL
- `active` TINYINT(1) NOT NULL DEFAULT 1
- `updated_at` DATETIME NOT NULL

> Nota: `natureza`, `motivo` e `qualificacao` podem entrar em fase posterior, somente se exigidos por novos indicadores.

## 3) Tabela agregada de mercado (API pronta para OPRM)

### `oprm_market_size_by_cnae`
Agregado materializado para consultas rápidas.

- `snapshot_date` DATE NOT NULL
- `cnae_code` VARCHAR(7) NOT NULL
- `total_estabelecimentos` BIGINT NOT NULL
- `total_estabelecimentos_ativos` BIGINT NOT NULL
- `total_empresas` BIGINT NOT NULL
- `total_empresas_mei` BIGINT NOT NULL
- `total_empresas_simples` BIGINT NOT NULL
- `avg_socios_por_empresa` DECIMAL(10,2) NULL
- `updated_at` DATETIME NOT NULL

PK e índices:
- PK (`snapshot_date`, `cnae_code`)
- `idx_market_size_total_ativos` (`total_estabelecimentos_ativos`)

## 4) Fatos detalhados (postergados)

As tabelas abaixo **não fazem parte da fase 1** para evitar sobrecarga de banco:

- `oprm_cnpj_empresa_fact`
- `oprm_cnpj_estabelecimento_fact`
- `oprm_cnpj_simples_fact`
- `oprm_cnpj_socio_fact`

Critério para ativação futura:
- necessidade formal de drill-down por CNPJ;
- auditoria detalhada com retenção histórica;
- novos produtos que dependam desses dados granulares.

## Fluxo de importação a implementar

## Etapa A — Inicialização da execução
1. Criar registro em `oprm_cnpj_import_run` com status `STARTED`.
2. Gerar lista de arquivos da snapshot.
3. Criar registros em `oprm_cnpj_import_file` (status `STARTED`).

## Etapa B — Importação de dimensão
1. Importar `Cnaes.zip`.
2. Upsert por chave de domínio.
3. Marcar arquivo como `COMPLETED` e atualizar contadores.

## Etapa C — Agregação quantitativa direta (sem persistir fatos)
1. Processar `Empresas*.zip`, `Estabelecimentos*.zip`, `Simples.zip` e `Socios*.zip` em lotes (batch 5k–20k linhas).
2. Agregar em memória/stream por `snapshot_date + cnae_code`.
3. Regras de parsing:
   - datas `00000000` -> `NULL`;
   - decimal com vírgula -> `DECIMAL(18,2)`;
   - campos fora do layout -> rejeição com contagem + log de erro.
4. Persistir somente resultado consolidado em `oprm_market_size_by_cnae`.

## Etapa D — Finalização
1. Fechar `oprm_cnpj_import_file` pendentes.
2. Atualizar `oprm_cnpj_import_run` com `COMPLETED` ou `PARTIAL/FAILED`.
3. Expor resumo em endpoint de execuções do OPRM.

## Endpoints backend necessários

- `POST /api/oprm/market/import-runs` (iniciar execução)
- `POST /api/oprm/market/import-runs/{runId}/files/{fileId}/events` (progresso)
- `POST /api/oprm/market/import-runs/{runId}/complete` (finalizar)
- `GET /api/oprm/market/import-runs` (histórico)
- `GET /api/oprm/market-size/cnaes?snapshotDate=YYYY-MM-DD&limit=...` (consulta agregada)

## Estratégia de rollout

1. **PR 1**: Liquibase (tabelas `run/file` + `cnae_dim` + `market_size_by_cnae`).
2. **PR 2**: Serviço backend de consolidação quantitativa e validações de contrato.
3. **PR 3**: Implementação do coletor OPRM para download/parse/agregação/envio em batch.
4. **PR 4**: Endpoint de consulta + observabilidade (métricas, logs, retentativas e alertas).
5. **PR 5 (opcional)**: Introdução de fatos detalhados, se justificado por caso de uso.

## Critérios de aceite (fase 1)

- Importação completa de uma snapshot com rastreabilidade por arquivo.
- Contagem consistente entre linhas lidas, válidas e rejeitadas.
- Consulta de tamanho de mercado por CNAE com tempo de resposta aceitável.
- Nenhuma dependência de recorte geográfico para entregar valor na fase inicial.
- Persistência limitada ao mínimo necessário para o objetivo quantitativo atual.
