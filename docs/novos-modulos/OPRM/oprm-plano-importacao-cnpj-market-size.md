# Plano de importação CNPJ para OPRM (fase 1: tamanho de mercado sem geografia)

## Objetivo

Definir como implementar a importação da base CNPJ aberta para suportar métricas quantitativas de tamanho de mercado no OPRM, com persistência no backend (MySQL 5.7) e consumo via API.

## Princípios de arquitetura

- Somente o backend acessa o banco de dados.
- OPRM/coletor executa lógica de coleta, parsing e envio em lotes para endpoints do backend.
- Fase 1 sem recorte geográfico obrigatório (município/UF opcional).
- Evitar JSON dentro de JSON nos contratos.

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

## 2) Dimensões (fase 1)

### `oprm_cnpj_cnae_dim`
- `cnae_code` VARCHAR(7) PK
- `description` VARCHAR(255) NOT NULL
- `active` TINYINT(1) NOT NULL DEFAULT 1
- `updated_at` DATETIME NOT NULL

### `oprm_cnpj_natureza_dim`
- `natureza_code` VARCHAR(4) PK
- `description` VARCHAR(255) NOT NULL
- `updated_at` DATETIME NOT NULL

### `oprm_cnpj_motivo_dim`
- `motivo_code` VARCHAR(2) PK
- `description` VARCHAR(255) NOT NULL
- `updated_at` DATETIME NOT NULL

### `oprm_cnpj_qualificacao_dim`
- `qualificacao_code` VARCHAR(2) PK
- `description` VARCHAR(255) NOT NULL
- `updated_at` DATETIME NOT NULL

## 3) Fatos de mercado (fase 1)

### `oprm_cnpj_empresa_fact`
Chave no nível CNPJ básico (8 dígitos).

- `cnpj_basico` VARCHAR(8) PK
- `razao_social` VARCHAR(255) NOT NULL
- `natureza_code` VARCHAR(4) NULL
- `porte_code` VARCHAR(2) NULL
- `capital_social` DECIMAL(18,2) NULL
- `ente_federativo` VARCHAR(255) NULL
- `snapshot_date` DATE NOT NULL
- `updated_at` DATETIME NOT NULL

Índices:
- `idx_empresa_natureza` (`natureza_code`)
- `idx_empresa_porte` (`porte_code`)

### `oprm_cnpj_estabelecimento_fact`
Chave no nível CNPJ completo (8+4+2).

- `cnpj_basico` VARCHAR(8) NOT NULL
- `cnpj_ordem` VARCHAR(4) NOT NULL
- `cnpj_dv` VARCHAR(2) NOT NULL
- `cnpj_completo` VARCHAR(14) NOT NULL
- `matriz_filial` VARCHAR(1) NULL
- `nome_fantasia` VARCHAR(255) NULL
- `situacao_cadastral` VARCHAR(2) NULL
- `motivo_code` VARCHAR(2) NULL
- `data_situacao_cadastral` DATE NULL
- `data_inicio_atividade` DATE NULL
- `cnae_principal` VARCHAR(7) NULL
- `cnaes_secundarios` TEXT NULL
- `snapshot_date` DATE NOT NULL
- `updated_at` DATETIME NOT NULL

PK e índices:
- PK (`cnpj_completo`)
- `idx_estab_cnpj_basico` (`cnpj_basico`)
- `idx_estab_cnae_principal` (`cnae_principal`)
- `idx_estab_situacao` (`situacao_cadastral`)
- `idx_estab_motivo` (`motivo_code`)

### `oprm_cnpj_simples_fact`
- `cnpj_basico` VARCHAR(8) PK
- `optante_simples` CHAR(1) NULL
- `data_opcao_simples` DATE NULL
- `data_exclusao_simples` DATE NULL
- `optante_mei` CHAR(1) NULL
- `data_opcao_mei` DATE NULL
- `data_exclusao_mei` DATE NULL
- `snapshot_date` DATE NOT NULL
- `updated_at` DATETIME NOT NULL

### `oprm_cnpj_socio_fact`
- `id` BIGINT PK AUTO_INCREMENT
- `cnpj_basico` VARCHAR(8) NOT NULL
- `tipo_socio` VARCHAR(1) NULL
- `nome_socio` VARCHAR(255) NULL
- `documento_socio_ofuscado` VARCHAR(20) NULL
- `qualificacao_code` VARCHAR(2) NULL
- `data_entrada_sociedade` DATE NULL
- `snapshot_date` DATE NOT NULL
- `updated_at` DATETIME NOT NULL

Índices:
- `idx_socio_cnpj_basico` (`cnpj_basico`)
- `idx_socio_qualificacao` (`qualificacao_code`)

## 4) Tabela agregada de mercado (API pronta para OPRM)

### `oprm_market_size_by_cnae`
Agregado materializado para consultas rápidas.

- `snapshot_date` DATE NOT NULL
- `cnae_code` VARCHAR(7) NOT NULL
- `total_estabelecimentos` BIGINT NOT NULL
- `total_estabelecimentos_ativos` BIGINT NOT NULL
- `total_empresas` BIGINT NOT NULL
- `total_empresas_mei` BIGINT NOT NULL
- `total_empresas_simples` BIGINT NOT NULL
- `avg_socios_por_empresa` DECIMAL(10,2) NOT NULL
- `updated_at` DATETIME NOT NULL

PK e índices:
- PK (`snapshot_date`, `cnae_code`)
- `idx_market_size_total_ativos` (`total_estabelecimentos_ativos`)

## Fluxo de importação a implementar

## Etapa A — Inicialização da execução
1. Criar registro em `oprm_cnpj_import_run` com status `STARTED`.
2. Gerar lista de arquivos da snapshot.
3. Criar registros em `oprm_cnpj_import_file` (status `STARTED`).

## Etapa B — Importação de dimensões
1. Importar `Cnaes.zip`, `Naturezas.zip`, `Motivos.zip`, `Qualificacoes.zip`.
2. Upsert por chave de domínio.
3. Marcar arquivo como `COMPLETED` e atualizar contadores.

## Etapa C — Importação dos fatos
1. Importar `Empresas*.zip` em lotes (batch 5k–20k linhas).
2. Importar `Estabelecimentos*.zip` em lotes.
3. Importar `Simples.zip` e `Socios*.zip`.
4. Regras de parsing:
   - datas `00000000` -> `NULL`;
   - decimal com vírgula -> `DECIMAL(18,2)`;
   - campos fora do layout -> rejeição com contagem + log de erro.

## Etapa D — Agregação quantitativa
1. Recalcular `oprm_market_size_by_cnae` para `snapshot_date` da execução.
2. Métricas mínimas:
   - estabelecimentos totais e ativos por CNAE;
   - empresas totais por CNAE;
   - optantes Simples/MEI por CNAE;
   - média de sócios por empresa por CNAE.

## Etapa E — Finalização
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

1. **PR 1**: Liquibase (tabelas de run/file + dimensões + fatos).
2. **PR 2**: Serviço backend de upsert e validações de contrato.
3. **PR 3**: Implementação do coletor OPRM para download/parse/envio em batch.
4. **PR 4**: Agregação `oprm_market_size_by_cnae` + endpoint de consulta.
5. **PR 5**: Observabilidade (métricas, logs, retentativas e alertas).

## Critérios de aceite (fase 1)

- Importação completa de uma snapshot com rastreabilidade por arquivo.
- Contagem consistente entre linhas lidas, válidas e rejeitadas.
- Consulta de tamanho de mercado por CNAE com tempo de resposta aceitável.
- Nenhuma dependência de recorte geográfico para entregar valor na fase inicial.
