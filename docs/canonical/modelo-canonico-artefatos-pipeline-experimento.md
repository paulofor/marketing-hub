# Modelo Canônico de Artefatos do Pipeline de Experimento

Este documento contém **apenas o esquema canônico** dos artefatos do pipeline.

> 🔵 **DIRETRIZ ESTRATÉGICA — PAPEL DO MDS NA CONSTRUÇÃO DO PRODUTO**
>
> O **MDS** é a principal fonte estruturada de informação para transformar dor de mercado em produto digital final.
>
> - Além de apoiar a definição de mecanismo, o MDS deve sustentar a construção de promessa, prova e oferta.
> - Os artefatos derivados do MDS devem manter rastreabilidade de origem e aderência comercial até a versão final do produto.
> - Sempre que houver conflito entre volume de conteúdo e qualidade de decisão comercial, priorizar evidência útil para conversão real.

> 🔴 **REGRA CANÔNICA EM DESTAQUE — LHM**
>
> O **LHM (Landing HTML Module)** é o módulo **determinístico** responsável por gerar o HTML final da landing page.
>
> - O LHM **não** é uma etapa de ideação criativa livre da copy.
> - O LHM deve renderizar de forma previsível a partir dos artefatos canônicos (ex.: `landingPageCopy` e `landingPageWireframe`) e contratos vigentes.
> - Mudanças no pipeline devem preservar essa responsabilidade para reduzir drift entre especificação, backend e página publicada.
> - **Regra de negócio mandatória**: a robustez arquitetural dos artefatos deve sempre servir ao objetivo de vendas (conversão, consistência de promessa, redução de fricção e avanço de funil).

## Observação importante — independência dos experimentos

- Cada experimento deve ser tratado de forma independente, sem reutilização implícita de artefatos entre experimentos.
- **Critério canônico de exclusividade**: todo artefato gerado direta **ou** indiretamente pelo pipeline de um experimento é considerado **exclusivo daquele experimento**.
- Portanto, para um artefato ser classificado como "não exclusivo", ele não pode ter sido produzido em nenhuma etapa do pipeline do experimento (incluindo derivações, transformações, consolidações ou enriquecimentos).

## Convenção de envelope (`artifact`)

```json
{
  "artifact": {
    "artifactType": "string",
    "artifactVersion": "v1",
    "status": "DRAFT | VALIDATED | APPROVED",
    "parentArtifactIds": ["string"],
    "content": {}
  }
}
```

## `sourceDocument`

```json
{
  "sourceType": "string",
  "sourceUrl": "string",
  "rawText": "string",
  "fetchedAt": "date-time",
  "permissionState": "string"
}
```

## `evidenceItem`

```json
{
  "claim": "string",
  "excerpt": "string",
  "url": "string",
  "citation": "string",
  "confidence": "number",
  "tags": ["string"]
}
```

## `opportunityMap`

```json
{
  "market": "string",
  "problem": "string",
  "demandSignals": ["string"],
  "competitionSignals": ["string"],
  "references": ["string"]
}
```

## `mechanismSpec`

```json
{
  "problem": "string",
  "causalModel": "string",
  "intervention": "string",
  "proofBase": ["string"],
  "limitations": ["string"]
}
```

## `offerSpec`

```json
{
  "promise": "string",
  "scope": "string",
  "deliverables": ["string"],
  "constraints": ["string"],
  "pricingIdea": "string"
}
```

## `campaignAngle`

```json
{
  "visualAngle": "string",
  "hook": "string",
  "promise": "string",
  "objections": ["string"],
  "messageMatch": "string"
}
```

## `landingPageCopy`

```json
{
  "pageGoal": "string",
  "messageMatchSource": "string",
  "messageMatchNotes": "string",
  "primaryCTA": "string",
  "complianceNotes": "string",
  "hero": {
    "eyebrow": "string",
    "headline": "string",
    "subheadline": "string",
    "promise": "string",
    "supportingCopy": "string",
    "proofBadge": "string",
    "microcopy": "string",
    "ctaLabel": "string",
    "ctaUrl": "string",
    "ctaMatchNotes": "string"
  },
  "bodySections": [
    {
      "sectionId": "string",
      "sectionType": "string",
      "title": "string",
      "summary": "string",
      "bullets": ["string"],
      "copy": "string",
      "ctaSupport": "string",
      "sectionDependsOn": "primaryPromise | mechanismSummary | proofSummary | primaryCTA",
      "messageMatchNotes": "string"
    }
  ],
  "ctaBlocks": [
    {
      "placement": "string",
      "ctaVariant": "string",
      "ctaLabel": "string",
      "ctaUrl": "string",
      "matchAdCta": "string",
      "ctaSupport": "string",
      "messageMatchNotes": "string"
    }
  ],
  "faq": [
    {
      "question": "string",
      "answer": "string",
      "objectionTag": "string"
    }
  ],
  "consistencyChecks": [
    {
      "check": "CTA_MATCH | PROMISE_MATCH | GOOGLE_LANDING_BEST_PRACTICES",
      "status": "PASS | FAIL | WARNING",
      "details": "string"
    }
  ]
}
```

### Regras canônicas de qualidade para geração da `landingPageCopy`

Para considerar a copy **rica** e **compatível com as especificações**, a etapa de geração deve cumprir os critérios abaixo antes da renderização pelo LHM:

1. **Cobertura completa de narrativa**
   - A copy deve cobrir explicitamente o eixo: **Dor → Resultado → Mecanismo → Prova → Oferta**.
   - Essa cobertura deve existir em `hero` + `bodySections` + `ctaBlocks` + `faq`.

2. **Densidade mínima de conteúdo**
   - `hero.supportingCopy` não pode ser vazio.
   - Cada item de `bodySections` deve conter `summary` e `copy` não vazios.
   - Cada item de `bodySections` deve conter ao menos 3 itens em `bullets`.
   - `faq` deve conter no mínimo 3 perguntas com `question` e `answer` preenchidos.
   - `ctaBlocks` deve conter no mínimo 2 variações (por exemplo: `mid` e `final` ou `hero` e `final`).

3. **Dependência da etapa de wireframe (ordem canônica)**
   - A geração de `landingPageCopy` acontece **depois** de `landingPageWireframe`; portanto, a copy **deve usar** os sinais estruturais definidos no wireframe (por exemplo, `sectionOrder`, `ctaSlot` e hierarquia de blocos) para manter consistência de narrativa e layout.
   - O alinhamento estrutural com layout acontece no `landingPageWireframe` e é refinado na `landingPageCopy`, preservando promessa, argumentação e clareza comercial.
   - Toda `ctaUrl` deve ser resolvida (sem placeholders como `{slug}`).

4. **Consistência de promessa e CTA**
   - `hero.promise`, `primaryCTA` e `ctaBlocks[*].matchAdCta` devem manter coerência semântica.
   - `consistencyChecks` deve incluir, no mínimo, os checks: `CTA_MATCH`, `PROMISE_MATCH` e `GOOGLE_LANDING_BEST_PRACTICES`.

