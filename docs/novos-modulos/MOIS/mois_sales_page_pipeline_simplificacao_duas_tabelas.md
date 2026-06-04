# Plano de simplificação — Pipeline de páginas de vendas MOIS em duas tabelas operacionais

## 1. Objetivo

Simplificar o pipeline operacional da Biblioteca de Páginas de Vendas do MOIS para reduzir ambiguidade entre coleta bruta, página consolidada, captura, análise e histórico de execução.

O modelo alvo passa a ter **duas tabelas operacionais principais** para a biblioteca:

1. `mois_sales_page` — estado atual e consolidado da página de venda.
2. `mois_sales_page_job_execution` — histórico/auditoria das execuções realizadas sobre a página.

A tabela `mois_collected_reference` permanece como **origem bruta dos coletores** Hotmart/ClickBank e não deve ser tratada como estado operacional da biblioteca.

## 2. Problema atual

O pipeline atual distribui o estado de uma página em várias tabelas:

- `mois_collected_reference` — referências brutas coletadas dos marketplaces;
- `mois_collected_reference_html_capture` — HTML bruto capturado a partir de referência coletada;
- `mois_sales_library_url_ingest` — URL consolidada/canonicalizada da biblioteca;
- `mois_sales_library_processing_job` — controle de job assíncrono;
- `mois_sales_library_page_analysis` — resultado estruturado de análise;
- `mois_sales_library_page_snapshot` — snapshot/versionamento de captura;
- `mois_sales_library_snapshot_artifact` — artefatos derivados do snapshot.

Esse desenho dificulta respostas operacionais simples, como:

- quantas páginas de venda existem de fato na biblioteca;
- quantas estão pendentes;
- qual etapa atual de uma página;
- qual URL final foi observada após redirecionamento;
- qual foi o último erro;
- qual execução gerou o HTML ou a análise atual;
- por que uma etapa processou zero URLs.

## 3. Princípio do modelo alvo

A tela e o operador devem consultar uma única fonte para o estado atual da página:

```text
mois_sales_page
```

Toda execução, tentativa, erro, payload, HTML bruto ou análise produzida deve ficar no histórico:

```text
mois_sales_page_job_execution
```

Regra operacional:

> A cada etapa do pipeline, o backend registra/atualiza a execução em `mois_sales_page_job_execution` e atualiza `mois_sales_page` com o estado atual consolidado.

## 4. Escopo do que permanece fora das duas tabelas

`mois_collected_reference` continua existindo como origem bruta dos coletores.

Ela deve continuar guardando dados vindos de Hotmart/ClickBank, como produto, marketplace, sinais de sucesso, preço, temperatura, URL de produto e URL de página de vendas. Porém, ela não deve ser usada pela UI principal da Biblioteca como indicador de status operacional da página.

Fluxo conceitual:

```text
mois_collected_reference
        │
        ▼
mois_sales_page
        │
        ▼
mois_sales_page_job_execution
```

## 5. Tabela alvo 1 — `mois_sales_page`

### 5.1 Responsabilidade

Guardar o estado atual consolidado de uma página de venda.

Essa tabela deve responder rapidamente às telas:

- total de páginas na biblioteca;
- total por fonte (`HOTMART`, `CLICKBANK` etc.);
- etapa atual;
- status atual;
- pendentes;
- capturadas;
- analisadas;
- falhas;
- URL original, URL canônica, URL final e raiz do redirecionamento;
- último erro;
- último score comercial;
- última execução que alterou a página.

### 5.2 Campos recomendados

Campos mínimos sugeridos:

```sql
id BIGINT PRIMARY KEY AUTO_INCREMENT,
workspace_id VARCHAR(120) NOT NULL,
source VARCHAR(40) NOT NULL,
source_job_id VARCHAR(120) NULL,
source_reference_id VARCHAR(120) NULL,
collected_reference_id BIGINT NULL,
product_name VARCHAR(512) NULL,
title VARCHAR(512) NULL,
url_original VARCHAR(1024) NOT NULL,
url_canonical VARCHAR(1024) NOT NULL,
sales_page_url VARCHAR(1024) NULL,
product_url VARCHAR(1024) NULL,
url_final VARCHAR(1024) NULL,
redirect_root_url VARCHAR(1024) NULL,
current_stage VARCHAR(40) NOT NULL,
current_status VARCHAR(40) NOT NULL,
capture_status VARCHAR(40) NULL,
analysis_status VARCHAR(40) NULL,
http_status INT NULL,
content_type VARCHAR(255) NULL,
html_sha256 VARCHAR(64) NULL,
html_bytes BIGINT NOT NULL DEFAULT 0,
score_total DECIMAL(6,2) NULL,
offer_summary VARCHAR(1000) NULL,
mechanism_summary VARCHAR(1000) NULL,
promise_summary VARCHAR(1000) NULL,
proof_summary VARCHAR(1000) NULL,
last_error_category VARCHAR(120) NULL,
last_error_message VARCHAR(1000) NULL,
last_job_execution_id BIGINT NULL,
ingest_count INT NOT NULL DEFAULT 1,
first_seen_at DATETIME NULL,
last_collected_at DATETIME NULL,
last_captured_at DATETIME NULL,
last_analyzed_at DATETIME NULL,
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

### 5.3 Índices recomendados

```sql
UNIQUE KEY uk_mois_sales_page_workspace_url (workspace_id, url_canonical(512)),
KEY idx_mois_sales_page_source_status (workspace_id, source, current_status, updated_at),
KEY idx_mois_sales_page_stage_status (workspace_id, current_stage, current_status, updated_at),
KEY idx_mois_sales_page_score (workspace_id, score_total),
KEY idx_mois_sales_page_source_reference (workspace_id, source, source_job_id, source_reference_id)
```

### 5.4 Regra de atualização

`mois_sales_page` deve receber `UPDATE` em cada mudança relevante:

- ingestão da URL;
- início da captura;
- conclusão/falha da captura;
- redirecionamento observado;
- início da análise;
- conclusão/falha da análise;
- solicitação de reprocessamento;
- anulação manual;
- seleção para etapa seguinte.

## 6. Tabela alvo 2 — `mois_sales_page_job_execution`

### 6.1 Responsabilidade

Guardar o histórico/auditoria de cada execução operacional sobre uma página.

Cada execução deve indicar:

- qual página foi processada;
- qual etapa foi executada;
- qual tentativa;
- entrada usada;
- saída gerada;
- payloads técnicos necessários para auditoria;
- erro, quando houver;
- horário de início e término.

### 6.2 Campos recomendados

```sql
id BIGINT PRIMARY KEY AUTO_INCREMENT,
sales_page_id BIGINT NOT NULL,
workspace_id VARCHAR(120) NOT NULL,
job_type VARCHAR(40) NOT NULL,
stage VARCHAR(40) NOT NULL,
status VARCHAR(40) NOT NULL,
attempt INT NOT NULL DEFAULT 1,
claimed_by VARCHAR(120) NULL,
input_url VARCHAR(1024) NULL,
final_url VARCHAR(1024) NULL,
redirect_root_url VARCHAR(1024) NULL,
http_status INT NULL,
content_type VARCHAR(255) NULL,
raw_html LONGTEXT NULL,
raw_html_sha256 VARCHAR(64) NULL,
raw_html_bytes BIGINT NOT NULL DEFAULT 0,
screenshot_blob LONGBLOB NULL,
screenshot_bytes BIGINT NOT NULL DEFAULT 0,
score_total DECIMAL(6,2) NULL,
sections_json LONGTEXT NULL,
copy_json LONGTEXT NULL,
visual_json LONGTEXT NULL,
image_json LONGTEXT NULL,
request_payload_json LONGTEXT NULL,
response_payload_json LONGTEXT NULL,
error_category VARCHAR(120) NULL,
error_message VARCHAR(1000) NULL,
started_at DATETIME NULL,
finished_at DATETIME NULL,
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
CONSTRAINT fk_mois_sales_page_job_execution_page
  FOREIGN KEY (sales_page_id) REFERENCES mois_sales_page(id)
