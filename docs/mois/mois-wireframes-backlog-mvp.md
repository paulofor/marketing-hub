# MOIS — Wireframes de baixa fidelidade + backlog técnico (próximo passo)

Este documento transforma o playbook funcional em especificação executável para produto, frontend e backend.

Referência base: `docs/mois/mois-conjunto-telas-playbook.md`.

---

## 1) Wireframes de baixa fidelidade (estrutura + estados)

## Tela 1 — Workspace MOIS

### Estrutura (desktop)

```
[Header: título + filtro de ciclo + botão "Nova análise"]
[KPIs: Coletas | Extrações | Aplicações | Testes]
[Stepper de estágio: Coleta > Extração > Síntese > Aplicação > Teste]
[Lista "Análises recentes"]
  - item: nicho | status | updatedAt | ações [Retomar] [Aplicar]
[Sidebar direita "Ações rápidas"]
  - Importar referência
  - Abrir biblioteca
  - Comparar oferta
```

### Estrutura (mobile)

```
[Header compacto]
[Botão principal "Nova análise"]
[Stepper horizontal scrollável]
[Cards de KPI em carrossel]
[Lista de análises]
```

### Componentes React sugeridos
- `MoisWorkspacePage`
- `MoisKpiCards`
- `MoisPipelineStepper`
- `MoisRecentAnalysisList`
- `MoisQuickActions`

### Estados de UI
- **loading:** skeleton nos KPIs e na lista.
- **empty:** CTA "Criar primeira análise".
- **error:** alerta com ação "Tentar novamente".

---

## Tela 2 — Coleta de referências

### Estrutura

```
[Header: Coleta de referências]
[Form principal]
  nicho* | url* | tipoAtivo* | promessa* | consciencia* 
  preco | formato | observacoes
[Preview lateral da URL]
[Botões: Cancelar | Salvar referência]
[Tabela de referências já coletadas]
```

### Componentes React sugeridos
- `MoisReferenceIntakePage`
- `MoisReferenceForm`
- `MoisUrlPreviewCard`
- `MoisReferenceTable`

### Estados de UI
- **submitting:** botão `Salvar referência` desabilitado + spinner.
- **validation error:** mensagem por campo.
- **save success:** toast "Referência salva".
- **save error:** toast com motivo técnico amigável.

---

## Tela 3 — Extração guiada

### Estrutura

```
[Header: Extração guiada]
[Coluna A: referência (snapshot)]
[Coluna B: editor estruturado]
  tabs: Dor | Resultado | Mecanismo | Prova | Oferta
  cada tab: campos + score de confiança + evidências
[Painel lateral: taxonomias detectadas]
  copy patterns
  layout patterns
[Rodapé fixo: Salvar rascunho | Aprovar insights]
```

### Componentes React sugeridos
- `MoisExtractionPage`
- `MoisReferenceSnapshotPanel`
- `MoisExtractionTabs`
- `MoisTaxonomyPanel`
- `MoisStickyActions`

### Estados de UI
- **loading extraction:** skeleton em tabs.
- **partial data:** badge "extração parcial".
- **approval success/error:** feedback no rodapé.

---

## Tela 4 — Biblioteca MOIS

### Estrutura

```
[Header: Biblioteca]
[Filtros: nicho, avatar, promessa, estágio, formato, evidência]
[Grid de blocos]
  card: tipo | resumo | tags | score | origem
  ações: Favoritar | Duplicar para oferta | Comparar
[Drawer: detalhes do bloco]
```

### Componentes React sugeridos
- `MoisLibraryPage`
- `MoisLibraryFilters`
- `MoisBlockGrid`
- `MoisBlockDetailDrawer`

### Estados de UI
- **loading grid:** skeleton cards.
- **empty filtered:** "Nenhum bloco para os filtros atuais".
- **error:** alerta + retry.

---

## Tela 5 — Comparador (mercado vs sua oferta)

### Estrutura

```
[Header: Comparador]
[Seletores: referência base | oferta atual]
[Quadro 2 colunas]
  Promessa (mercado vs atual)
  Mecanismo (mercado vs atual)
  Prova (mercado vs atual)
  Layout (mercado vs atual)
[Scorecards: clareza | prova | coerência | atrito]
[Lista de melhorias priorizadas]
```

### Componentes React sugeridos
- `MoisComparisonPage`
- `MoisComparisonSelectors`
- `MoisComparisonMatrix`
- `MoisScorecards`
- `MoisImprovementBacklog`

### Estados de UI
- **no selection:** estado orientativo para escolher pares.
- **comparison loaded:** diffs com destaques visuais.

---

## Tela 6 — Aplicar na minha oferta (builder)

### Estrutura

```
[Header: Builder de oferta]
[3 colunas]
  A: blocos recomendados
  B: versão atual
  C: versão proposta (editável)
[Checklist obrigatório]
  Dor | Resultado | Mecanismo | Prova | Oferta
[Ações: Gerar versão | Salvar versão | Exportar]
```

