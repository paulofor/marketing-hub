# Plano OPRM + MEI — dados de nicho e tamanho de mercado

## Objetivo
Estruturar uma camada de inteligência de mercado baseada em MEI para alimentar o OPRM com evidências quantitativas de nicho (tamanho, crescimento e concentração regional) para **todos os nichos mapeados**, com foco operacional e comercial nos **nichos de maior tamanho**, mantendo o fluxo canônico **Dor → Resultado → Mecanismo → Prova → Oferta**.

## Resultado esperado
- Backend com dados versionados de nicho por CNAE/ocupação.
- Endpoints para ranking e contexto de nicho.
- OPRM consumindo contexto de nicho durante processamento de jobs.
- Score de oportunidade por nicho para priorização comercial.

---

## Fase 1 — Governança e modelagem (Semana 1)

### 1.1 Definição de fontes oficiais
- Catalogar as fontes oficiais para:
  - base de MEIs ativos;
  - abertura/baixa por período;
  - classificação por CNAE e geografia (UF/município).
- Definir política de atualização (ex.: mensal).

### 1.2 Mapeamento de domínio OPRM ↔ CNAE
- Criar dicionário de mapeamento entre ocupações OPRM e CNAEs relevantes.
- Permitir mapeamento 1:N (uma ocupação com múltiplos CNAEs).
- Definir regra de ponderação por CNAE (peso padrão e ajustes).

### 1.3 Modelo de dados no backend
Criar entidades/tabelas:
- `niche_catalog`
  - `id`, `occupation_key`, `cnae_code`, `cnae_label`, `weight`, `active`
- `niche_snapshot`
  - `snapshot_date`, `cnae_code`, `uf`, `municipio`, `mei_active`, `openings`, `closures`, `net`
- `niche_occupation_aggregate`
  - `snapshot_date`, `occupation_key`, `mei_total`, `growth_12m`, `hotspot_uf`, `confidence`
- `niche_opportunity_score`
  - `snapshot_date`, `occupation_key`, `size_score`, `growth_score`, `concentration_score`, `final_score`, `explanation`

### 1.4 Contratos de API backend
Definir endpoints:
- `GET /api/niches/ranking?metric=size|growth|opportunity&limit=20`
- `GET /api/niches/context?occupationKey=...&locale=pt-BR`
- `GET /api/niches/snapshots?occupationKey=...&from=...&to=...`

---

## Fase 2 — Ingestão e qualidade de dados (Semana 2)

### 2.1 Pipeline de ingestão
- Criar job batch no backend para ingestão da base MEI/CNAE.
- Etapas:
  1. download/coleta da fonte;
  2. parse e validação de esquema;
  3. normalização de chaves geográficas;
  4. carga incremental em `niche_snapshot`.

### 2.2 Regras de qualidade
- Duplicidade por chave (`snapshot_date`, `cnae_code`, `uf`, `municipio`).
- Campos obrigatórios e limites válidos (sem negativos indevidos).
- Flag de qualidade por lote (`quality_status`, `quality_notes`).

### 2.3 Observabilidade
- Métricas:
  - registros lidos/descartados/carregados;
  - duração do pipeline;
  - atraso da última atualização.
- Logs com `correlationId` e identificação de lote.

---

## Fase 3 — Cálculo de tamanho e score de nicho (Semana 3)

### 3.1 Indicadores primários
- `mei_total` (tamanho atual do nicho).
- `growth_12m` (crescimento acumulado 12 meses).
- `regional_concentration` (participação dos 3 principais estados/municípios).

### 3.2 Score de oportunidade
- Fórmula inicial:
  - `final_score = 0.45*size_score + 0.35*growth_score + 0.20*concentration_score`.
- Padronizar escala 0–100.
- Armazenar explicação textual curta para auditoria de decisão.

### 3.3 Validação de negócio
- Rodar validação sobre o universo completo de nichos mapeados.
- Priorizar análises detalhadas para os nichos com maior `mei_total` e melhor `final_score`.
- Ajustar pesos e thresholds com base em resultado percebido.

---

## Fase 4 — Integração com OPRM (Semana 4)

### 4.1 Enriquecimento do job OPRM
- Antes da geração de artefatos, OPRM chama backend:
  - `GET /api/niches/context?occupationKey=...`
- Contexto retornado mínimo:
  - `meiTotal`, `growth12m`, `regionalHotspots`, `opportunityScore`, `snapshotDate`.

### 4.2 Inclusão em artefatos OPRM
Adicionar bloco `marketEvidence` aos artefatos relevantes:
- `occupationProfileSnapshot`
- `occupationPersonaRoutineCard`
- `dorResultadoOfertaMecanismoProvaInput`

### 4.3 Regras de fallback
- Sem contexto disponível: continuar pipeline com `marketEvidence.status=UNAVAILABLE`.
- Não bloquear processamento de job por indisponibilidade temporária desse contexto.

---

## Fase 5 — Operação contínua e evolução (Semana 5+)

### 5.1 Rotina operacional
- Atualização mensal automatizada.
- Alertas para atraso de carga e queda abrupta de qualidade.

### 5.2 Governança canônica
- Manter sincronizados:
  - `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`
  - `docs/modelo-dados-experimento.md`
  - contratos de API backend/OPRM.

### 5.3 Roadmap evolutivo
- Segmentação por faixa de maturidade do nicho.
- Correlação com performance de hipóteses (CTR, CVR, CPA).
- Priorização automática de nichos para geração de ofertas.

---

## Critérios de aceite
1. Endpoints de nicho entregues e testados.
2. Ingestão executando com sucesso em base real.
3. OPRM consumindo `marketEvidence` sem quebrar jobs existentes.
4. Ranking de nichos disponível por tamanho, crescimento e oportunidade.
5. Documentação canônica atualizada e alinhada aos testes.

## Riscos e mitigação
- **Mudança de layout da fonte**: criar versionamento de parser.
- **Dados incompletos por município**: fallback por UF e marcação de confiança.
- **Drift de mapeamento ocupação↔CNAE**: revisão mensal assistida por negócio.
- **Sobrecarga no backend**: pré-agregação e cache de consultas de ranking.

## Backlog inicial (execução)
1. Criar dicionário ocupação↔CNAE para todos os nichos mapeados.
2. Criar migrações e entidades backend para snapshots e agregados.
3. Implementar ingestão batch + validações de qualidade.
4. Implementar endpoints de contexto/ranking.
5. Integrar OPRM ao endpoint de contexto com priorização de processamento para os maiores nichos.
6. Ajustar artefatos com bloco `marketEvidence`.
7. Criar testes unitários/integração ponta a ponta.