5. **Mínimos comerciais de oferta (mandatórios)**
   - A copy deve explicitar risco reverso (garantia, teste ou política equivalente).
   - A copy deve apresentar escassez/urgência legítima (sem alegação artificial).
   - A copy deve apresentar ancoragem de valor (comparação clara entre custo e valor percebido).
   - A copy deve incluir prova específica e contextualizada (evitar prova genérica).

6. **Biblioteca de mecanismos de prova por nicho**
   - Priorizar combinação de pelo menos 2 tipos de prova entre: antes/depois, benchmark comparativo, micro-caso com número e evidência técnica simplificada.

7. **Critério de bloqueio**
   - Se qualquer regra acima falhar, o artefato permanece em `DRAFT` e não pode seguir para renderização final.
   - O LHM só deve processar copy com validação aprovada (`status = VALIDATED` ou `APPROVED`).

## `landingPageWireframe`

```json
{
  "pageGoal": "string",
  "variantLayoutId": "form-first | proof-first | story-first",
  "sectionOrder": [
    {
      "sectionId": "string",
      "sectionName": "string",
      "objective": "string",
      "contentType": "string",
      "copySource": "string",
      "uiNotes": "string",
      "messageMatchDependency": "string",
      "sectionDependsOn": "string",
      "mobilePriorityScore": "1..10",
      "dropOffRisk": "baixo | medio | alto",
      "surfaceSpec": {
        "surfaceToken": "string",
        "notes": "string"
      },
      "ctaSlot": {
        "hasCta": "boolean",
        "ctaLabel": "string",
        "ctaVariant": "hero | mid | final | sticky | inline",
        "matchAdCta": "string",
        "notes": "string"
      }
    }
  ],
  "mobilePriorityNotes": "string",
  "ctaPlacementNotes": "string",
  "formPlacementNotes": "string",
  "readingFlowSpec": {
    "scanPattern": "F-pattern | Z-pattern | mixed",
    "maxParagraphLinesMobile": "number",
    "bulletDensityPerSection": "number",
    "headlineClarityRule": "string",
    "cognitiveLoadNotes": "string"
  },
  "conversionPathSpec": {
    "primaryAction": "string",
    "ctaLabelCanonical": "string",
    "ctaLabelVariantsAllowed": ["string"],
    "stickyCtaMobile": {
      "enabled": "boolean",
      "triggerAfterSection": "string",
      "ctaLabel": "string",
      "ctaUrl": "string"
    },
    "microCommitments": ["string"],
    "frictionPoints": ["string"]
  },
  "proofPlan": {
    "requiredProofTypes": [
      "depoimento",
      "mini-caso-com-numero",
      "evidencia-tecnica-simplificada"
    ],
    "proofSectionIds": ["string"],
    "proofContinuityNotes": "string"
  },
  "trustSignalsSpec": {
    "brandIdentityRequired": "boolean",
    "heroIdentityItems": [
      "logo",
      "nome-da-marca",
      "promessa-curta",
      "prova-rapida"
    ],
    "authorityElements": ["string"],
    "privacyNoticeNearForm": "boolean",
    "privacyPolicyUrl": "string",
    "legalFooterItems": ["empresa", "cnpj", "contato", "politica-de-privacidade"]
  },
  "accessibilitySpec": {
    "minTextContrast": "4.5:1",
    "minTouchTargetPx": "44",
    "formFieldMinHeightPx": "44",
    "smallTextUsageNotes": "string"
  },
  "consistencyChecks": [
    {
      "check": "string",
      "status": "PASS | FAIL | WARNING",
      "details": "string"
    }
  ],
  "images": [{"sectionId":"string","sectionName":"string","imageBindingKey":"slug","objective":"string","imagePrompt":"string","negativePrompt":"string","layoutBinding":{"preferredDesktopPlacement":"left | right | center | background","preferredMobilePlacement":"above-copy | below-copy | inline | background","desktopAspectRatio":"string","mobileAspectRatio":"string"}}],
  "formSpec": {
    "formId": "string",
    "title": "string",
    "submitLabel": "string",
    "submitTarget": "string",
    "submitOwnership": "inside-form | external-with-form-attr",
    "fields": [
      {
        "name": "string",
        "label": "string",
        "type": "text | email | tel",
        "required": "boolean",
        "placeholder": "string"
      }
    ],
    "consent": {
      "enabled": "boolean",
      "required": "boolean",
      "label": "string"
    },
    "successState": {
      "title": "string",
      "message": "string"
    }
  }
}
```

> **Responsabilidade canônica (vigente):**
> - `landingPageWireframe` define **estrutura** (ordem, hierarquia, intenção de seção) e mantém em `surfaceSpec` apenas a âncora estrutural (`surfaceToken` + `notes`).
> - `style` e `contrastMode` são responsabilidade da etapa `landingPageDesignPreset.sectionPresets` (detalhe visual por `sectionId`).
> - Não existe co-ownership de campo: `surfaceToken` pertence ao wireframe; `style/contrastMode` pertencem ao design preset. Em caso de divergência, `landingPageDesignPreset` prevalece para os campos visuais.
> - Após `complete` da etapa de wireframe, o backend valida presença de `sectionOrder` estruturado e `surfaceSpec.surfaceToken` por seção antes de aceitar o artefato.
> - Antes de iniciar `landingPageHtml`, o backend executa pré-validação de consistência entre wireframe e design preset para antecipar falhas que antes apareciam apenas no fim do pipeline.

### Regras canônicas de qualidade para `landingPageWireframe` (enriquecido para conversão)

1. **Escaneabilidade mobile mandatória**
   - `readingFlowSpec.maxParagraphLinesMobile` deve ser `<= 4`.
   - `readingFlowSpec.bulletDensityPerSection` deve ser `>= 3` para seções de argumento e prova.
   - `sectionOrder[*].mobilePriorityScore` deve refletir risco de drop-off (se `dropOffRisk = alto`, score mínimo `>= 8`).

2. **Continuidade comercial por seção**
   - Cada seção deve declarar objetivo explícito coerente com **Dor → Resultado → Mecanismo → Prova → Oferta**.
   - `conversionPathSpec.primaryAction` deve ser idêntica ao CTA principal da copy.
   - `conversionPathSpec.ctaLabelCanonical` deve ser o rótulo dominante da página; variações só podem existir se listadas em `ctaLabelVariantsAllowed`.
   - Se `stickyCtaMobile.enabled = true`, `ctaLabel` e `ctaUrl` devem manter match semântico com `primaryCTA`.

3. **Plano de prova obrigatório**
   - `proofPlan.requiredProofTypes` deve conter pelo menos 2 tipos distintos de prova.
   - `proofSectionIds` deve apontar para seções existentes em `sectionOrder`.
   - Quando faltar prova contextualizada, o artefato não pode avançar para `APPROVED`.

4. **Ritmo visual e redução de fricção**
   - `conversionPathSpec.frictionPoints` deve mapear explicitamente pontos de abandono previstos.
   - Cada ponto de fricção mapeado deve ter mitigação em `uiNotes`, `ctaPlacementNotes` ou `formPlacementNotes`.
   - O wireframe deve reservar pelo menos 1 bloco de objeções (FAQ ou equivalente) antes do CTA final.