### Componentes React sugeridos
- `MoisOfferBuilderPage`
- `MoisRecommendedBlocksColumn`
- `MoisCurrentOfferColumn`
- `MoisProposedOfferEditor`
- `MoisCanonicalChecklist`

### Estados de UI
- **generating:** botões desabilitados + spinner.
- **missing checklist items:** bloqueio com explicação.
- **save/export feedback:** toast de confirmação/erro.

---

## Tela 7 — Plano de experimento

### Estrutura

```
[Header: Plano de experimento]
[Form]
  hipótese* | controle* | variação* | métrica* | janela* | critério*
[Resumo automático de plano]
[Botões: Salvar plano | Publicar teste]
```

### Componentes React sugeridos
- `MoisExperimentPlanPage`
- `MoisExperimentPlanForm`
- `MoisPlanSummaryCard`

### Estados de UI
- **draft saved:** badge "Rascunho salvo".
- **publish pending:** spinner no botão.
- **publish failed:** bloco com causa + ação corretiva.

---

## 2) Contratos backend (MVP) para suportar as telas

> Observação: como o front depende do backend, os endpoints abaixo devem ser priorizados antes de UI final.

## 2.1 Workspace

- `GET /api/v1/mois/workspaces/{workspaceId}/dashboard`
  - retorna KPIs, etapa atual e análises recentes.

## 2.2 Coleta

- `POST /api/v1/mois/references`
  - cria referência de mercado.
- `GET /api/v1/mois/references?workspaceId=...`
  - lista referências coletadas.

Payload mínimo (`POST /references`):

```json
{
  "workspaceId": "uuid",
  "niche": "nutricao-esportiva",
  "sourceUrl": "https://exemplo.com/oferta",
  "assetType": "LANDING_PAGE",
  "primaryPromise": "Secar 5kg em 8 semanas",
  "awarenessStage": "PROBLEM_AWARE",
  "priceRange": "97-297",
  "formatType": "CURSO",
  "notes": "Oferta com forte prova social"
}
```

## 2.3 Extração

- `POST /api/v1/mois/references/{referenceId}/extractions`
  - inicia/atualiza extração estruturada.
- `POST /api/v1/mois/extractions/{extractionId}/approve`
  - aprova insights.

## 2.4 Biblioteca

- `GET /api/v1/mois/library/blocks`
  - busca com filtros.
- `POST /api/v1/mois/library/blocks/{blockId}/favorite`
- `POST /api/v1/mois/library/blocks/{blockId}/duplicate`

## 2.5 Comparador

- `POST /api/v1/mois/comparisons`
  - gera comparação e scorecards.

## 2.6 Builder

- `POST /api/v1/mois/offers/build`
  - monta versão proposta com base em blocos selecionados.
- `POST /api/v1/mois/offers/{offerId}/exports`
  - exporta blueprint (markdown/json).

## 2.7 Experimentos

- `POST /api/v1/mois/experiment-plans`
- `POST /api/v1/mois/experiment-plans/{planId}/publish`

---

## 3) Backlog funcional (stories prontas para execução)

## Epic A — Fundamentos do fluxo MOIS
1. **A1 — Dashboard do workspace**
   - como usuário, quero ver KPIs e análises recentes para retomar rapidamente.
   - DoD: loading/empty/error + paginação funcional.
2. **A2 — Cadastro de referência com validação**
   - DoD: campos obrigatórios, validação de URL e feedback de submit.
3. **A3 — Lista de referências coletadas**
   - DoD: ordenação por data + filtros básicos por tipo/nicho.

## Epic B — Extração e biblioteca
4. **B1 — Extração em tabs DRMP-O**
   - DoD: salvar rascunho e aprovar insights.
5. **B2 — Biblioteca de blocos com filtros**
   - DoD: favoritar, duplicar e abrir detalhe.

## Epic C — Aplicação e validação
6. **C1 — Comparador com scorecards**
   - DoD: cálculo + explicação de score por dimensão.
7. **C2 — Builder de oferta em 3 colunas**
   - DoD: checklist obrigatório bloqueando export sem completude.
8. **C3 — Plano de experimento publicável**
   - DoD: validação de campos e status de publicação.

---

## 4) Priorização técnica (2 semanas)

### Semana 1
- Backend: dashboard, references (POST/GET), extraction draft.
- Frontend: telas 1 e 2 completas + estados.

### Semana 2
- Backend: library list/favorite/duplicate, comparisons, offer build.
- Frontend: telas 3, 4, 5 e 6 (MVP), com plano de experimento simplificado.

---

## 5) Critérios de aceite transversais

- Todos os botões assíncronos devem ficar desabilitados durante processamento.
- Campos obrigatórios devem exibir `*` ao lado do rótulo.
- Cada tela deve possuir estados: loading, empty (quando aplicável) e error.
- Toda recomendação gerada deve manter rastreabilidade de origem (referência/bloco).
- O fluxo completo deve preservar o eixo canônico: Dor → Resultado → Mecanismo → Prova → Oferta.