```

### 6.3 Índices recomendados

```sql
KEY idx_mois_sales_page_job_page_created (sales_page_id, created_at),
KEY idx_mois_sales_page_job_status (workspace_id, stage, status, updated_at),
KEY idx_mois_sales_page_job_type_status (workspace_id, job_type, status, updated_at)
```

### 6.4 Observação sobre dados grandes

Para MVP, `raw_html` pode ficar em `mois_sales_page_job_execution` para simplificar diagnóstico.

Quando o volume crescer, a evolução recomendada é mover HTML/screenshot para storage externo e manter na execução apenas:

- hash;
- tamanho;
- content type;
- ponteiro/URL de armazenamento;
- metadados de auditoria.

## 7. Mapeamento do modelo atual para o modelo alvo

| Modelo atual | Modelo alvo |
|---|---|
| `mois_sales_library_url_ingest` | `mois_sales_page` |
| `mois_sales_library_processing_job` | `mois_sales_page_job_execution` + campos atuais em `mois_sales_page` |
| `mois_sales_library_page_analysis` | `mois_sales_page_job_execution` + resumo atual em `mois_sales_page` |
| `mois_sales_library_page_snapshot` | `mois_sales_page_job_execution` + captura atual em `mois_sales_page` |
| `mois_sales_library_snapshot_artifact` | `mois_sales_page_job_execution` ou storage externo futuro |
| `mois_collected_reference_html_capture` | `mois_sales_page_job_execution`, quando a captura bruta for incorporada ao pipeline unificado |
| `mois_collected_reference` | permanece como origem bruta dos coletores |

## 8. Fases de implementação

### Fase 1 — Canonizar e criar o modelo novo

Objetivo: preparar o banco e documentar a regra sem quebrar o fluxo atual.

Entregas:

1. Atualizar o cânone MOIS com o modelo alvo de duas tabelas operacionais.
2. Criar changelog Liquibase incremental para `mois_sales_page`.
3. Criar changelog Liquibase incremental para `mois_sales_page_job_execution`.
4. Adicionar índices de leitura operacional.
5. Criar testes de schema/integração quando aplicável.

Critério de aceite:

- tabelas novas existem;
- nenhuma tabela antiga foi removida;
- aplicação sobe sem quebrar endpoints atuais.

### Fase 2 — Backfill inicial

Objetivo: popular o novo modelo a partir do estado atual.

Fontes de leitura:

- `mois_sales_library_url_ingest`;
- último job em `mois_sales_library_processing_job`;
- última análise em `mois_sales_library_page_analysis`;
- último snapshot em `mois_sales_library_page_snapshot`;
- artefatos relevantes de `mois_sales_library_snapshot_artifact`;
- vínculo possível com `mois_collected_reference`.

Entregas:

1. Criar serviço/rotina idempotente de backfill.
2. Popular `mois_sales_page` com o estado atual consolidado.
3. Popular `mois_sales_page_job_execution` com histórico mínimo ou com últimas execuções relevantes.
4. Registrar contadores de migração em log.

Critério de aceite:

- total de páginas no novo modelo bate com o total consolidado esperado;
- páginas Hotmart/ClickBank aparecem com fonte correta;
- status atual da tela pode ser derivado de `mois_sales_page`.

### Fase 3 — Escrita dupla temporária

Objetivo: manter compatibilidade enquanto validamos o novo modelo.

Entregas:

1. Ao ingerir URL, gravar no modelo antigo e no novo.
2. Ao capturar HTML, gravar snapshot antigo e execução nova.
3. Ao analisar página, gravar análise antiga e execução nova.
4. Atualizar sempre o estado atual em `mois_sales_page`.
5. Adicionar logs de divergência quando antigo e novo não baterem.

Critério de aceite:

- nenhum endpoint atual quebra;
- o novo modelo reflete o mesmo estado operacional do antigo;
- divergências ficam visíveis em log.

### Fase 4 — Leitura do frontend pelo modelo novo

Objetivo: simplificar a UI e eliminar contadores ambíguos.

Entregas:

1. Criar endpoints de listagem/resumo baseados em `mois_sales_page`.
2. Criar endpoint de histórico baseado em `mois_sales_page_job_execution`.
3. Atualizar `/mois/sales-pages-library` para usar a tabela consolidada nova.
4. Atualizar `/mois/sales-pages-library/pipeline` para mostrar contadores globais reais:
   - total de páginas;
   - pendentes;
   - em captura;
   - capturadas;
   - analisadas;
   - falhas;
   - bloqueadas por cooldown.

Critério de aceite:

- o número exibido como total de páginas vem de `mois_sales_page`;
- pendências não são calculadas apenas sobre uma página de paginação;
- detalhes e histórico são explicáveis a partir de duas tabelas.

### Fase 5 — Trocar a escrita principal

Objetivo: tornar o novo modelo a fonte operacional principal.

Entregas:

1. Backend passa a escrever primariamente em `mois_sales_page` e `mois_sales_page_job_execution`.
2. Tabelas antigas ficam em modo legado/auditoria, sem alimentar a UI principal.
3. Ajustar jobs/workers para usar contratos novos.
4. Atualizar Swagger e documentação do módulo.

Critério de aceite:

- pipeline executa ponta a ponta usando as duas tabelas operacionais;
- UI não depende mais de joins com as tabelas antigas;
- logs indicam claramente cada transição de etapa.

### Fase 6 — Congelamento e desativação gradual do legado

Objetivo: reduzir manutenção do modelo antigo sem perder auditoria.

Entregas:

1. Marcar tabelas antigas como legado no cânone.
2. Manter somente leitura por janela de auditoria definida.
3. Remover escrita dupla após validação operacional.
4. Planejar arquivamento ou limpeza futura dos dados antigos.

Critério de aceite:

- não há endpoint produtivo dependendo das tabelas antigas para estado atual;
- dados legados seguem disponíveis para auditoria durante a janela combinada;
- documentação deixa claro qual é a fonte de verdade atual.

## 9. Regras de transição de estado

Estados sugeridos para `mois_sales_page.current_status`:

- `DISCOVERED` — página identificada a partir da coleta;
- `PENDING_CAPTURE` — aguardando captura de HTML;
- `CAPTURING` — captura em andamento;
- `CAPTURED` — HTML útil capturado;
- `PENDING_ANALYSIS` — aguardando análise comercial;
- `ANALYZING` — análise em andamento;
- `ANALYZED` — análise concluída;
- `FAILED` — última etapa falhou;
- `BLOCKED_COOLDOWN` — bloqueada temporariamente por falha recente;
- `DISCARDED` — anulado/manual ou sem valor operacional.

Estados sugeridos para `mois_sales_page_job_execution.status`:

- `PENDING`;
- `RUNNING`;
- `DONE`;
- `FAILED`;
- `SKIPPED`;
- `CANCELLED`.

Tipos sugeridos para `job_type`/`stage`:

- `INGESTION`;
- `HTML_CAPTURE`;
- `ANALYSIS`;
- `REANALYSIS`;
- `STATUS_UPDATE`;
- `PUBLICATION_PREP`.

## 10. Benefícios esperados

- Redução de ambiguidade entre total coletado e total operacional.
- Tela mais simples e precisa.
- Diagnóstico de causa-raiz mais rápido.
- Menos joins para responder ao usuário.
- Histórico preservado sem fragmentar o estado atual.
- Base mais clara para escalar páginas vencedoras e gerar vendas.

## 11. Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Perda de histórico de snapshots | Guardar execuções completas em `mois_sales_page_job_execution` durante a transição. |
| Tabela de execução crescer muito | Definir política futura de retenção/storage externo para HTML e screenshots. |
| Divergência entre modelo antigo e novo | Usar fase de escrita dupla com logs de comparação. |
| Quebra de frontend | Migrar leitura em fase separada após backfill validado. |
| Mudança grande no backend | Implementar por fases pequenas e testáveis. |

## 12. Decisão operacional

A simplificação deve ser implementada incrementalmente. Até a conclusão da migração, as tabelas antigas permanecem funcionando.

A fonte de verdade futura para a UI operacional da Biblioteca de Páginas de Vendas será:

```text
mois_sales_page
```

A fonte de verdade futura para histórico/auditoria de execuções será:

```text
mois_sales_page_job_execution
```