5. **Confiança e conformidade de coleta**
   - `trustSignalsSpec.brandIdentityRequired` deve ser `true` para páginas de captação com formulário.
   - `privacyNoticeNearForm` deve ser `true` e `privacyPolicyUrl` deve estar preenchida.
   - `legalFooterItems` deve conter ao menos empresa, contato e política de privacidade.

6. **Acessibilidade mínima obrigatória**
   - `accessibilitySpec.minTextContrast` deve ser no mínimo `4.5:1` para texto comum.
   - `accessibilitySpec.minTouchTargetPx` deve ser `>= 44`.
   - `formFieldMinHeightPx` deve ser `>= 44`.

7. **Critério de bloqueio**
   - Ausência de `readingFlowSpec`, `conversionPathSpec`, `proofPlan`, `trustSignalsSpec` ou `accessibilitySpec` mantém o artefato em `DRAFT`.
   - `landingPageHtml` só pode iniciar com wireframe em `VALIDATED` ou `APPROVED`.

## `landingPageImagePlanning`

```json
{
  "generationPrompt": "string"
}
```

> **Responsabilidade canônica (vigente):**
> - A etapa `landingPageImagePlanning` é responsável somente por gerar o prompt final de execução para o modelo de imagem.
> - Os bindings estruturais de imagem (por seção) são definidos no `landingPageWireframe` e apenas consumidos nesta etapa.

## `landingPageDesignPreset`

> Artefato canônico para separar decisão de estética/tema da etapa final de composição HTML.
> O objetivo é permitir evolução visual com previsibilidade, sem transformar o LHM em camada de ideação livre.
> Toda execução desta etapa via modelo deve solicitar resposta estritamente em JSON válido aderente ao schema canônico da própria etapa.

```json
{
  "presetId": "string",
  "lhmRuntime": {
    "baseCss": "string",
    "cssVersion": "string",
    "cssNotes": "string"
  },
  "theme": {
    "palette": {
      "background": "string",
      "surface": "string",
      "textPrimary": "string",
      "textMuted": "string",
      "brandPrimary": "string",
      "brandSecondary": "string",
      "ctaPrimary": "string",
      "ctaPrimaryHover": "string",
      "success": "string",
      "warning": "string",
      "border": "string"
    },
    "typography": {
      "fontFamily": "string",
      "baseSize": "string",
      "headingScale": "string",
      "lineHeightBody": "string",
      "lineHeightHeading": "string",
      "fontWeightRegular": "string",
      "fontWeightSemibold": "string",
      "fontWeightBold": "string",
      "maxLineLength": "string"
    },
    "spacing": {
      "scaleBase": "4|8",
      "sectionGapDesktop": "string",
      "sectionGapMobile": "string",
      "containerWidthDesktop": "string",
      "containerPaddingMobile": "string"
    },
    "accessibility": {
      "textContrastBody": "string",
      "textContrastSmall": "string",
      "focusVisibleStyle": "string",
      "touchTargetMinPx": "string"
    },
    "radius": {
      "card": "string",
      "field": "string",
      "button": "string"
    },
    "shadow": {
      "card": "string",
      "focusRing": "string"
    }
  },
  "sectionPresets": [
    {
      "sectionId": "string",
      "surfaceStyle": "band | solid | gradient-soft | image-tint",
      "contrastMode": "normal | high | soft",
      "layoutPreset": "hero-focus | form-focus | proof-grid | narrative-stack | faq-clean | cta-strong",
      "emphasis": "primary | secondary | support",
      "notes": "string"
    }
  ],
  "componentPresets": {
    "primitives": {
      "hero-title": { "tokens": ["string"], "attributes": ["string"] },
      "section-title": { "tokens": ["string"], "attributes": ["string"] },
      "body": { "tokens": ["string"], "attributes": ["string"] },
      "btn-primary": { "tokens": ["string"], "attributes": ["string"] },
      "btn-secondary": { "tokens": ["string"], "attributes": ["string"] },
      "field": { "tokens": ["string"], "attributes": ["string"] },
      "card": { "tokens": ["string"], "attributes": ["string"] },
      "faq-item": { "tokens": ["string"], "attributes": ["string"] }
    },
    "hero": {
      "titleMaxWidth": "string",
      "summaryMaxWidth": "string",
      "ctaVariant": "primary | ghost",
      "mediaPlacement": "left | right | center | background"
    },
    "form": {
      "fieldSpacing": "string",
      "labelWeight": "string",
      "submitStyle": "pill | block",
      "fieldHeight": "string",
      "errorStyle": "inline | tooltip"
    },
    "faq": {
      "variant": "accordion | stacked-cards"
    },
    "proof": {
      "cardVariant": "metric-first | testimonial-first | mixed",
      "showIdentity": "boolean",
      "highlightMetric": "boolean"
    },
    "trust": {
      "showBrandLockupInHero": "boolean",
      "showLegalFooter": "boolean",
      "privacyMicrocopyNearForm": "boolean",
      "authorityStripVariant": "logo-row | credential-cards | mixed"
    },
    "cta": {
      "stickyMobile": "boolean",
      "stickyOffsetBottom": "string",
      "pulseOnScrollStop": "boolean"
    }
  },
  "motion": {
    "enabled": "boolean",
    "intensity": "none | subtle | moderate"
  },
  "consistencyChecks": [
    {
      "check": "THEME_CONTRAST | CTA_VISUAL_HIERARCHY | MOBILE_READABILITY",
      "status": "PASS | FAIL | WARNING",
      "details": "string"
    }
  ]
}
```

### Cobertura obrigatória de elementos (fonte de verdade: pesquisa profunda)

O `landingPageDesignPreset` deve registrar, via `componentPresets.primitives` e tokens de `theme`, cobertura explícita para os elementos:
`<p>`, `<h1>`, `<h2>`, `<h3>`, `<ul>/<li>`, `<button>`/CTA, `<form>`, `<label>`, `<input>`, `<img>`.

Para cada elemento, o preset deve declarar atributos visuais trabalhados (tipografia, espaçamento, dimensão, contraste, foco, superfície e comportamento responsivo quando aplicável) e os tokens correspondentes. A referência de padrões é a seção **"Padrões de CSS e componentes por elemento"** de `docs/pesquisa-profunda/pesquisa-profunda-html-estilos.md`.

> **Atualização canônica (2026-04-29) — CSS base do LHM deve vir de artefato:**
> - A malha CSS base da landing (`baseCss`) deve ser definida no `landingPageDesignPreset.lhmRuntime.baseCss` (ou, por compatibilidade temporária, em `landingPageDesignPreset.baseCss`).
> - O `LandingHtmlModule` não deve definir regras de superfície/constraste/layout como decisão hardcoded fora dos JSONs canônicos.
> - Ausência de `baseCss` é quebra de contrato e deve bloquear a renderização da etapa `landingPageHtml`.

### Itens mínimos obrigatórios dentro de `lhmRuntime.baseCss`

Para garantir compatibilidade com o HTML emitido pelo LHM, o CSS canônico deve cobrir no mínimo os seletores abaixo (podendo estender):

