template_id: landing-design-preset
template_version: v1
artifact_target: landingPageDesignPreset

SYSTEM_INSTRUCTIONS
Você está gerando o artefato canônico `landingPageDesignPreset` para a landing page.

Objetivo:
- Definir tema visual e presets por seção de forma estruturada e determinística.
- Não gerar HTML final nesta etapa.
- Não inventar sectionId que não exista no wireframe.

Regras:
1. Preencher `presetId`, `lhmRuntime`, `theme`, `sectionPresets`, `componentPresets`, `motion` e `consistencyChecks`.
2. `sectionPresets[]` deve cobrir todas as seções relevantes do wireframe.
3. `surfaceStyle` só pode ser: `band`, `solid`, `gradient-soft`, `image-tint`.
4. `contrastMode` só pode ser: `normal`, `high`, `soft`.
5. `emphasis` só pode ser: `primary`, `secondary`, `support`.
6. `motion.intensity` só pode ser: `none`, `subtle`, `moderate`.
7. `consistencyChecks` deve incluir pelo menos:
   - `THEME_CONTRAST`
   - `CTA_VISUAL_HIERARCHY`
   - `MOBILE_READABILITY`
8. `layoutPreset` só pode ser: `hero-focus`, `form-focus`, `proof-grid`, `narrative-stack`, `faq-clean`, `cta-strong`.
9. `motion.enabled` deve ser booleano (`true` ou `false`).
10. `theme.typography.maxLineLength` deve ficar entre `55ch` e `75ch`.
11. `theme.typography.lineHeightBody` deve ser `>= 1.5`.
12. `theme.spacing.sectionGapMobile` deve ser `>= 48px`.
13. `theme.palette.ctaPrimary` deve ter contraste AA com o fundo predominante da seção.
14. O preset deve conter obrigatoriamente os tokens mínimos: `theme.palette`, `theme.typography`, `theme.spacing`, `theme.accessibility`, `componentPresets.cta`, `componentPresets.trust`.
15. Para páginas de venda/captação, definir obrigatoriamente `componentPresets.proof.showIdentity = true` (não omitir e não usar `false`).
16. Incorporar no preset os padrões da seção "Padrões de CSS e componentes por elemento" de `docs/pesquisa-profunda/pesquisa-profunda-html-estilos.md`, cobrindo explicitamente: `<p>`, `<h1>`, `<h2>`, `<h3>`, `<ul>/<li>`, `<button>`/CTA, `<form>`, `<label>`, `<input>`, `<img>`.
17. Para cada elemento listado, declarar atributos visuais trabalhados e os tokens correspondentes no preset (tipografia, espaçamento, dimensões, contraste, foco e superfície), mantendo consistência com `theme` e `componentPresets.primitives`.
18. Distribuir no preset (de forma natural e não mecânica) diretrizes de acabamento premium para **todas** as superfícies e variações (`.lhm-surface-band`, `.lhm-surface-solid`, `.lhm-surface-gradient-soft`, `.lhm-surface-image-tint`), combinando ao menos: borda sutil, raio coerente, sombra de profundidade, respiro interno responsivo, controle de overflow e estados visuais consistentes com os tokens de `theme.radius`, `theme.shadow`, `theme.spacing` e `theme.palette`.
19. No `lhmRuntime.baseCss`, preservar rigorosamente a ordem das declarações CSS e regras de camadas (base → componentes → utilitários → overrides, e superfícies/background antes de conteúdo/efeitos), evitando sobrescritas destrutivas que degradem acabamentos premium das camadas elaboradas.
20. `lhmRuntime` é obrigatório e deve conter:
   - `baseCss`: string com o CSS base que sustenta classes/superfícies do preset.
   - `cssVersion`: string de versão da malha CSS (ex.: `lhm-css-v1`).
   - `cssNotes`: string curta explicando escopo e compatibilidade da versão.
21. Nunca omitir `lhmRuntime` e nunca serializar esse bloco como texto/JSON escapado (deve ser objeto JSON real).
22. Saída obrigatoriamente em JSON válido no envelope do artefato, sem markdown, sem bloco de código e sem texto adicional.
23. A resposta deve aderir estritamente ao schema JSON canônico da etapa `landingPageDesignPreset`; não incluir campos fora do contrato.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Responda em JSON válido no formato:
{
  "landingPageDesignPreset": {
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
}
