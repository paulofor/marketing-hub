# Análise — erro `Divergência de superfície` na geração de landing com LHM

Data da análise: 2026-04-26 (UTC)

## 1) Evidência encontrada no banco (MCP)

Foi identificado um job `LANDING_PAGE_HTML` com falha `422` contendo exatamente a mensagem reportada:

- `created_at`: `2026-04-24T21:42:22` (UTC)
- erro retornado pelo backend (`/complete`):
  - `timestamp`: `2026-04-24T18:44:29.527029916-03:00`
  - `status`: `422`
  - `message`: `Divergência de superfície: landing-page-html deve reproduzir exatamente landing-page-wireframe.sectionOrder.surfaceSpec`

Também há execuções posteriores do mesmo experimento (`experiment_id = 15`) com `LANDING_PAGE_HTML` em `COMPLETED`, indicando falha pontual de aderência do output nessa tentativa específica.

## 2) Regra canônica aplicável (artefatos)

No cânone de artefatos, o `landingPageHtml` deve respeitar o contrato de superfície por seção com os atributos:

- `data-section-id`
- `data-surface-token`
- `data-surface-style`
- `data-surface-contrast`

A etapa de HTML precisa manter binding estrito com o `landingPageWireframe.sectionOrder[*].surfaceSpec`.

## 3) Validação ativa no backend que rejeitou

A rejeição ocorre no método:

- `validateLandingHtmlSurfaceConsistency(...)`

Fluxo de validação:

1. Lê `landingPageWireframe.sectionOrder[*].surfaceSpec` e monta a lista esperada (`extractExpectedSectionSurfaces`).
2. Extrai do HTML os atributos `data-section-id`, `data-surface-token`, `data-surface-style`, `data-surface-contrast` (`extractSectionSurfacesFromHtmlDocument`).
3. Ordena por `sectionId` e compara igualdade exata (`expectedSorted.equals(actualSorted)`).
4. Se diferente, lança `422` com a mensagem de divergência de superfície.

## 4) Diagnóstico no formato SOP (422)

### O que o modelo entregou (literal)

Na tentativa analisada, o backend recebeu um `landing-page-html` cuja superfície extraída do HTML **não bateu exatamente** com o wireframe ao finalizar o job (conforme mensagem de erro 422 acima).

> Observação operacional: o log persistido em banco contém a mensagem do erro, mas não persistiu o diff `expected vs actual` do WARN interno. Esse diff aparece no log de aplicação quando disponível em retenção.

### O que a especificação esperava (literal)

Para cada seção do `wireframe.sectionOrder`, o HTML precisa reproduzir exatamente:

- mesmo `sectionId` em `data-section-id`
- mesmo `surfaceToken` em `data-surface-token`
- mesmo `style` em `data-surface-style`
- mesmo `contrastMode` em `data-surface-contrast`

### Diferença entre a entrega e o esperado

A validação de igualdade estrita detectou divergência entre a lista esperada (wireframe) e a lista extraída do HTML final. Portanto, pelo menos um item teve:

- `sectionId` ausente/trocado, **ou**
- `surfaceToken` divergente, **ou**
- `style` divergente, **ou**
- `contrastMode` divergente.

### Ação corretiva recomendada

1. Reforçar no prompt de `landing-page-html` a obrigatoriedade de copiar `surfaceSpec` sem reinterpretar por seção.
2. No worker, adicionar checagem pré-envio para `/complete` (falhar antes) comparando superfícies esperadas x extraídas, com log explícito por `sectionId`.
3. Persistir no job (campo técnico) o diff objetivo `expected vs actual` para diagnóstico rápido sem depender de retenção de logs.
4. Manter retry automático, pois há evidência de sucesso em tentativas posteriores do mesmo experimento.

## 5) Conclusão objetiva

O que aconteceu foi uma rejeição contratual do backend (422) por não conformidade estrita entre superfícies do HTML gerado e o `surfaceSpec` canônico do wireframe naquela tentativa. Não foi erro de indisponibilidade; foi bloqueio de consistência de artefato.


## 6) Relação com a investigação de override por `landingPageDesignPreset.sectionPresets`

Sim, **há relação técnica direta** com a investigação que apontou override indevido no LHM.

- O erro observado (`Divergência de superfície`) nasce exatamente quando os atributos `data-surface-*` do HTML divergem do `wireframe.sectionOrder[].surfaceSpec`.
- Se o LHM aplicar `surfaceStyle/contrastMode` vindos de `sectionPresets` (preset visual) sobre o que veio do wireframe, a validação estrita de superfície pode reprovar com 422.
- Portanto, a correção de manter `surfaceSpec` do wireframe como fonte única para `data-surface-token`, `data-surface-style` e `data-surface-contrast` é compatível com a causa raiz descrita na outra investigação.

### Observação de data/horário

Nesta coleta específica via MCP, não apareceu registro com falha em `2026-04-25 21:46` (UTC-3). O registro mais próximo com essa mensagem foi:

- `2026-04-24T18:44:29.527-03:00` (erro 422 no `/complete`, experimento 15).

Ou seja: o sintoma é o mesmo; a diferença pode ser tentativa distinta, fuso/percepção de horário na UI, ou novo ciclo de geração já não presente na janela de retenção.