- layout base: `body`, `main`, `.lhm-card`;
- tipografia/conteúdo: `h1`, `h2`, `p`, `ul`, `li`;
- formulário: `form`, `.field`, `label`, `input`, `button`, `#form-feedback`;
- FAQ: `.faq-list details`, `.faq-list summary`;
- variações de superfície e contraste usadas pelo wireframe/design preset:
  - `.lhm-surface-band`, `.lhm-surface-solid`, `.lhm-surface-gradient-soft`, `.lhm-surface-image-tint`;
  - `.lhm-normal`, `.lhm-high`, `.lhm-soft`.

> **Namespace canônico de classe (LHM):** o HTML determinístico deve usar classes prefixadas com `lhm-` para evitar colisão global e manter alinhamento 1:1 com `landingPageDesignPreset.lhmRuntime.baseCss`.

Se o wireframe/design preset introduzir novos `surfaceStyle` ou `contrastMode`, o `baseCss` correspondente deve ser atualizado no mesmo ciclo de versão.

> `landingPageDesignPreset.sectionPresets` é a fonte canônica de `surfaceStyle` e `contrastMode` por `sectionId` para renderização final do HTML.
> Após `complete` da etapa de design preset, o backend valida se `sectionPresets` cobre todas as `sectionId` do wireframe atual.

### Regras canônicas de qualidade para `landingPageDesignPreset` (preset rico)

1. **Legibilidade e leitura orientada à conversão**
   - `theme.typography.maxLineLength` deve ficar entre `55ch` e `75ch`.
   - `theme.typography.lineHeightBody` deve ser `>= 1.5`.
   - `theme.spacing.sectionGapMobile` não pode ser inferior a `48px`.

2. **Hierarquia de CTA obrigatória**
   - `theme.palette.ctaPrimary` deve ter contraste AA com o fundo predominante da seção.
   - O preset de CTA (`componentPresets.cta`) deve definir comportamento mobile quando existir CTA sticky.
   - O texto dominante de CTA no preset deve respeitar `conversionPathSpec.ctaLabelCanonical` do wireframe.
   - `sectionPresets` de alta intenção comercial (hero, oferta, fechamento) devem usar `emphasis = primary` ou justificar em `notes`.

3. **Prova visual com identidade**
   - `componentPresets.proof.showIdentity` deve ser `true` para páginas de venda direta.
   - Quando `highlightMetric = true`, a métrica principal deve estar na primeira dobra da seção de prova.

4. **Consistência wireframe ↔ preset**
   - Todo `sectionId` do wireframe deve ter 1 preset correspondente e sem duplicidade.
   - `layoutPreset` em `sectionPresets` deve ser compatível com `contentType` e `objective` do wireframe.
   - Incompatibilidades devem ser registradas em `consistencyChecks` como `FAIL`.

5. **Critério de bloqueio**
   - Preset sem tokens mínimos (`palette`, `typography`, `spacing`, `accessibility`, `componentPresets.cta`, `componentPresets.trust`) permanece em `DRAFT`.
   - O LHM não deve renderizar HTML final quando `THEME_CONTRAST`, `CTA_VISUAL_HIERARCHY` ou `MOBILE_READABILITY` estiver `FAIL`.

## `landingPageHtml`

> **Owner canônico de composição:** **LHM (Landing HTML Module)**.
> O LHM é o módulo de backend responsável por receber os insumos aprovados
> (`wireframe`, `copy`, `designPreset` e `imagens com URL`) e consolidá-los no
> `htmlDocument` final da landing.
>
> **Leitura operacional obrigatória para evolução do LHM:** `docs/canonical/lhm-evolution-guide.v1.md`.
>
> **Atualização canônica (2026-04-27) — geração dupla obrigatória:**
> - a etapa `landingPageHtml` passa a gerar **duas versões públicas** da landing para o mesmo `experimentId`:
>   1. `deterministic` (renderizada pelo **LHM**);
>   2. `ai` (renderizada por geração de IA).
> - as duas versões devem receber **a mesma entrada canônica** (`landingPageCopy`, `landingPageWireframe`, `landingPageDesignPreset` e `landingPageImagePlanning` resolvido com URLs finais);
> - as duas versões devem passar pelas **mesmas validações canônicas** antes de qualquer publicação.

> Cada documento HTML gerado pela etapa `landingPageHtml` traz atributos de dados para vincular os visuais planejados aos ativos finais.
> Os principais atributos são `data-image-section-id` (mapeia o `planning_item_key`), `data-image-binding-key` (chave semântica)
> e `data-image-role`/`data-conversion-role` (indicam o papel do ativo). Esses marcadores permitem que o pipeline substitua
> placeholders genéricos por imagens aprovadas automaticamente durante a publicação das landings.
>
> **Contrato operacional (vigente a partir de 2026-04-22):**
> - a resposta do modelo para a etapa `landingPageHtml` deve ser **HTML puro** (documento final completo);
> - é proibido retornar envelope JSON, markdown, bloco ``` ou campo textual contendo JSON serializado;
> - o backend do Lead Portal **não** deve tentar “desempacotar” HTML de payload misto/JSON: entradas fora de HTML puro devem ser rejeitadas.
> - para atributos canônicos de binding/superfície no HTML (`data-section-id`, `data-surface-token`, `data-surface-style`, `data-surface-contrast`), o backend normaliza artefatos de serialização antes da validação estrita: aspas codificadas (`&quot;`, `&#34;`, `&#x22;`), versões escapadas com barra invertida (ex.: `\\&quot;`) e tokens de quebra de linha escapados (`\\n`, `\\r`, `\\t`). O warning operacional deve permanecer para correção definitiva no worker/prompt.
>
> **Atualização canônica (2026-04-28) — entrada/saída da etapa `landingPageHtml`:**
> - para execução do job `LANDING_PAGE_HTML` no Worker IA, o conteúdo retornado pelo modelo deve chegar em `response.output_text` como **texto HTML bruto**;
> - respostas no formato `{"landingPageHtml":{"htmlDocument":"..."}}` passam a ser consideradas **quebra de contrato de etapa** (não apenas variação de formato);
> - o parse canônico da etapa deve priorizar texto HTML puro e rejeitar qualquer tentativa de interpretar JSON como fallback funcional.

