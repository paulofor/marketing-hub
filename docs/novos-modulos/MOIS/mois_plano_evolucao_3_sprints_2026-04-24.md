# MOIS — Plano de evolução em 3 sprints (24/04/2026)

## 1) Contexto e objetivo

Este plano organiza a evolução do MOIS para sair do estágio atual de fundação funcional (módulo separado + façade no backend) e chegar a uma operação confiável para inteligência de ofertas de mercado.

O plano mantém o eixo central do Marketing Hub:

**Dor → Resultado → Mecanismo → Prova → Oferta**

E reforça as metas do módulo:
- gerar artefatos canônicos reutilizáveis;
- produzir sinais acionáveis de saturação, lacunas e diferenciação;
- manter rastreabilidade (lineage) e aderência a contratos.

---

## 2) Princípios de execução

1. **Backend como fachada institucional** e MOIS como bounded context operacional.
2. **Schema-first e lineage obrigatório** para cada artefato publicado.
3. **Persistência antes de sofisticação algorítmica**.
4. **Compatibilidade aditiva de contratos** (sem quebras abruptas).
5. **Evidência operacional por testes e observabilidade** em toda sprint.

---

## 3) Sprint 1 — Persistência e contratos canônicos

### Objetivo
Eliminar volatilidade do estado e consolidar o envelope canônico de artefatos.

### Entregas
- Persistir `discoveryRequest`, `sourceSnapshot`, `offerCard` e `insightReport` em MySQL 5.7 via backend (sem acesso direto de outros módulos ao banco).
- Introduzir versionamento explícito de schema para todos os artefatos MOIS (`v1`).
- Completar envelope com campos canônicos mandatórios (incluindo `createdBy`).
- Implementar idempotência básica para execução de `run` por `requestId`.
- Ajustar `GET /api/v1/mois/insight-reports` para aceitar e propagar filtro `category` no backend façade.

### Critérios de pronto
- Reinício do módulo não perde estado de requests/ofertas/relatórios.
- Artefatos recuperados em `GET /artifacts/{id}` incluem envelope completo + lineage mínimo.
- Contract tests backend↔MOIS cobrindo `category` e novos campos canônicos.
- Migrações Liquibase revisadas para MySQL 5.7 com `databaseChangeLog` YAML e `preConditions` adequadas.

### Métricas-alvo
- Taxa de sucesso de `run` >= 95% em ambiente de homologação.
- 100% dos artefatos com `artifactId`, `artifactType`, `schemaVersion`, `status`, `module`, `lineage`, `metadata`, `content`, `createdBy`.

### Riscos e mitigação
- **Risco:** divergência entre DTO e validação backend (422).
  - **Mitigação:** checklist de contrato + validação automatizada de payload por endpoint.

---

## 4) Sprint 2 — Coleta real, extração orientada a sinais e qualidade do dado

### Objetivo
Trocar fluxo sintético por pipeline de coleta e extração com evidência de origem.

### Entregas
- Pipeline de coleta real com seeds (queries/URLs), snapshots e normalização textual.
- Extração estruturada dos sinais principais:
  - promessa,
  - prova,
  - mecanismo alegado,
  - precificação,
  - padrão de funil.
- Pontuação de confiança por artefato derivado com referência de evidência (`evidenceRefs`).
- Regras de deduplicação de ofertas por URL canônica + assinatura de conteúdo.
- Observabilidade operacional: logs estruturados por `requestId` e `correlationId`.

### Critérios de pronto
- `run` gera artefatos baseados em fontes reais (não somente placeholders).
- Cada insight relevante aponta para fonte/snapshot de origem.
- Taxonomia de `sourceKind`, `primaryOfferType` e `proofType` documentada e validada.
- Testes unitários + integração cobrindo casos de duplicidade, fonte inválida e baixa confiança.

### Métricas-alvo
- Cobertura de campos essenciais de `offerCard` >= 85% nas coletas válidas.
- Deduplicação reduz redundância de ofertas em pelo menos 30% no mesmo request.
- 100% dos insights com lineage verificável.

### Riscos e mitigação
- **Risco:** variação de estrutura de páginas reduzindo precisão.
  - **Mitigação:** normalização por heurísticas + fallback por extratores por canal.

---

## 5) Sprint 3 — Consolidação analítica e acionabilidade para oferta

### Objetivo
Elevar o `marketOfferInsightReport` para recomendação prática de posicionamento e experimento.

### Entregas
- Refinar algoritmos de padrões repetidos e saturação por nicho/categoria/faixa de preço.
- Melhorar geração de `gapOpportunities` com critérios transparentes de prioridade/confiança.
- Incluir recomendações orientadas ao framework:
  - dor dominante,
  - resultado mais prometido,
  - mecanismo mais explorado,
  - prova mais usada,
  - ângulos de oferta subexplorados.
- Endpoint de resumo executivo para consumo por módulos de hipótese/oferta.
- Painel mínimo (ou payload estruturado) para leitura rápida dos principais sinais.

### Critérios de pronto
- Relatório final contém recomendações claras e justificadas por evidência.
- Backend e consumers conseguem usar o relatório sem transformação manual adicional.
- Testes de regressão de ranking/padrões evitando drift em alterações futuras.

### Métricas-alvo
- Tempo médio de geração do insight report <= 2 min por request padrão.
- Aumento da taxa de utilização do relatório por módulos consumidores.
- Redução de “insight genérico” (sem ação concreta) em revisão qualitativa interna.

### Riscos e mitigação
- **Risco:** relatório prolixo e pouco acionável.
  - **Mitigação:** template de saída com limites e foco em decisões (não em dump de sinais).

---

## 6) Sequenciamento recomendado (alto nível)

1. **Semana 1-2 (Sprint 1):** persistência + contratos + ajuste façade.
2. **Semana 3-4 (Sprint 2):** coleta real + extração + deduplicação + observabilidade.
3. **Semana 5-6 (Sprint 3):** consolidação analítica + recomendação acionável + endpoint executivo.

---

## 7) Definition of Done transversal

Para considerar a evolução concluída ao final das 3 sprints:

- contratos backend↔MOIS sincronizados e versionados;
- artefatos canônicos completos e rastreáveis;
- pipeline com coleta real e qualidade mínima mensurada;
- relatório final útil para decisão de oferta e experimento;
- cobertura de testes adequada e monitoramento operacional ativo.

---

## 8) Artefatos/documentos a atualizar a cada sprint

- `docs/canonical/system-governance-canon.v2.md` (se regra de governança mudar)
- `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (se contrato canônico evoluir)
- `docs/modelo-dados-experimento.md` (se houver novas entidades/relacionamentos)
- `docs/database/liquibase-mysql57.md` (se houver novas migrações/padrões)
- documentação de contrato do MOIS em `docs/novos-modulos/MOIS/`

