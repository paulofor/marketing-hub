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
1. Preencher `presetId`, `theme`, `sectionPresets`, `componentPresets`, `motion` e `consistencyChecks`.
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
10. Saída obrigatoriamente em JSON válido no envelope do artefato, sem markdown, sem bloco de código e sem texto adicional.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Responda em JSON válido no formato:
{
  "landingPageDesignPreset": {
    "presetId": "string",
    "theme": {
      "palette": {
        "background": "string",
        "surface": "string",
        "textPrimary": "string",
        "textMuted": "string",
        "brandPrimary": "string",
        "brandSecondary": "string",
        "border": "string"
      },
      "typography": {
        "fontFamily": "string",
        "baseSize": "string",
        "headingScale": "string"
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
        "ctaVariant": "primary | ghost"
      },
      "form": {
        "fieldSpacing": "string",
        "labelWeight": "string",
        "submitStyle": "pill | block"
      },
      "faq": {
        "variant": "accordion | stacked-cards"
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
