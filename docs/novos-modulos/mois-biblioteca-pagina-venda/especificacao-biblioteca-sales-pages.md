# Especificação do Módulo MOIS — Biblioteca de Sales Pages (Hotmart Collector)

## 1. Objetivo

Este módulo tem como objetivo criar uma biblioteca contínua de páginas de vendas coletadas pelo **mois-hotmart-collector**, com processamento em background para:

1. armazenar snapshots rastreáveis das URLs;
2. extrair padrões de copy, estilo visual e imagens;
3. transformar os dados em análises estruturadas reutilizáveis pelo Marketing Hub;
4. acelerar a criação de produtos e páginas com maior potencial de conversão.

A definição funcional segue o eixo canônico do sistema:

**Dor → Resultado → Mecanismo → Prova → Oferta**.

---

## 2. Escopo funcional

O módulo deverá:

- receber URLs vindas do coletor Hotmart;
- enfileirar coletas e análises assíncronas;
- capturar conteúdo bruto (HTML, metadados e screenshots);
- gerar dados estruturados de análise (copy, visual, imagem, score);
- disponibilizar consulta via API backend para consumo do frontend MOIS;
- manter histórico de versões/snapshots para reanálise e auditoria.

Fora do escopo inicial (MVP):

- inferência automática de conversão real sem fonte confiável de performance;
- automação de publicação de páginas;
- análise multimídia avançada de vídeo por frame.

---

## 3. Princípios de arquitetura

1. **Backend como ponto único de integração**
   - Frontend e coletores não acessam banco diretamente.
   - Toda persistência e leitura passam pelo backend principal.

2. **Processamento assíncrono orientado a filas**
   - Coleta e análise não devem bloquear a UX administrativa.

3. **Idempotência e reprocessamento seguro**
   - Jobs devem poder repetir sem duplicar resultados inconsistentes.

4. **Persistência em duas camadas**
   - camada bruta (snapshot);
   - camada estruturada (análises e taxonomias).

5. **Rastreabilidade completa**
   - cada análise precisa apontar para URL, snapshot, versão do parser e versão de prompt/modelo.

---

## 4. Fluxo macro

1. **Ingestão de URLs**
   - origem: `mois-hotmart-collector`.
   - backend recebe lote de URLs e normaliza canonicamente.

2. **Enfileiramento**
   - backend cria jobs de coleta (`PENDING`).

3. **Coleta técnica (worker fetcher)**
   - download do HTML;
   - captura de screenshot desktop/mobile;
   - extração de metadados principais;
   - gravação de snapshot.

4. **Análise estruturada (worker analyzer)**
   - segmentação de seções da página;
   - extração de copy;
   - extração de características visuais e de imagem;
   - score e taxonomia final.

5. **Publicação interna**
   - biblioteca atualizada;
   - status final do job (`DONE`/`FAILED`).

---

## 5. Estados do pipeline

Estado sugerido para jobs:

- `PENDING`
- `FETCHING`
- `FETCHED`
- `ANALYZING`
- `DONE`
- `FAILED`
- `RETRY_SCHEDULED`

Regras:

- falhas transitórias devem ir para retry com backoff exponencial;
- falhas definitivas devem registrar causa-raiz explícita;
- qualquer estado final deve manter trilha de auditoria.

---

## 6. Modelo de dados (proposta inicial)

> Observação: ao implementar fisicamente, atualizar `docs/modelo-dados-experimento.md`.

### 6.1 Entidades principais

- `source_url`
  - url original, url canônica, origem (`HOTMART`), status.

- `sales_page`
  - identidade lógica da página e metadados estáveis.

- `page_snapshot`
  - versão coletada em data/hora, html bruto, hash de conteúdo, artefatos de mídia.

- `processing_job`
  - estado do processamento, tentativas, timestamps e correlação.

- `processing_error`
  - categoria da falha, mensagem técnica, contexto operacional.

- `section_analysis`
  - blocos da estrutura (hero, problema, mecanismo, prova, oferta, faq, cta).

- `copy_analysis`
  - headline, promessa, dor, mecanismo, provas, oferta, CTA e objeções.

- `visual_analysis`
  - padrões de layout, hierarquia, densidade e contraste.

- `image_analysis`
  - tipo de imagem, contexto narrativo e função persuasiva.

- `analysis_score`
  - notas por dimensão e score consolidado.

### 6.2 Índices recomendados

- `source_url(url_canonical)` único;
- `page_snapshot(sales_page_id, captured_at desc)`;
- `processing_job(status, updated_at)`;
- `analysis_score(score_total desc)` para ranqueamento.

---

## 7. Taxonomia de seções canônicas

Seções mínimas para padronização:

1. `hero`
2. `problem_agitation`
3. `mechanism_explanation`
4. `proof`
5. `offer_stack`
6. `faq`
7. `cta_repetition`

Essa taxonomia é obrigatória para comparabilidade entre páginas.

