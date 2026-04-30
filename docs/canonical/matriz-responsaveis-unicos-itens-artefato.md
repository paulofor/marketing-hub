# Matriz detalhada — responsável único por item de artefato (pipeline de experimento)

## Objetivo
Definir, sem ambiguidade, **um único responsável canônico por campo/path** dos artefatos do pipeline de experimento, evitando co-ownership e divergência entre módulos/etapas.

## Regra geral de ownership
- Cada campo/path abaixo possui **apenas 1 dono canônico**.
- Outros estágios podem **consumir** o campo, mas não redefinir sua fonte de verdade.
- Em conflito, prevalece sempre o dono indicado nesta matriz.

## Convenção de leitura
- **Dono canônico**: etapa/módulo autorizado a definir o valor final do campo.
- **Consumidores principais**: etapas que usam o campo sem serem donas.
- **Gate de validação**: onde o backend rejeita inconsistência.

---

## 1) Envelope e metadados comuns

| Path canônico | Dono canônico | Consumidores principais | Gate de validação |
|---|---|---|---|
| `experimentMetadata.primary_variable` | `campaign-angle` | todas as etapas seguintes | validação de metadados no `complete` da etapa atual |
| `experimentMetadata.variant_id` | Backend (orquestração do experimento) | todas as etapas | criação de prompt e validação de lineage |
| `experimentMetadata.stage` | Backend (estado do experimento) | todas as etapas | validação de consistência de estágio |
| `experimentMetadata.control_or_treatment` | Backend (orquestração) | todas as etapas | validação de metadado obrigatório |
| `experimentMetadata.asset_role` | Etapa dona do artefato corrente | backend + worker | validação de contrato por seção |

## 2) `campaignAngle`

| Path canônico | Dono canônico | Consumidores principais | Gate de validação |
|---|---|---|---|
| `campaignAngle.primaryMessage` | `campaign-angle` | `ad-copy`, `landingPageCopy` | `complete` da etapa `campaign-angle` |
| `campaignAngle.mechanismSummary` | `campaign-angle` | `landingPageCopy`, `landingPageWireframe` | `complete` da etapa `campaign-angle` |
| `campaignAngle.proofSummary` | `campaign-angle` | `landingPageCopy`, `landingPageWireframe` | `complete` da etapa `campaign-angle` |
| `campaignAngle.ctaAnchor` | `campaign-angle` | `ad-copy`, `landingPageCopy`, `landingPageWireframe` | checks de continuidade |

## 3) `landingPageCopy`

| Path canônico | Dono canônico | Consumidores principais | Gate de validação |
|---|---|---|---|
| `landingPageCopy.pageGoal` | `landing-page-copy` | `landingPageWireframe`, `landingPageHtml` | `complete` da etapa `landing-page-copy` |
| `landingPageCopy.primaryCTA` | `landing-page-copy` | `landingPageWireframe`, `landingPageHtml` | checks `CTA_MATCH` |
| `landingPageCopy.hero.*` | `landing-page-copy` | `landingPageHtml` | estrutura mínima de hero no fechamento |
| `landingPageCopy.bodySections[*]` | `landing-page-copy` | `landingPageWireframe`, `landingPageHtml` | validação de lista e conteúdo |
| `landingPageCopy.ctaBlocks[*]` | `landing-page-copy` | `landingPageHtml` | coerência de CTA |
| `landingPageCopy.consistencyChecks` | `landing-page-copy` | backend (gate) | obrigatórios `CTA_MATCH` + `PROMISE_MATCH` |

## 4) `landingPageWireframe`