```json
{
  "htmlVariants": {
    "deterministic": {
      "generator": "LHM",
      "htmlDocument": "string",
      "publicUrl": "string",
      "validationSummary": {
        "status": "PASS | FAIL",
        "checks": [
          {
            "check": "CTA_MATCH | PROMISE_MATCH | IMAGE_PLAN_BINDING | SURFACE_SPEC_BINDING | FORM_SPEC_BINDING | FORM_USABILITY",
            "status": "PASS | FAIL | WARNING",
            "details": "string"
          }
        ]
      }
    },
    "ai": {
      "generator": "AI_WORKER",
      "htmlDocument": "string",
      "publicUrl": "string",
      "validationSummary": {
        "status": "PASS | FAIL",
        "checks": [
          {
            "check": "CTA_MATCH | PROMISE_MATCH | IMAGE_PLAN_BINDING | SURFACE_SPEC_BINDING | FORM_SPEC_BINDING | FORM_USABILITY",
            "status": "PASS | FAIL | WARNING",
            "details": "string"
          }
        ]
      }
    }
  },
  "canonicalInputHash": "string",
  "formSpec": {
    "formId": "string",
    "title": "string",
    "submitLabel": "string",
    "submitTarget": "string",
    "submitTargetTemplateRule": "Quando usar placeholder de slug, manter exatamente {slug} (sem codificar para %7Bslug%7D).",
    "submissionRuntime": {
      "mode": "async-fetch",
      "onSubmit": {
        "preventDefault": "boolean",
        "validityGate": "checkValidity+reportValidity",
        "request": {
          "urlSource": "form.action",
          "methodSource": "form.method.toUpperCase()",
          "bodySource": "new FormData(form)",
          "payloadPartName": "payload",
          "payloadPartContentType": "application/json"
        },
        "buttonState": {
          "disableDuringRequest": "boolean",
          "loadingLabel": "string",
          "restoreLabelAfterRequest": "string"
        },
        "successFeedback": {
          "inlineElementId": "string",
          "displayMode": "block|flex|inline-block",
          "resetForm": "boolean",
          "waitForEmailMessage": "string"
        },
        "errorFeedback": {
          "mode": "inline|alert",
          "message": "string"
        }
      }
    },
    "cta": {
      "label": "string",
      "target": "string",
      "variant": "hero | mid | final | sticky | inline"
    },
    "fields": [
      {
        "name": "string",
        "label": "string",
        "type": "text | email | tel",
        "required": "boolean",
        "placeholder": "string"
      }
    ],
    "consent": {
      "enabled": "boolean",
      "required": "boolean",
      "label": "string"
    },
    "successState": {
      "title": "string",
      "message": "string",
      "nextStepHint": "string"
    }
  },
  "summary": "string",
  "imagePlacementContract": {
    "requiredDataAttributes": [
      "data-image-desktop-placement",
      "data-image-mobile-placement"
    ],
    "requiredDesktopClasses": [
      "image-placement-left",
      "image-placement-right",
      "image-placement-center",
      "image-placement-background"
    ],
    "requiredMobileClasses": [
      "image-mobile-above-copy",
      "image-mobile-below-copy",
      "image-mobile-inline",
      "image-mobile-background"
    ]
  },
  "consistencyChecks": [
    {
      "check": "CTA_MATCH | PROMISE_MATCH | IMAGE_PLAN_BINDING | SURFACE_SPEC_BINDING | FORM_SPEC_BINDING | FORM_USABILITY",
      "status": "PASS | FAIL | WARNING",
      "details": "string"
    }
  ]
}
```

### Regras canônicas obrigatórias — geração dupla `landingPageHtml`

1. **Entrada única e idêntica**
   - `deterministic` e `ai` devem usar o mesmo snapshot de entrada canônica do experimento.
   - Divergência de snapshot de entrada entre variantes é bloqueio de domínio.

2. **Paridade de validação**
   - As duas variantes devem executar exatamente o mesmo conjunto de checks canônicos.
   - Não é permitido “afrouxar” validação da variante `ai`.

3. **Publicação em URL pública dupla**
   - O backend deve publicar duas URLs rastreáveis e auditáveis no mesmo ciclo:
     - `publicUrl` da variante `deterministic`;
     - `publicUrl` da variante `ai`.
   - As duas URLs devem permanecer vinculadas ao mesmo `experimentId`.

4. **Critério de bloqueio**
   - Se qualquer variante falhar em check crítico (`CTA_MATCH`, `PROMISE_MATCH`, `FORM_SPEC_BINDING` ou `FORM_USABILITY`), a etapa permanece bloqueada para publicação oficial até correção.

## `landingCodeBundle`

```json
{
  "html": "string",
  "css": "string",
  "js": "string",
  "imageRefs": ["string"],
  "metadata": {
    "model": "string",
    "prompt": "string"
  }
}
```

## Fluxo automático do pipeline (referência normativa)

Este documento define o **schema canônico dos artefatos**.

A orquestração da fila automática (ordem de etapas, estados, progressão, bloqueios e retomada)
fica definida no documento:

- `docs/canonical/experiments-automation-flow-canon.v1.md`

Regra prática:

- este arquivo responde a "qual artefato e qual estrutura";
- o cânone de automação responde a "em qual ordem, com quais estados e com quais gatilhos".

### Regra canônica de publicação da landing (resumo executivo obrigatório)

Independentemente dos detalhes internos de orquestração, a experiência oficial
de publicação da landing para o usuário deve seguir exatamente:

1. geração da landing em **duas variantes** (LHM determinístico + IA) com a mesma entrada;
2. aprovação do usuário;
3. sistema cria/publica as **duas URLs finais** e aplica o pixel do nicho automaticamente em ambas.

Regras mandatórias:

- é proibido exigir do usuário uma segunda aprovação para concluir publicação;
- é proibido exigir inserção manual do pixel após a aprovação;
- ambas as URLs publicadas e o pixel aplicado em cada variante devem ficar auditáveis no backend.
- a ação oficial de aprovação/publicação deve estar disponível na aba `Landing` do experimento (não apenas em abas auxiliares).


---

## Apêndice — Avatar Sales Video (migração do cânone de artefatos)

> Conteúdo migrado de `docs/novos-modulos/avatar/melhores/avatar-sales-video-canonical-artifacts-initial.md` em 2026-04-23 para centralização canônica.

# Avatar Sales Video — Documento Canônico Inicial de Artefatos

## 1. Finalidade

Este documento define o **cânone inicial de artefatos** do módulo **Avatar Sales Video** no Marketing Hub.

Ele existe para:

- padronizar os artefatos produzidos e consumidos pelo módulo;
- reduzir ambiguidades entre backend, `ai-worker`, `video-management-service` e frontend;
- permitir versionamento, lineage, validação e evolução controlada;
- orientar o Codex em mudanças sem quebrar contratos já definidos;
- separar artefatos de domínio de artefatos técnicos transitórios.

Este documento é **inicial**.  
Ele define o primeiro conjunto canônico de artefatos necessários para operar o módulo com segurança e evoluí-lo sem drift semântico.

---

## 2. Relação com outros documentos canônicos

Este documento deve ser lido em conjunto com:

- `docs/canonical/system-governance-canon.v2.md`
- `docs/canonical/avatar-sales-video-canonical-rules.md`

## Regra obrigatória

O documento de regras do módulo prevalece em caso de dúvida sobre:

- fronteiras de responsabilidade;
- persistência;
- fluxo entre serviços;
- publicação;
- compliance;
- rollout;
- anti-padrões.

Este documento trata do **cânone de artefatos**, não substitui o documento canônico de regras.

---

## 3. Princípios do cânone de artefatos

## 3.1 Artefato não é tabela

Um artefato canônico é uma **unidade semântica tipada e versionada do domínio ou da operação do módulo**.

A persistência relacional pode representar esse artefato em uma ou mais tabelas, mas:

- a tabela **não redefine** o significado do artefato;
- o backend persiste e serve o artefato;
- o significado canônico do artefato é definido neste documento.

## 3.2 Backend como fonte de verdade operacional

O backend é a **fonte de verdade operacional e persistente** dos artefatos do módulo.

Isso significa que:

- workers não têm estado canônico próprio;
- providers externos não definem o estado final do domínio;
- o frontend não cria artefatos canônicos fora do backend.
- resultados e evidências produzidos pelo MDS devem ser persistidos no backend como base informacional do produto digital final.

## 3.3 Schema-first

Todo artefato canônico deve ter:

- tipo explícito;
- versão explícita;
- envelope comum;
- payload com estrutura conhecida;
- regras mínimas de lineage;
- campo ou mecanismo de status quando aplicável.

## 3.4 Artefatos devem ser estáveis o suficiente para consumo

Artefatos devem poder ser consumidos por:

- backend;
- `ai-worker`;
- `video-management-service`;
- frontend administrativo;
- runtime público da landing;

sem depender de interpretação informal de texto.

## 3.5 Artefatos de domínio e artefatos transitórios não são a mesma coisa

Nem toda resposta de provider deve virar artefato canônico.

### Deve virar artefato canônico:
- o que tem valor de domínio;
- o que precisa ser auditado;
- o que precisa ser versionado;
- o que precisa ser exibido, reaproveitado ou publicado.

### Não deve virar artefato canônico:
- qualquer payload transitório sem valor durável;
- respostas efêmeras sem necessidade de reuso;
- dados técnicos temporários que só existem durante uma chamada.

---

## 4. Envelope canônico comum

Todo artefato canônico do módulo deve seguir um envelope lógico comum.

## 4.1 Campos comuns obrigatórios

- `artifactId`
- `artifactType`
- `schemaVersion`
- `module`
- `tenantId`
- `createdAt`
- `updatedAt`
- `producedBy`
- `lineage`
- `payload`

## 4.2 Campos comuns recomendados

- `statusLabel`
- `productId`
- `landingPageId`
- `profileId`
- `scriptId`
- `jobId`
- `slotId`
- `correlationId`
- `notes`
- `checks`

## 4.3 Estrutura lógica recomendada

```json
{
  "artifactId": "uuid-or-stable-id",
  "artifactType": "avatar.salesVideoProfile.v1",
  "schemaVersion": "1.0.0",
  "module": "avatar-sales-video",
  "tenantId": "tenant-x",
  "productId": 123,
  "landingPageId": 456,
  "profileId": 789,
  "jobId": null,
  "slotId": null,
  "statusLabel": "ACTIVE",
  "createdAt": "2026-04-16T00:00:00Z",
  "updatedAt": "2026-04-16T00:00:00Z",
  "correlationId": "corr-xyz",
  "producedBy": {
    "component": "backend/ads-service",
    "actorType": "system",
    "actorId": "sales-video-service"
  },
  "lineage": {
    "parentArtifactIds": [],
    "sourceJobIds": [],
    "sourceAssetIds": []
  },
  "checks": {
    "validated": true,
    "approved": false
  },
  "payload": {}
}
```

---

## 5. Convenção de naming

## 5.1 Padrão obrigatório

Formato recomendado:

`avatar.<artifactName>.v<major>`

### Exemplos
- `avatar.salesVideoProfile.v1`
- `avatar.salesVideoScriptVersion.v1`
- `avatar.salesVideoStoryboard.v1`
- `avatar.salesVideoRenderRequest.v1`
- `avatar.salesVideoRenderJob.v1`
- `avatar.salesVideoJobEvent.v1`
- `avatar.salesVideoAssetBundle.v1`
- `avatar.landingVideoSlotBinding.v1`
- `avatar.landingVideoSlotHistorySnapshot.v1`
- `avatar.salesVideoComplianceRecord.v1`

## 5.2 Regras de naming

- usar nomes de domínio claros;
- evitar nomes genéricos como `data`, `result`, `payloadFinal`;
- distinguir artefato de solicitação, execução, resultado e publicação;
- distinguir artefato canônico de objeto técnico transitório.

---

## 6. Labels de ciclo de vida do artefato

Os labels abaixo são do **artefato**, não necessariamente do job.

## Labels recomendados

- `DRAFT`
- `READY`
- `APPROVED`
- `ACTIVE`
- `PUBLISHED`
- `FAILED`
- `ARCHIVED`

## Regras

- nem todo artefato precisa usar todos os labels;
- jobs continuam tendo seu próprio conjunto de estados operacionais;
- `statusLabel` de artefato não deve substituir o status operacional do job;
- quando houver ambos, deve ficar claro qual é o status do artefato e qual é o status da execução.

---

## 7. Lineage canônico

Todo artefato relevante deve deixar trilha mínima de origem.

## 7.1 Campos mínimos de lineage

- `parentArtifactIds`
- `sourceJobIds`
- `sourceAssetIds`

## 7.2 Regras

- artefatos derivados devem apontar para os artefatos pais;
- se um artefato nasce de um job, o `jobId` ou `sourceJobIds` deve ser preservado;
- se um artefato representa publicação de asset, os assets usados devem poder ser rastreados;
- lineage deve permitir auditoria sem depender de inferência manual.

---

# 8. Catálogo inicial de artefatos canônicos

---

## 8.1 `avatar.salesVideoProfile.v1`

### Finalidade
Representa a configuração canônica de um tipo de vídeo de vendas para uma oferta, produto ou landing.

### Produzido por
- backend

### Consumido por
- backend
- frontend
- `ai-worker`
- `video-management-service`

---

## 8.10 `avatar.salesVideoCommercialPlaybook.v1`

### Finalidade
Representa variações comerciais iniciais (objeção + CTA) por nicho para orientar iterações da Sprint V7.

### Produzido por
- backend

### Consumido por
- backend
- frontend administrativo

### Campos mínimos recomendados
- `profileId`
- `tenantId`
- `nicheKey`
- `variantKey`
- `objectionText`
- `ctaText`
- `active`

---

## 8.11 `avatar.salesVideoConversionEvent.v1`

### Finalidade
Representa fato de conversão associado ao módulo para aprendizado comercial e comparação entre script/provider.

### Produzido por
- backend (via endpoint canônico de ingestão)

### Consumido por
- backend
- frontend administrativo

### Campos mínimos recomendados
- `profileId`
- `jobId` (opcional)
- `scriptId` (opcional)
- `tenantId`
- `eventType`
- `eventValue`
- `currencyCode`
- `occurredAt`
- `source`

---

## 8.12 `avatar.salesVideoPerformanceSummary.v1`

### Finalidade
Projeção agregada para revisão comercial inicial da Sprint V7, consolidando eventos totais, leads, compras e receita por variação.

### Produzido por
- backend

### Consumido por
- frontend administrativo
- operação comercial

### Campos mínimos recomendados
- `profileId`
- `tenantId`
- `totalEvents`
- `totalLeads`
- `totalPurchases`
- `totalRevenue`
- `variants[]` com `scriptId`, `providerName`, `variantKey`, `events`, `leads`, `purchases`, `revenue`

