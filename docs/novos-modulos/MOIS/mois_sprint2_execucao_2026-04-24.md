# MOIS — Execução da Sprint 2 (24/04/2026)

## Escopo executado

Implementação da Sprint 2 do plano `mois_plano_evolucao_3_sprints_2026-04-24.md` com foco em:

1. coleta real via HTTP para seeds (`seedUrls` e queries convertidas em URL de busca);
2. normalização textual e snapshots com evidência de origem;
3. extração heurística de sinais estruturados:
   - promessa,
   - prova,
   - mecanismo alegado,
   - precificação,
   - padrão de funil;
4. pontuação de confiança por oferta derivada;
5. `evidenceRefs` por oferta para rastreabilidade de lineage;
6. deduplicação por `canonicalUrl + contentSignature`;
7. observabilidade com logs estruturados por `requestId` e `correlationId`.

## Taxonomias ativas

### `sourceKind`
- `LANDING_PAGE`
- `SOCIAL`
- `VIDEO`
- `ARTICLE`

### `primaryOfferType` (heurístico atual)
- `DIGITAL_PRODUCT`
- `MENTORSHIP`
- `CONTENT`

### `proofType`
No estágio atual, `proofType` ainda é inferido em texto livre (`proofSummary`) a partir de sinais como depoimentos, caso, avaliação e percentuais.
A enumeração formal de `proofType` fica prevista para incremento aditivo na próxima iteração, preservando compatibilidade de contrato.

## Critérios de pronto cobertos

- `run` passa a produzir artefatos de fontes reais (sem placeholders sintéticos).
- ofertas incluem `evidenceRefs` apontando snapshots de origem.
- casos de fonte inválida e deduplicação cobertos por testes automatizados.
- caso de baixa confiança coberto por teste automatizado.