| Path canônico | Dono canônico | Consumidores principais | Gate de validação |
|---|---|---|---|
| `landingPageWireframe.sectionOrder[*].sectionId` | `landing-page-wireframe` | `landingPageImagePlanning`, `landingPageDesignPreset`, `landingPageHtml` | validação de estrutura e cobertura por seção |
| `landingPageWireframe.images[*].sectionId` | `landing-page-wireframe` | `landingPageImagePlanning`, `landingPageHtml` | validação de binding estrutural de imagem por seção |
| `landingPageWireframe.images[*].imageBindingKey` | `landing-page-wireframe` | `landingPageImagePlanning`, `landingPageHtml` | validação de unicidade e vínculo por seção |
| `landingPageWireframe.sectionOrder[*].surfaceSpec.surfaceToken` | `landing-page-wireframe` | `landingPageHtml` | validação de surface binding |
| `landingPageWireframe.sectionOrder[*].surfaceSpec.notes` | `landing-page-wireframe` | `landingPageHtml` (suporte) | validação estrutural do wireframe |
| `landingPageWireframe.formSpec.*` | `landing-page-wireframe` | `landingPageHtml`, apply-to-form | validação determinística de formulário |
| `landingPageWireframe.readingFlowSpec.*` | `landing-page-wireframe` | `landingPageHtml` | gate de qualidade do wireframe |
| `landingPageWireframe.conversionPathSpec.*` | `landing-page-wireframe` | `landingPageHtml` | gate de continuidade/comercial |

## 5) `landingPageImagePlanning`

| Path canônico | Dono canônico | Consumidores principais | Gate de validação |
|---|---|---|---|
| `landingPageImagePlanning.generationPrompt` | `landing-page-image-planning` | worker de imagem | validação de prompt obrigatório e não vazio |

## 6) `landingPageDesignPreset`

| Path canônico | Dono canônico | Consumidores principais | Gate de validação |
|---|---|---|---|
| `landingPageDesignPreset.sectionPresets[*].sectionId` | `landing-page-design-preset` | `landingPageHtml` | cobertura 1:1 com `wireframe.sectionOrder.sectionId` |
| `landingPageDesignPreset.sectionPresets[*].surfaceStyle` | `landing-page-design-preset` | `landingPageHtml` | validação de surface binding visual |
| `landingPageDesignPreset.sectionPresets[*].contrastMode` | `landing-page-design-preset` | `landingPageHtml` | validação de surface binding visual |
| `landingPageDesignPreset.theme.*` | `landing-page-design-preset` | `landingPageHtml` | validação de tema e acessibilidade |
| `landingPageDesignPreset.componentPresets.*` | `landing-page-design-preset` | `landingPageHtml` | validação de completude de preset |
| `landingPageDesignPreset.consistencyChecks` | `landing-page-design-preset` | backend (gate) | checks obrigatórios do preset |

## 7) `landingPageHtml`

| Path canônico | Dono canônico | Consumidores principais | Gate de validação |
|---|---|---|---|
| `landingPageHtml.htmlDocument` | `landing-page-html` | publicação + apply-to-form + Lead Portal | validação determinística final |
| `htmlDocument[data-section-id]` | `landing-page-html` (implementação), com source-of-truth de `wireframe.sectionId` | backend (validador) | divergência de superfície (422) |
| `htmlDocument[data-surface-token]` | `landing-page-html` (implementação), com source-of-truth de `wireframe.surfaceToken` | backend (validador) | divergência de superfície (422) |
| `htmlDocument[data-surface-style]` | `landing-page-html` (implementação), com source-of-truth de `designPreset.surfaceStyle` | backend (validador) | divergência de superfície (422) |
| `htmlDocument[data-surface-contrast]` | `landing-page-html` (implementação), com source-of-truth de `designPreset.contrastMode` | backend (validador) | divergência de superfície (422) |
| `landingPageHtml.formSpec.*` | `landing-page-html` (implementação), com source-of-truth de `wireframe.formSpec` | apply-to-form | divergência de formulário (422) |

---

## Regra de precedência para conflitos (normativa)
1. **Estrutura** (`sectionId`, `surfaceToken`, `formSpec`) → prevalece `landingPageWireframe`.
2. **Visual por seção** (`surfaceStyle`, `contrastMode`) → prevalece `landingPageDesignPreset`.
3. **Renderização final** (`htmlDocument`) deve refletir fielmente 1 + 2; divergência reprova no `complete`.

## Escopo
Esta matriz cobre os artefatos canônicos do pipeline principal de experimento (`campaign-angle` até `landing-page-html`). Artefatos de Avatar Sales Video permanecem sob seu cânone específico.