### Pais típicos
- nenhum obrigatório

### Payload mínimo recomendado

```json
{
  "videoKind": "HERO",
  "title": "Vídeo principal da landing",
  "language": "pt-BR",
  "targetDurationSeconds": 30,
  "providerFamily": "EXTERNAL_VIDEO_MODULE",
  "providerName": "provider-x",
  "personaStyle": "consultivo",
  "voiceStyle": "natural",
  "landingPageId": 456,
  "productId": 123
}
```

### Invariantes
- deve existir antes de qualquer geração de script ou render;
- é a âncora principal do fluxo;
- não representa o job;
- não representa o asset final.

---

## 8.2 `avatar.salesVideoScriptVersion.v1`

### Finalidade
Representa uma versão editorial do script do vídeo, com seus campos principais aprováveis e auditáveis.

### Produzido por
- `ai-worker` via backend
- operador humano via backend

### Consumido por
- backend
- frontend
- `video-management-service`

### Pais típicos
- `avatar.salesVideoProfile.v1`

### Payload mínimo recomendado

```json
{
  "version": 3,
  "source": "OPENAI",
  "model": "gpt-4o",
  "scriptText": "texto completo do script",
  "hookText": "gancho inicial",
  "ctaText": "chamada para ação",
  "captionText": "legenda resumida",
  "approval": {
    "approved": true,
    "approvedBy": "user@example.com",
    "approvedAt": "2026-04-16T00:00:00Z"
  }
}
```

### Invariantes
- deve ser versionado;
- render produtivo deve depender de versão aprovada;
- não deve ser sobrescrito de forma destrutiva;
- deve preservar rastreabilidade de origem.

---

## 8.3 `avatar.salesVideoStoryboard.v1`

### Finalidade
Representa o storyboard estruturado da peça, quando existir, para orientar render, composição de cena ou validação editorial.

### Produzido por
- `ai-worker` via backend
- backend em fluxo manual estruturado

### Consumido por
- backend
- frontend
- `video-management-service`

### Pais típicos
- `avatar.salesVideoProfile.v1`
- `avatar.salesVideoScriptVersion.v1`

### Payload mínimo recomendado

```json
{
  "scenes": [
    {
      "sceneId": "scene-1",
      "purpose": "hook",
      "speechText": "texto falado",
      "visualIntent": "avatar em destaque",
      "durationSeconds": 5
    }
  ],
  "totalDurationSeconds": 30
}
```

### Invariantes
- storyboard não substitui o script completo;
- cenas devem ser rastreáveis por ordem e propósito;
- se existir, deve ser coerente com a versão de script usada no render.

---

## 8.4 `avatar.salesVideoRenderRequest.v1`

### Finalidade
Representa a intenção canônica de renderizar uma peça a partir de um perfil e de um script aprovado.

### Produzido por
- backend

### Consumido por
- backend
- `video-management-service`

### Pais típicos
- `avatar.salesVideoProfile.v1`
- `avatar.salesVideoScriptVersion.v1`
- opcionalmente `avatar.salesVideoStoryboard.v1`

### Payload mínimo recomendado

```json
{
  "providerFamily": "EXTERNAL_VIDEO_MODULE",
  "providerName": "provider-x",
  "requestedBy": "user@example.com",
  "requestedAt": "2026-04-16T00:00:00Z",
  "scriptVersion": 3,
  "renderMode": "PRODUCTION"
}
```

### Invariantes
- não é o job em si;
- não representa progresso;
- é o artefato de solicitação que dá origem ao job.

---

## 8.5 `avatar.salesVideoRenderJob.v1`

### Finalidade
Representa a unidade canônica de execução assíncrona do render.

### Produzido por
- backend

### Consumido por
- backend
- `video-management-service`
- frontend

### Pais típicos
- `avatar.salesVideoRenderRequest.v1`

### Payload mínimo recomendado

```json
{
  "jobType": "RENDER",
  "status": "VIDEO_REQUESTED",
  "providerFamily": "EXTERNAL_VIDEO_MODULE",
  "providerName": "provider-x",
  "providerJobId": null,
  "progressPercent": 0,
  "retryAttempt": 1,
  "failureCode": null,
  "failureDetail": null,
  "requestedAt": "2026-04-16T00:00:00Z",
  "startedAt": null,
  "finishedAt": null,
  "expiresAt": null
}
```

### Invariantes
- status operacional do render pertence aqui;
- o provider pode influenciar, mas não dita o estado final do domínio;
- retries devem ser auditáveis;
- histórico deve ser preservado por eventos, não por sobregravação sem trilha.

---

## 8.6 `avatar.salesVideoJobEvent.v1`

### Finalidade
Representa a trilha de eventos do job para auditoria, operação e diagnóstico.

### Produzido por
- backend
- opcionalmente a partir de reportes vindos de workers

### Consumido por
- backend
- frontend
- operação técnica

### Pais típicos
- `avatar.salesVideoRenderJob.v1`

### Payload mínimo recomendado

```json
{
  "eventType": "PROGRESS_REPORTED",
  "oldStatus": "VIDEO_REQUESTED",
  "newStatus": "VIDEO_PROCESSING",
  "message": "provider aceitou o render",
  "details": {
    "progressPercent": 35
  },
  "occurredAt": "2026-04-16T00:00:00Z"
}
```

### Invariantes
- eventos devem ser cumulativos;
- eventos não substituem o job;
- eventos precisam ser suficientes para reconstruir a história operacional.

---

## 8.7 `avatar.salesVideoProviderExecution.v1`

### Finalidade
Representa o registro técnico da execução junto ao provider externo, quando for necessário preservar detalhes operacionais além do job.

### Produzido por
- `video-management-service` via backend

### Consumido por
- backend
- operação técnica

### Pais típicos
- `avatar.salesVideoRenderJob.v1`

### Payload mínimo recomendado

```json
{
  "providerName": "provider-x",
  "providerJobId": "ext-123",
  "requestFingerprint": "hash-ou-fingerprint",
  "externalStatus": "processing",
  "externalMetadata": {
    "region": "us-east-1"
  },
  "lastSyncedAt": "2026-04-16T00:00:00Z"
}
```

### Invariantes
- este artefato é técnico e secundário;
- ele não substitui o job canônico;
- só deve existir quando agregar valor de auditoria/operação.

---

## 8.8 `avatar.salesVideoAssetBundle.v1`

### Finalidade
Representa o conjunto canônico de assets resultantes de um render que ficaram disponíveis no backend.

### Produzido por
- backend, após ingestão de assets enviados pelo `video-management-service`

### Consumido por
- backend
- frontend
- landing pública

### Pais típicos
- `avatar.salesVideoRenderJob.v1`

### Payload mínimo recomendado

```json
{
  "videoAssetId": 1001,
  "posterAssetId": 1002,
  "captionAssetId": 1003,
  "assetUrls": {
    "video": "/assets/1001",
    "poster": "/assets/1002",
    "caption": "/assets/1003"
  },
  "technicalMetadata": {
    "providerName": "provider-x",
    "providerJobId": "ext-123"
  }
}
```