---

## 8. API backend (contrato inicial)

### 8.1 Ingestão

- `POST /api/mois/sales-library/urls:ingest`
  - entrada: lista de URLs + origem/coleta;
  - saída: IDs de jobs criados.

### 8.2 Observabilidade de pipeline

- `GET /api/mois/sales-library/jobs/{jobId}`
- `GET /api/mois/sales-library/jobs?status=PENDING|FAILED|DONE`

### 8.3 Biblioteca

- `GET /api/mois/sales-library/pages`
  - filtros: nicho, idioma, tipo de oferta, score mínimo.

- `GET /api/mois/sales-library/pages/{pageId}`
  - detalhe da página e último snapshot.

- `GET /api/mois/sales-library/pages/{pageId}/analysis`
  - análise estruturada completa.

- `POST /api/mois/sales-library/pages/{pageId}:reanalyze`
  - reexecuta análise a partir do snapshot persistido.


### 8.4 Consulta paginada de entradas ingeridas (implementado)

- `GET /api/mois/sales-library/entries`
  - parâmetros: `workspaceId` (obrigatório), `page` (default `1`), `pageSize` (default `20`, máx `100`);
  - ordenação: `updated_at desc`;
  - retorno: `page`, `pageSize`, `total`, `items[]` com URL original/canônica, origem, título e contadores de ingestão.

### 8.5 Organização de código no backend (implementado)

Para manter o domínio da biblioteca isolado dentro do contexto MOIS no backend principal, os componentes da biblioteca ficam no pacote:

- `com.marketinghub.mois.biblioteca.dto`
- `com.marketinghub.mois.biblioteca.service`
- `com.marketinghub.mois.biblioteca.web`


### 8.6 Captura de snapshots brutos e artefatos (implementado)

- `POST /api/mois/sales-library/snapshots:capture`
  - entrada: `workspaceId`, `limit` e `force`;
  - função: buscar páginas ingeridas sem snapshot bruto, capturar HTML e gerar artefato PNG de screenshot básico;
  - uso: execução manual/operacional e suporte ao agendamento de captura incremental.

- `GET /api/mois/sales-library/pages/{pageId}/snapshots`
  - função: listar snapshots capturados para uma página, com status, hash, bytes de HTML e bytes de screenshot.

- Agendamento backend: `MoisSalesLibrarySnapshotScheduler` executa `captureSnapshots` a cada 30 minutos para reduzir o backlog de páginas sem camada bruta.

- Persistência física:
  - `mois_sales_library_page_snapshot` guarda metadados do snapshot bruto;
  - `mois_sales_library_snapshot_artifact` guarda os artefatos separados `RAW_HTML` e `SCREENSHOT_PNG`.

---

## 9. Regras de qualidade

1. Persistir sempre o snapshot bruto antes da análise.
2. Não descartar falhas silenciosamente; registrar categoria e causa-raiz.
3. Garantir idempotência por `(url_canonical, snapshot_hash)`.
4. Evitar JSON em string dentro de outro JSON.
5. Manter consistência entre contrato API, DTOs e validadores backend.

---

## 10. Segurança, compliance e operação

- respeitar robots/termos de uso aplicáveis;
- nunca armazenar credenciais em código ou documentos;
- usar observabilidade para tempo médio por etapa e taxa de erro;
- manter trilha de auditoria por job e por versão de análise.

---

## 11. Roadmap de implementação

### Fase 1 — MVP operacional

- ingestão de URLs do Hotmart collector;
- fila + worker de coleta;
- persistência de snapshot e status;
- consulta básica no MOIS.

### Fase 2 — Análise estruturada

- taxonomia de seções;
- análise de copy/visual/imagem;
- score inicial.

### Fase 3 — Benchmark e produtividade

- comparação lado a lado entre páginas;
- filtros avançados por padrão persuasivo;
- reanálise em lote por versão de prompt/modelo.

### Fase 4 — Inteligência contínua

- recomendações de blueprint por nicho;
- feedback loop com desempenho de ativos.

---

## 12. Critérios de aceite (MVP)

1. URLs do coletor Hotmart entram automaticamente na fila.
2. Pelo menos 95% das URLs válidas finalizam em `DONE` ou `FAILED` com motivo explícito.
3. Cada item `DONE` possui snapshot + análise mínima em taxonomia canônica.
4. API de listagem e detalhe responde para consumo do frontend MOIS.
5. Reprocessamento (`reanalyze`) funciona sem novo download obrigatório.

---

## 13. Dependências entre módulos

- **mois-hotmart-collector**: origem de URLs.
- **backend**: contrato, orquestração, persistência e exposição de API.
- **worker AI**: análises semânticas/classificação quando aplicável.
- **frontend MOIS**: consumo e visualização da biblioteca.

Toda integração deve ser feita via backend central, mantendo o padrão arquitetural do ecossistema.