### Invariantes
- publicação deve depender deste artefato ou de seus equivalentes canônicos;
- URLs efêmeras de provider não devem ser tratadas como publicação final;
- vídeo, poster e legenda devem permanecer rastreáveis como assets distintos.

---

## 8.9 `avatar.landingVideoSlotBinding.v1`

### Finalidade
Representa a associação canônica entre um bundle de vídeo pronto e um slot publicado na landing.

### Produzido por
- backend

### Consumido por
- backend
- frontend administrativo
- runtime público da landing

### Pais típicos
- `avatar.salesVideoAssetBundle.v1`
- `avatar.salesVideoProfile.v1`

### Payload mínimo recomendado

```json
{
  "landingPageId": 456,
  "slotName": "hero-video",
  "assetBundleRef": {
    "videoAssetId": 1001,
    "posterAssetId": 1002,
    "captionAssetId": 1003
  },
  "playback": {
    "autoplay": true,
    "muted": true,
    "loop": false,
    "controlsEnabled": true,
    "lazyLoad": true
  },
  "publishedBy": "user@example.com",
  "publishedAt": "2026-04-16T00:00:00Z"
}
```

### Invariantes
- a landing pública só deve depender do que estiver ligado canonicamente ao slot;
- o slot é artefato de publicação, não de render;
- o slot não deve apontar para provider diretamente.

---

## 8.10 `avatar.landingVideoSlotHistorySnapshot.v1`

### Finalidade
Representa o snapshot histórico de alteração/publicação de um slot.

### Produzido por
- backend

### Consumido por
- backend
- frontend administrativo
- operação/auditoria

### Pais típicos
- `avatar.landingVideoSlotBinding.v1`

### Payload mínimo recomendado

```json
{
  "slotName": "hero-video",
  "snapshotType": "PUBLISHED",
  "previousAssetBundleRef": null,
  "currentAssetBundleRef": {
    "videoAssetId": 1001,
    "posterAssetId": 1002,
    "captionAssetId": 1003
  },
  "changedBy": "user@example.com",
  "changedAt": "2026-04-16T00:00:00Z"
}
```

### Invariantes
- histórico deve ser cumulativo;
- snapshots devem permitir reconstituir a linha do tempo da publicação;
- o histórico não deve depender de inferência sobre logs soltos.

---

## 8.11 `avatar.salesVideoComplianceRecord.v1`

### Finalidade
Representa o registro mínimo de compliance e consentimento associado ao uso produtivo do avatar, quando aplicável.

### Produzido por
- backend
- fluxo administrativo/manual

### Consumido por
- backend
- frontend administrativo
- `video-management-service` apenas via decisão do backend

### Pais típicos
- `avatar.salesVideoProfile.v1`

### Payload mínimo recomendado

```json
{
  "consentRequired": true,
  "consentCaptured": true,
  "consentReference": "doc-or-record-id",
  "reviewRequired": true,
  "reviewCompleted": true,
  "reviewedBy": "user@example.com",
  "reviewedAt": "2026-04-16T00:00:00Z"
}
```

### Invariantes
- render produtivo com avatar pessoal não deve ignorar este registro quando exigido;
- compliance não deve ser inferido apenas por convenção;
- a ausência deste artefato pode bloquear o fluxo, quando a política exigir.

---

# 9. Relações canônicas entre artefatos

## 9.1 Cadeia principal do fluxo

```text
salesVideoProfile
  -> salesVideoScriptVersion
      -> salesVideoStoryboard (opcional)
          -> salesVideoRenderRequest
              -> salesVideoRenderJob
                  -> salesVideoJobEvent(s)
                  -> salesVideoProviderExecution (opcional)
                  -> salesVideoAssetBundle
                      -> landingVideoSlotBinding
                          -> landingVideoSlotHistorySnapshot(s)
```

## 9.2 Regras

- `salesVideoProfile` é a âncora do fluxo;
- `salesVideoScriptVersion` é versionado e reutilizável;
- `salesVideoRenderRequest` antecede o job;
- `salesVideoRenderJob` é a unidade de execução assíncrona;
- `salesVideoAssetBundle` representa o resultado pronto no backend;
- `landingVideoSlotBinding` representa publicação;
- `landingVideoSlotHistorySnapshot` representa histórico de publicação;
- `salesVideoComplianceRecord` pode funcionar como gating transversal do fluxo.

---

## 10. O que ainda não entra no cânone inicial

Os itens abaixo podem virar artefatos canônicos no futuro, mas **não entram ainda no cânone inicial**:

- experimentação comercial avançada do módulo;
- artefatos de avaliação de performance por script/avatar;
- artefatos de playbook de objeção por nicho;
- artefatos de otimização de conversão;
- artefatos de analytics agregados;
- artefatos de treinamento avançado de avatar;
- artefatos de biblioteca global de prompts do módulo.

Esses itens podem ser adicionados em versões futuras do cânone, quando deixarem de ser apenas operação auxiliar e passarem a ser parte estável do domínio.

---

## 11. Regras de versionamento do cânone

## 11.1 Versão do artefato

Cada artefato deve ter:
- `artifactType` com versão major;
- `schemaVersion` explícita no envelope.

## 11.2 Mudanças compatíveis

Mudanças compatíveis podem:
- adicionar campos opcionais;
- enriquecer metadata;
- refinar payload sem quebrar consumidores existentes.

## 11.3 Mudanças incompatíveis

Mudanças incompatíveis devem:
- criar nova versão major do `artifactType`;
- registrar migração;
- atualizar consumidores.

---

## 12. Regras para o Codex ao implementar artefatos

O Codex deve:

- tratar este documento como fonte de definição semântica dos artefatos;
- não reduzir artefatos a DTOs arbitrários sem lineage;
- não mover o significado do artefato para nomes de tabela ou implementação local;
- registrar mudanças de contrato;
- manter compatibilidade quando possível;
- evitar criar artefato canônico para dado que é apenas transitório.

O Codex não deve:

- duplicar artefatos com nomes parecidos e sem justificativa;
- usar nomes genéricos demais;
- misturar status de job com status de artefato sem distinção;
- criar publication state fora do backend;
- criar “resultado final” sem distinguir solicitação, execução e publicação.

---

## 13. Critério de conformidade com este cânone

Uma implementação está em conformidade com este documento quando:

- usa os nomes e papéis canônicos definidos aqui;
- respeita o envelope comum;
- preserva lineage mínima;
- distingue artefatos de domínio, execução e publicação;
- mantém persistência via backend;
- não trata resposta efêmera de provider como estado canônico sem mediação do backend.

---

## 14. Próxima etapa recomendada

Depois deste documento, a próxima etapa natural é:

**definir os schemas concretos de cada artefato**  
em documentos ou arquivos específicos, por exemplo:

- `/schemas/avatar/sales-video-profile.schema.json`
- `/schemas/avatar/sales-video-script-version.schema.json`
- `/schemas/avatar/sales-video-render-job.schema.json`
- etc.

Este documento define o **cânone inicial**.  
Os schemas detalhados e os contratos de API devem refinar esse cânone, não contradizê-lo.

---
